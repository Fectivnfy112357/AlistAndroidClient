package com.textvision.alistclient.admin.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StoragePatch
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StorageEditUiState {
    data object Loading : StorageEditUiState
    data class Form(
        val storage: StorageInfo,
        val driver: DriverInfo?,
        val formItems: List<FormItem>,
        val fieldValues: Map<String, Any?>,
        val enabled: Boolean,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val saved: Boolean = false,
    ) : StorageEditUiState
    data class Error(val message: String) : StorageEditUiState
}

@HiltViewModel
class StorageEditViewModel @Inject constructor(
    @Suppress("unused") private val authRepository: AuthRepository,
    @Suppress("unused") private val transferManager: TransferManager,
    @Suppress("unused") private val previewFileStore: PreviewFileStore,
    private val storageRepository: StorageRepositoryContract,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StorageEditUiState>(StorageEditUiState.Loading)
    val uiState: StateFlow<StorageEditUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.value = StorageEditUiState.Loading
            val listR = storageRepository.list(base)
            val s = (listR as? AdminResult.Ok)?.data?.content?.firstOrNull { it.id == id }
            if (s == null) {
                _uiState.value = StorageEditUiState.Error("找不到存储 #$id")
                return@launch
            }
            val driversR = storageRepository.listDrivers(base)
            val driversMap = (driversR as? AdminResult.Ok)?.data.orEmpty()
            val driver = driversMap[s.driver]
            val common = driver?.common ?: emptyList()
            val additional = driver?.additional ?: emptyList()
            val formItems = (common + additional).map { FormItem.fromConfigItem(it) }

            // Build fieldValues: start with parsed addition JSON, overlay flat storage fields.
            val additionMap = parseAddition(s.addition)
            val flatMap = flatStorageFields(s)
            // common fields take precedence (flat over addition)
            val merged = additionMap + flatMap
            _uiState.value = StorageEditUiState.Form(
                storage = s,
                driver = driver,
                formItems = formItems,
                fieldValues = merged,
                // 用 disabled 布尔字段判定,不要用 status 字符串（status 是运行时挂载态,
                // 已禁用但最近未挂载过的存储 status="work",会误判为启用）。
                enabled = !s.disabled,
            )
        }
    }

    fun updateField(name: String, value: Any?) {
        _uiState.update { state ->
            if (state is StorageEditUiState.Form) {
                state.copy(fieldValues = state.fieldValues + (name to value))
            } else state
        }
    }

    /**
     * 启用/禁用切换：单独走专用端点，与 update 完全分离。
     * 不调 save()，不调用 update 端点，不会触发"卸载→重挂"副作用。
     */
    fun toggleEnabled() {
        val state = _uiState.value as? StorageEditUiState.Form ?: return
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        val storageId = state.storage.id ?: return
        val newEnabled = !state.enabled
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val r = storageRepository.setEnabled(base, storageId, newEnabled)
            _uiState.value = when (r) {
                is AdminResult.Ok -> state.copy(
                    isSaving = false,
                    enabled = newEnabled,
                    // 同时更新底层 storage 的 disabled,让 flatStorageFields 对齐,
                    // 否则下次 save() 计算"字段是否改动"会用旧的 disabled 起点。
                    storage = state.storage.copy(disabled = !newEnabled),
                )
                is AdminResult.ServerError -> state.copy(
                    isSaving = false,
                    errorMessage = r.message?.takeIf { it.isNotBlank() } ?: "操作失败 (HTTP ${r.code})",
                )
                AdminResult.Unauthorized -> state.copy(
                    isSaving = false,
                    errorMessage = "未登录或登录已过期",
                )
                is AdminResult.Network -> state.copy(
                    isSaving = false,
                    errorMessage = "网络错误：无法连接服务器",
                )
            }
        }
    }

    /** @deprecated 仅保留兼容性，新 UI 不要再调用 — 请改用 toggleEnabled()。 */
    fun setEnabled(enabled: Boolean) {
        val state = _uiState.value as? StorageEditUiState.Form ?: return
        if (state.enabled == enabled) return
        toggleEnabled()
    }

    fun save() {
        val state = _uiState.value as? StorageEditUiState.Form ?: return
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            val mountPath = readCommonString(state.fieldValues, "mount_path", state.storage.mountPath)
            if (mountPath.isNullOrBlank()) {
                _uiState.value = state.copy(isSaving = false, errorMessage = "挂载路径不能为空")
                return@launch
            }
            _uiState.value = state.copy(isSaving = true, errorMessage = null)

            // 保存前先拉取最新 storage:启用/禁用可能刚通过专用端点改过,
            // state.storage 里的 disabled/status/modified 可能是进页面时的旧值。
            // 用最新值填 patch,避免把旧 status(如已禁用后仍是 "work")原样回写覆盖后端。
            val storageId = state.storage.id ?: 0L
            val latest = (storageRepository.list(base) as? AdminResult.Ok)
                ?.data?.content?.firstOrNull { it.id == storageId }
                ?: state.storage

            val additionalItems = state.driver?.additional ?: emptyList()
            val additionalNames = additionalItems.map { it.name }.toSet()
            // 把 fieldValues 按 ConfigItem.type 还原为正确 JSON 类型（bool/number/string）
            val typeByName = additionalItems.associate { it.name to it.type?.lowercase() }
            val additionOnly = state.fieldValues
                .filterKeys { it in additionalNames }
                .mapValues { (k, v) -> coerceForJson(v, typeByName[k]) }
            val addition = serializeAddition(additionOnly)
            val v = state.fieldValues
            val patch = StoragePatch(
                id = storageId,
                mountPath = readCommonString(v, "mount_path", state.storage.mountPath) ?: state.storage.mountPath,
                driver = state.storage.driver,
                // 启用/禁用走专用端点;update 用最新的 disabled/status/modified,不覆盖刚切换的状态。
                disabled = latest.disabled,
                status = latest.status,
                modified = latest.modified,
                order = readCommonInt(v, "order", state.storage.order),
                remark = readCommonString(v, "remark", state.storage.remark),
                cacheExpiration = readCommonInt(v, "cache_expiration", state.storage.cacheExpiration),
                webProxy = readCommonBool(v, "web_proxy", state.storage.webProxy),
                webdavPolicy = readCommonString(v, "webdav_policy", state.storage.webdavPolicy),
                downProxyUrl = readCommonString(v, "down_proxy_url", state.storage.downProxyUrl),
                downProxySign = readCommonBool(v, "down_proxy_sign", state.storage.downProxySign),
                proxyRange = readCommonBool(v, "proxy_range", state.storage.proxyRange),
                orderBy = readCommonString(v, "order_by", state.storage.orderBy),
                orderDirection = readCommonString(v, "order_direction", state.storage.orderDirection),
                extractFolder = readCommonString(v, "extract_folder", state.storage.extractFolder),
                disableIndex = readCommonBool(v, "disable_index", state.storage.disableIndex),
                enableSign = readCommonBool(v, "enable_sign", state.storage.enableSign),
                addition = addition,
            )
            when (val r = storageRepository.update(base, patch)) {
                is AdminResult.Ok -> _uiState.value = state.copy(isSaving = false, saved = true)
                is AdminResult.ServerError -> {
                    val raw = r.message?.takeIf { it.isNotBlank() } ?: "保存失败 (HTTP ${r.code})"
                    _uiState.value = state.copy(isSaving = false, errorMessage = localizeStorageError(raw))
                }
                AdminResult.Unauthorized -> _uiState.value = state.copy(
                    isSaving = false,
                    errorMessage = "未登录或登录已过期",
                )
                is AdminResult.Network -> _uiState.value = state.copy(
                    isSaving = false,
                    errorMessage = "网络错误：无法连接服务器",
                )
            }
        }
    }

    private fun parseAddition(raw: String?): Map<String, Any?> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val json = kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonObject
            json.mapValues { (_, v) ->
                when (v) {
                    is kotlinx.serialization.json.JsonPrimitive -> when (v.content) {
                        "true" -> true
                        "false" -> false
                        else -> v.content
                    }
                    is kotlinx.serialization.json.JsonNull -> null
                    else -> v.toString()
                }
            }
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    /** 把 Any? 用户输入值按 ConfigItem.type 归一化为 Bool/Number/String/JsonNull。 */
    private fun coerceForJson(value: Any?, type: String?): kotlinx.serialization.json.JsonElement {
        if (value == null) return kotlinx.serialization.json.JsonNull
        return when (type) {
            "bool", "boolean" -> kotlinx.serialization.json.JsonPrimitive(
                when (value) {
                    is Boolean -> value
                    is String -> value.equals("true", ignoreCase = true)
                    else -> false
                }
            )
            "number", "int", "integer", "float", "double" -> {
                val s = value.toString()
                val d = s.toDoubleOrNull()
                if (d != null) kotlinx.serialization.json.JsonPrimitive(d)
                else kotlinx.serialization.json.JsonPrimitive(s)
            }
            else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
        }
    }

    private fun serializeAddition(values: Map<String, kotlinx.serialization.json.JsonElement>): String {
        return kotlinx.serialization.json.JsonObject(values).toString()
    }

    private fun flatStorageFields(s: StorageInfo): Map<String, Any?> {
        val m = mutableMapOf<String, Any?>()
        m["mount_path"] = s.mountPath
        m["order"] = s.order
        m["remark"] = s.remark ?: ""
        m["cache_expiration"] = s.cacheExpiration
        m["web_proxy"] = s.webProxy
        m["webdav_policy"] = s.webdavPolicy ?: ""
        m["down_proxy_url"] = s.downProxyUrl ?: ""
        m["down_proxy_sign"] = s.downProxySign
        m["proxy_range"] = s.proxyRange
        m["order_by"] = s.orderBy ?: ""
        m["order_direction"] = s.orderDirection ?: ""
        m["extract_folder"] = s.extractFolder ?: ""
        m["disable_index"] = s.disableIndex
        m["enable_sign"] = s.enableSign
        m["disabled"] = s.disabled
        return m
    }

    /** 从 fieldValues 读 common 扁平字段的值（用户可能编辑过）。 */
    private fun readCommon(
        values: Map<String, Any?>,
        name: String,
        fallback: Any?,
    ): Any? = if (values.containsKey(name)) values[name] else fallback

    private fun readCommonString(values: Map<String, Any?>, name: String, fallback: String?): String? {
        val raw = readCommon(values, name, fallback)
        val s = raw?.toString().orEmpty()
        return s.ifEmpty { null }
    }

    private fun readCommonInt(values: Map<String, Any?>, name: String, fallback: Int): Int {
        val raw = readCommon(values, name, fallback) ?: return fallback
        return raw.toString().toIntOrNull() ?: fallback
    }

    private fun readCommonBool(values: Map<String, Any?>, name: String, fallback: Boolean): Boolean {
        val raw = readCommon(values, name, fallback) ?: return fallback
        return when (raw) {
            is Boolean -> raw
            is String -> raw.equals("true", ignoreCase = true)
            else -> fallback
        }
    }
}

private val kotlinx.serialization.json.JsonElement.jsonObject: kotlinx.serialization.json.JsonObject
    get() = this as kotlinx.serialization.json.JsonObject

/**
 * Translate known English server errors into Chinese hints for the storage edit screen.
 * Pass-through anything not recognised so unexpected messages still surface.
 */
private fun localizeStorageError(raw: String): String {
    val lower = raw.lowercase()
    return when {
        // Alist 卸载旧挂载后按新配置重挂失败(通常是 Local 的 root_folder_path 指向不存在的目录),
        // 该存储会脱离内存挂载表,之后 update/disable/enable 都会连锁失败,只能重启 Alist 服务恢复。
        "no mount path" in lower ->
            "重新挂载失败:请检查根目录路径(root_folder_path)是否指向服务器上真实存在的目录。" +
                "若该存储已无法启用/禁用,需重启 Alist 服务后再修改。"
        "no mount" in lower -> "挂载路径无效,请确认挂载路径不为空"
        "driver" in lower && ("invalid" in lower || "not found" in lower) -> "存储驱动无效或未安装,请在 Alist 后台检查驱动配置"
        "have enabled" in lower -> "该存储已处于启用状态"
        "permission" in lower || "forbidden" in lower -> "权限不足,请用管理员账号登录"
        else -> raw
    }
}
