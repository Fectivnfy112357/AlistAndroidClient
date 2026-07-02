package com.textvision.alistclient.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.textvision.alistclient.ui.components.CloudBottomBar
import com.textvision.alistclient.ui.screens.FileScreen
import com.textvision.alistclient.home.HomeScreen
import com.textvision.alistclient.ui.screens.LoginScreen
import com.textvision.alistclient.ui.screens.MoveCopyTargetPickerScreen
import com.textvision.alistclient.ui.screens.PreviewScreen
import com.textvision.alistclient.ui.screens.SettingsScreen
import com.textvision.alistclient.ui.screens.TransferScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavHost(startAuthenticated: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startAuthenticated) AppRoute.Files.route else AppRoute.Login.route
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in setOf(AppRoute.Home.route, AppRoute.Files.route, AppRoute.Transfers.route, AppRoute.Settings.route)

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { hyperOsEnterTransition() },
                exitTransition = { hyperOsExitTransition() },
                popEnterTransition = { hyperOsPopEnterTransition() },
                popExitTransition = { hyperOsPopExitTransition() },
            ) {
                composable(AppRoute.Login.route) {
                    LoginScreen(onLoginSuccess = {
                        navController.navigate(AppRoute.Files.route) {
                            popUpTo(AppRoute.Login.route) { inclusive = true }
                        }
                    })
                }
                composable(AppRoute.Home.route) {
                    HomeScreen(
                        onStorageClick = { mountPath: String ->
                            navController.navigate(AppRoute.Files.create(mountPath))
                        },
                    )
                }
                composable(
                    route = AppRoute.Files.route,
                    arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "/" }),
                ) { entry ->
                    val path = URLDecoder.decode(entry.arguments?.getString("path") ?: "/", StandardCharsets.UTF_8.name())
                    FileScreen(
                        initialPath = path,
                        onPreview = { item ->
                            navController.navigate(
                                AppRoute.Preview.create(
                                    name = item.name,
                                    path = item.path,
                                    type = item.type,
                                    downloadUrl = item.downloadUrl,
                                    size = item.size,
                                )
                            )
                        },
                    )
                }
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
                    arguments = listOf(navArgument("payload") { type = NavType.StringType })
                ) { entry ->
                    val payload = requireNotNull(entry.arguments?.getString("payload"))
                    val args = AppRoute.Preview.decode(payload)
                    PreviewScreen(
                        name = args.name,
                        path = args.path,
                        type = args.type,
                        downloadUrl = args.downloadUrl,
                        size = args.size,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            if (showBottomBar) {
                CloudBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}
