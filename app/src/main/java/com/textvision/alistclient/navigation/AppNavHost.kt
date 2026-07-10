package com.textvision.alistclient.navigation

import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.ui.feature.home.HomeScreen
import com.textvision.alistclient.ui.feature.admin.AdminSiteSettingsScreen
import com.textvision.alistclient.ui.feature.file.FileScreen
import com.textvision.alistclient.ui.feature.auth.LoginScreen
import com.textvision.alistclient.ui.feature.picker.MoveCopyTargetPickerScreen
import com.textvision.alistclient.ui.feature.storage.StorageEditScreen
import com.textvision.alistclient.ui.feature.transfer.TransferScreen
import com.textvision.alistclient.ui.feature.preview.PreviewScreen
import com.textvision.alistclient.ui.feature.settings.SettingsScreen
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

/** Custom NavType so [PreviewDest] can carry the nested [PreviewDestArgs] serializable payload. */
private val PreviewArgsNavType = object : NavType<PreviewDestArgs>(isNullableAllowed = false) {
    private val serializer = serializer<PreviewDestArgs>()

    override fun get(bundle: Bundle, key: String): PreviewDestArgs? =
        bundle.getString(key)?.let { Json.decodeFromString(serializer, it) }

    override fun parseValue(value: String): PreviewDestArgs =
        Json.decodeFromString(serializer, Uri.decode(value))

    override fun serializeAsValue(value: PreviewDestArgs): String =
        Uri.encode(Json.encodeToString(serializer, value))

    override fun put(bundle: Bundle, key: String, value: PreviewDestArgs) {
        bundle.putString(key, Json.encodeToString(serializer, value))
    }
}

@Composable
fun AppNavHost(
    startAuthenticated: Boolean,
    navController: NavHostController = rememberNavController(),
    snackbarHostState: SnackbarHostState,
) {
    val startDestination: Any = if (startAuthenticated) FilesDest() else LoginDest

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AppBottomNavBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            enterTransition = { hyperOsEnterTransition() },
            exitTransition = { hyperOsExitTransition() },
            popEnterTransition = { hyperOsPopEnterTransition() },
            popExitTransition = { hyperOsPopExitTransition() },
        ) {
            composable<LoginDest> {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(FilesDest()) {
                        popUpTo(LoginDest) { inclusive = true }
                    }
                })
            }
            composable<HomeDest> {
                HomeScreen(onStorageClick = { mountPath -> navController.navigate(FilesDest(mountPath)) })
            }
            composable<FilesDest> { entry ->
                val dest = entry.toRoute<FilesDest>()
                FileScreen(
                    initialPath = dest.path,
                    onPreview = { item ->
                        navController.navigate(
                            PreviewDest(
                                PreviewDestArgs(
                                    name = item.name,
                                    path = item.path,
                                    fileTypeName = item.type.name,
                                    downloadUrl = item.downloadUrl,
                                    size = item.size,
                                ),
                            ),
                        )
                    },
                    onFolderNavigate = { folderPath -> navController.navigate(FilesDest(folderPath)) },
                    onBack = if (dest.path != "/") {
                        { navController.popBackStack() }
                    } else {
                        null
                    },
                )
            }
            composable<TransfersDest> { TransferScreen() }
            composable<SettingsDest> {
                SettingsScreen(
                    onLoggedOut = {
                        navController.navigate(LoginDest) { popUpTo(0) { inclusive = true } }
                    },
                    onStorageClick = { id -> navController.navigate(StorageEditDest(id.toInt())) },
                    onAdvancedSettings = { navController.navigate(AdminSiteSettingsDest) },
                )
            }
            composable<AdminSiteSettingsDest> {
                AdminSiteSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable<StorageEditDest> { entry ->
                val id = entry.toRoute<StorageEditDest>().id
                StorageEditScreen(storageId = id.toLong(), onBack = { navController.popBackStack() })
            }
            composable<MoveCopyPickerDest> {
                MoveCopyTargetPickerScreen(onTargetSelected = { navController.popBackStack() })
            }
            composable<PreviewDest>(
                typeMap = mapOf(typeOf<PreviewDestArgs>() to PreviewArgsNavType),
            ) { entry ->
                val args = entry.toRoute<PreviewDest>().args
                val fileType = runCatching { FileType.valueOf(args.fileTypeName) }.getOrDefault(FileType.Other)
                PreviewScreen(
                    name = args.name,
                    path = args.path,
                    type = fileType,
                    downloadUrl = args.downloadUrl,
                    size = args.size,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
