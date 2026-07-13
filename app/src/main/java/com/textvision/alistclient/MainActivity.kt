package com.textvision.alistclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionGate
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.navigation.LoginDest
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.TransferNotificationController
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("MainActivity must provide a SnackbarHostState via CompositionLocalProvider")
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var transferManager: TransferManager
    @Inject lateinit var notificationController: TransferNotificationController
    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var sessionGate: SessionGate

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        notificationController.ensureChannels()
        transferManager.initialize()
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                AlistTheme(darkMode = DarkMode.LIGHT) {
                    val navController = rememberNavController()
                    LaunchedEffect(navController) {
                        sessionGate.navEvent.collect {
                            navController.navigate(LoginDest) {
                                popUpTo(0) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                    AppNavHost(
                        startAuthenticated = authRepository.loadSavedSession() != null,
                        navController = navController,
                        snackbarHostState = snackbarHostState,
                    )
                }
            }
        }
    }
}
