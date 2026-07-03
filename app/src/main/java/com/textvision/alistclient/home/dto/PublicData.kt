package com.textvision.alistclient.home.dto

data class PublicData(
    val siteTitle: String,
    val siteVersion: String?,
    val announcement: String?,
    val logo: String?,
    val favicon: String?,
    val mainColor: String?,
    val allowRegister: Boolean,
)
