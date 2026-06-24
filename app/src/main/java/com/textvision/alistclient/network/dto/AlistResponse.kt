package com.textvision.alistclient.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlistResponse<T>(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: T? = null,
)
