package com.textvision.alistclient.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.net.Uri
import com.textvision.alistclient.ui.screens.FileScreen
import com.textvision.alistclient.ui.screens.LoginScreen
import com.textvision.alistclient.ui.screens.MoveCopyTargetPickerScreen
import com.textvision.alistclient.ui.screens.PreviewScreen
import com.textvision.alistclient.ui.screens.SettingsScreen
import com.textvision.alistclient.ui.screens.TransferScreen

@Composable
fun AppNavHost(startAuthenticated: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startAuthenticated) AppRoute.Files.route else AppRoute.Login.route
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in setOf(AppRoute.Files.route, AppRoute.Transfers.route, AppRoute.Settings.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Files.route,
                        onClick = { navController.navigate(AppRoute.Files.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        label = { Text("文件") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Transfers.route,
                        onClick = { navController.navigate(AppRoute.Transfers.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.SyncAlt, contentDescription = null) },
                        label = { Text("传输") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Settings.route,
                        onClick = { navController.navigate(AppRoute.Settings.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("设置") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppRoute.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(AppRoute.Files.route) {
                        popUpTo(AppRoute.Login.route) { inclusive = true }
                    }
                })
            }
            composable(AppRoute.Files.route) { FileScreen() }
            composable(AppRoute.Transfers.route) { TransferScreen() }
            composable(AppRoute.Settings.route) { SettingsScreen() }
            composable(AppRoute.MoveCopyPicker.route) {
                MoveCopyTargetPickerScreen(onTargetSelected = { navController.popBackStack() })
            }
            composable(
                route = AppRoute.Preview.route,
                arguments = listOf(navArgument("filePath") { type = NavType.StringType })
            ) { entry ->
                val encoded = requireNotNull(entry.arguments?.getString("filePath"))
                val filePath = Uri.decode(encoded)
                PreviewScreen(
                    filePath = filePath,
                    onDownload = { navController.popBackStack() },
                    onExternalOpen = { navController.popBackStack() },
                )
            }
        }
    }
}
