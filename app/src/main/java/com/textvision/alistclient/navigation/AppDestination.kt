package com.textvision.alistclient.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destinations (Navigation Compose 2.8+ / kotlinx.serialization).
 * Task 20 将把 AppNavHost 从旧 string-based AppRoute 迁移到这套定义。
 */
@Serializable
data object LoginDest

@Serializable
data class FilesDest(val path: String = "/")

@Serializable
data object TransfersDest

@Serializable
data object SettingsDest

@Serializable
data object HomeDest

@Serializable
data class PreviewDestArgs(
    val name: String,
    val path: String,
    val fileTypeName: String,
    val downloadUrl: String?,
    val size: Long,
)

@Serializable
data class PreviewDest(val args: PreviewDestArgs)

@Serializable
data class MoveCopyPickerDest(val op: String, val path: String)

@Serializable
data class StorageEditDest(val id: Int)

@Serializable
data object AdminSiteSettingsDest

@Serializable
data object MusicLibraryDest

@Serializable
data object MusicPreviewDest
