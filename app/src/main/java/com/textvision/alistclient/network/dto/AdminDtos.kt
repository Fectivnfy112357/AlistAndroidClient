package com.textvision.alistclient.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull

@Serializable
data class StorageList(
    @SerialName("content") val content: List<StorageInfo> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class StorageInfo(
    @SerialName("id") val id: Long? = null,
    @SerialName("mount_path") val mountPath: String,
    @SerialName("driver") val driver: String = "",
    @SerialName("status") val status: String? = null,
    @SerialName("used_bytes") val usedBytes: Long = 0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
    @SerialName("addition") val addition: String? = null,
)

/**
 * /api/public/settings -> data
 */
@Serializable
data class PublicSettings(
    @SerialName("title") val title: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("version") val version: String? = null,
)

@Serializable
data class StorageListRequest(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
)

@Serializable
data class UserList(
    @SerialName("content") val content: List<User> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class User(
    val id: Long? = null,
    val username: String = "",
    @SerialName("base_path") val basePath: String? = null,
    val role: String = "",
    val disabled: Boolean = false,
    val permission: Int = 0,
    @SerialName("sso_id") val ssoId: String? = null,
)

@Serializable
data class RoleList(
    @SerialName("content") val content: List<Role> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class Role(
    val id: Long? = null,
    val name: String = "",
    val description: String? = null,
    val default: Boolean = false,
)

@Serializable
data class SessionInfo(
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("last_active") val lastActive: Long = 0,
    val status: Int = 0,
    val ua: String? = null,
    val ip: String? = null,
)

@Serializable
data class TaskInfo(
    val id: String = "",
    val name: String = "",
    val state: String? = null,
    val status: String? = null,
    val progress: Double = 0.0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
    val error: String? = null,
)

@Serializable
data class StoragePatch(
    val id: Long,
    @SerialName("mount_path") val mountPath: String,
    val driver: String,
    val order: Int = 0,
    val remark: String? = null,
    @SerialName("disabled") val disabled: Boolean = false,
    @SerialName("cache_expiration") val cacheExpiration: Int = 0,
    @SerialName("web_proxy") val webProxy: Boolean = false,
    @SerialName("webdav_policy") val webdavPolicy: String? = null,
    @SerialName("down_proxy_url") val downProxyUrl: String? = null,
    @SerialName("down_proxy_sign") val downProxySign: Boolean = false,
    @SerialName("proxy_range") val proxyRange: Boolean = false,
    @SerialName("order_by") val orderBy: String? = null,
    @SerialName("order_direction") val orderDirection: String? = null,
    @SerialName("extract_folder") val extractFolder: String? = null,
    @SerialName("disable_index") val disableIndex: Boolean = false,
    @SerialName("enable_sign") val enableSign: Boolean = false,
    @SerialName("addition") val addition: String = "{}",
)

@Serializable
data class DriverInfo(
    val name: String,
    val label: String? = null,
    @SerialName("common") val common: List<ConfigItem>? = null,
    @SerialName("additional") val additional: List<ConfigItem>? = null,
)

@Serializable
data class ConfigItem(
    val name: String,
    val label: String? = null,
    val type: String? = null,
    val default: kotlinx.serialization.json.JsonElement? = null,
    val options: kotlinx.serialization.json.JsonElement? = null,
    val required: Boolean = false,
    val help: String? = null,
) {
    /** v3 returns defaults as strings ("30", "true", "name", or empty). */
    fun defaultAsString(): String? =
        (default as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

    fun defaultAsBool(): Boolean? = when (defaultAsString()) {
        "true" -> true
        "false" -> false
        else -> null
    }

    fun defaultAsNumber(): Double? = defaultAsString()?.toDoubleOrNull()
}

@Serializable
data class DriverList(
    // Real v3 returns map keyed by driver name.
    val content: Map<String, DriverInfo> = emptyMap(),
    val total: Int = 0,
)

@Serializable
data class SettingItem(
    val key: String,
    val value: String? = null,
    val type: String? = null,
    val group: String? = null,
    val help: String? = null,
    @SerialName("form_items") val formItems: List<ConfigItem>? = null,
    val options: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class SettingsList(
    val content: List<SettingItem> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class SettingSaveItem(
    val key: String,
    val value: String,
)

@Serializable
data class SettingSaveRequest(
    val items: List<SettingSaveItem>,
)
