package com.textvision.alistclient.auth.model

data class SavedSession(
    val serverUrl: String,
    val username: String,
    val password: String,
    val token: String,
)
