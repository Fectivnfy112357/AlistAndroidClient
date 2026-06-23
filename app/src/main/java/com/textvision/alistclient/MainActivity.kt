package com.textvision.alistclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.ui.theme.AlistClientTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = false)
            }
        }
    }
}
