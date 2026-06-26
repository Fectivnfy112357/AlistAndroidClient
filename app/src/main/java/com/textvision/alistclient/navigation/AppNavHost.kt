package com.textvision.alistclient.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.textvision.alistclient.ui.components.CloudBottomBar
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

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
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
                composable(AppRoute.Settings.route) {
                    SettingsScreen(
                        onLoggedOut = {
                            navController.navigate(AppRoute.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
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
            if (showBottomBar) {
                CloudBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) { launchSingleTop = true }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
