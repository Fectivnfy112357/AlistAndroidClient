package com.textvision.alistclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionGate
import com.textvision.alistclient.debug.TraceMarkers
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.navigation.LoginDest
import com.textvision.alistclient.startup.AppStartupWarmer
import com.textvision.alistclient.transfer.TransferManager
import com.textvision.alistclient.transfer.TransferNotificationController
import com.textvision.alistclient.ui.foundation.SplashGate
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
    @Inject lateinit var appStartupWarmer: AppStartupWarmer

    override fun onCreate(savedInstanceState: Bundle?) {
        // alist: temporary jank instrumentation, see TraceMarkers. This is the
        // t0 reference every other marker is read against.
        val cookie = TraceMarkers.begin("app:onCreate")
        try {
            enableEdgeToEdge()
            super.onCreate(savedInstanceState)
            notificationController.ensureChannels()
            transferManager.initialize()

            // Show the splash gate only on the first onCreate of this process.
            // Config changes, theme changes, and recreation after a process kill
            // restart (with a non-null savedInstanceState) skip the gate so the
            // user never sees the splash twice in a row.
            val showGate = savedInstanceState == null

            setContent {
                AlistTheme(darkMode = DarkMode.LIGHT) {
                    RootContent(
                        warmer = appStartupWarmer,
                        showGate = showGate,
                        snackbarHostState = remember { SnackbarHostState() },
                        startAuthenticated = authRepository.loadSavedSession() != null,
                        sessionGate = sessionGate,
                    )
                }
            }
        } finally {
            TraceMarkers.end("app:onCreate", cookie)
        }
    }
}

@Composable
private fun RootContent(
    warmer: AppStartupWarmer,
    showGate: Boolean,
    snackbarHostState: SnackbarHostState,
    startAuthenticated: Boolean,
    sessionGate: SessionGate,
) {
    var ready by remember { mutableStateOf(!showGate) }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        if (!ready) {
            SplashGate(
                warmer = warmer,
                onReady = { ready = true },
            )
        } else {
            // alist: temporary jank instrumentation, see TraceMarkers.
            // Marks the frame on which the gate falls and the NavHost mounts.
            val cookie = TraceMarkers.begin("app:appRoot")
            AppRoot(
                startAuthenticated = startAuthenticated,
                snackbarHostState = snackbarHostState,
                sessionGate = sessionGate,
            )
            TraceMarkers.end("app:appRoot", cookie)
        }
    }
}

@Composable
private fun AppRoot(
    startAuthenticated: Boolean,
    snackbarHostState: SnackbarHostState,
    sessionGate: SessionGate,
) {
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
        startAuthenticated = startAuthenticated,
        navController = navController,
        snackbarHostState = snackbarHostState,
    )
}