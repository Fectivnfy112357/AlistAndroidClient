package com.textvision.alistclient.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FsGetRequest(val path: String, val password: String = "")

@Serializable
data class AlistFsGetData(
    val name: String,
    val size: Long = 0,
    @SerialName("is_dir") val isDir: Boolean = false,
    val sign: String? = null,
    @SerialName("raw_url") val rawUrl: String? = null,
    val thumb: String? = null,
)
