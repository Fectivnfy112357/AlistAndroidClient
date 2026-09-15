package com.textvision.alistclient.ui.feature.home.dto

import androidx.compose.runtime.Immutable

@Immutable
data class PublicData(
    val siteTitle: String,
    val siteVersion: String?,
    val announcement: String?,
    val logo: String?,
    val favicon: String?,
    val mainColor: String?,
    val allowRegister: Boolean,
)
