package com.textvision.alistclient.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class AlistLoginData(
    val token: String,
)
