package com.textvision.alistclient.navigation

sealed class AppRoute(val route: String) {
    data object Login : AppRoute("login")
    data object Files : AppRoute("files")
    data object Transfers : AppRoute("transfers")
    data object Settings : AppRoute("settings")
}
