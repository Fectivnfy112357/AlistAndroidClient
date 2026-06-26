package com.textvision.alistclient.navigation

sealed class AppRoute(val route: String) {
    data object Login : AppRoute("login")
    data object Files : AppRoute("files")
    data object Transfers : AppRoute("transfers")
    data object Settings : AppRoute("settings")
    data object MoveCopyPicker : AppRoute("copy_move_picker")
    data object Preview : AppRoute("preview/{filePath}") {
        fun create(filePath: String): String = "preview/${android.net.Uri.encode(filePath)}"
    }
}
