package com.textvision.alistclient.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
