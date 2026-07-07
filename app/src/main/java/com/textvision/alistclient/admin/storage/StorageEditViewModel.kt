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
            val driversList = (driversR as? AdminResult.Ok)?.data.orEmpty()
            val driver = driversList.firstOrNull { it.name == s.driver }

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
                enabled = s.status != "disabled",
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

    fun setEnabled(enabled: Boolean) {
        _uiState.update { state -> if (state is StorageEditUiState.Form) state.copy(enabled = enabled) else state }
    }

    fun save() {
        val state = _uiState.value as? StorageEditUiState.Form ?: return
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val additionalNames = (state.driver?.additional ?: emptyList()).map { it.name }.toSet()
            val additionOnly = state.fieldValues.filterKeys { it in additionalNames }
            val addition = serializeAddition(additionOnly)
            val patch = StoragePatch(
                id = state.storage.id ?: 0L,
                mountPath = state.storage.mountPath,
                driver = state.storage.driver,
                disabled = !state.enabled,
                addition = addition,
            )
            when (val r = storageRepository.update(base, patch)) {
                is AdminResult.Ok -> _uiState.value = state.copy(isSaving = false, saved = true)
                else -> _uiState.value = state.copy(isSaving = false, errorMessage = "保存失败")
            }
        }
    }

    private fun parseAddition(raw: String?): Map<String, Any?> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val json = kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonObject
            json.mapValues { (_, v) ->
                when (v) {
                    is kotlinx.serialization.json.JsonPrimitive -> v.content
                    else -> v.toString()
                }
            }
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    private fun serializeAddition(values: Map<String, Any?>): String {
        val obj = kotlinx.serialization.json.JsonObject(
            values.mapValues { (_, v) -> kotlinx.serialization.json.JsonPrimitive(v?.toString() ?: "") }
        )
        return obj.toString()
    }

    /**
     * Flat storage fields that the v3 API stores on the storage object directly
     * (not inside the addition JSON). These are read-only here for the edit screen —
     * mount_path / order / remark / disabled / cache_expiration / web_proxy /
     * webdav_policy / down_proxy_url / down_proxy_sign / order_by / order_direction /
     * extract_folder / disable_index / enable_sign. Only fields we have data for
     * from [StorageInfo] are populated; the rest will arrive via a future richer
     * storage DTO and are omitted from the patch.
     */
    private fun flatStorageFields(s: StorageInfo): Map<String, Any?> {
        val m = mutableMapOf<String, Any?>()
        s.status?.let { if (it == "disabled") m["disabled"] = true }
        // The current StorageInfo DTO only carries a subset of flat fields — we keep
        // keys that we can actually populate. Common field names that the user may
        // edit should be added here as the StorageInfo model grows.
        m["mount_path"] = s.mountPath
        return m
    }
}

private val kotlinx.serialization.json.JsonElement.jsonObject: kotlinx.serialization.json.JsonObject
    get() = this as kotlinx.serialization.json.JsonObject
