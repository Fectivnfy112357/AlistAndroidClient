package com.textvision.alistclient.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.textvision.alistclient.ui.foundation.AppBottomBar
import com.textvision.alistclient.ui.foundation.BottomNavItem

private const val TAB_HOME = "home"
private const val TAB_FILES = "files"
private const val TAB_MUSIC = "music"
private const val TAB_TRANSFERS = "transfers"
private const val TAB_SETTINGS = "settings"

private val BottomNavItems = listOf(
    BottomNavItem(TAB_HOME, "首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(TAB_FILES, "文件", Icons.Filled.Folder, Icons.Outlined.Folder),
    BottomNavItem(TAB_MUSIC, "音乐", Icons.Filled.MusicNote, Icons.Outlined.MusicNote),
    BottomNavItem(TAB_TRANSFERS, "传输", Icons.Filled.SwapVert, Icons.Outlined.SwapVert),
    BottomNavItem(TAB_SETTINGS, "设置", Icons.Filled.Settings, Icons.Outlined.Settings),
)

/**
 * Bottom navigation bar for the five top-level tabs. Decides its own visibility:
 * it only renders when the current destination is one of
 * Home/Files/Music/Transfers/Settings.
 */
@Composable
fun AppBottomNavBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val currentTab = when {
        destination == null -> null
        destination.hasRoute(HomeDest::class) -> TAB_HOME
        destination.hasRoute(FilesDest::class) -> TAB_FILES
        destination.hasRoute(MusicLibraryDest::class) -> TAB_MUSIC
        destination.hasRoute(TransfersDest::class) -> TAB_TRANSFERS
        destination.hasRoute(SettingsDest::class) -> TAB_SETTINGS
        else -> null
    } ?: return

    Box(modifier) {
        AppBottomBar(
            currentRoute = currentTab,
            items = BottomNavItems,
            onNavigate = { route ->
                val target: Any = when (route) {
                    TAB_HOME -> HomeDest
                    TAB_FILES -> FilesDest()
                    TAB_MUSIC -> MusicLibraryDest
                    TAB_TRANSFERS -> TransfersDest
                    TAB_SETTINGS -> SettingsDest
                    else -> return@AppBottomBar
                }
                navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
}
