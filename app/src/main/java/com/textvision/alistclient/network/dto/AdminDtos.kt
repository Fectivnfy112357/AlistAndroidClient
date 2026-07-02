package com.textvision.alistclient.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * /api/admin/info -> data
 *
 * Field names follow Alist v3 source. Some servers return the totals
 * inside the `data` payload as `used_bytes` / `total_bytes`; others
 * nest them in a `data.usage` object. The mapping below covers the
 * flat form which is the documented public shape.
 */
@Serializable
data class AdminInfo(
    @SerialName("version") val version: String? = null,
    @SerialName("build_date") val buildDate: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("used_bytes") val usedBytes: Long = 0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
)

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
data class AdminInfoRequest(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
)

@Serializable
data class StorageListRequest(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
)