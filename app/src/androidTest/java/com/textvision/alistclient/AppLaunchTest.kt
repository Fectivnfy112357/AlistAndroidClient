package com.textvision.alistclient

import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ActivityScenario
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.ui.screens.PreviewScreen
import com.textvision.alistclient.ui.theme.AlistClientTheme
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun launchesToLoginScreen() {
        clearSavedSession()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("登录 Alist").assertIsDisplayed()
            composeRule.onNodeWithText("Alist Cloud").assertIsDisplayed()
            composeRule.onNodeWithText("服务器地址").assertIsDisplayed()
        }
    }

    @Test
    fun launchWithSavedSessionShowsAuthenticatedShell() {
        saveSessionForLaunch()

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("我的文件").assertIsDisplayed()
            composeRule.onNodeWithText("文件").assertIsDisplayed()
            composeRule.onNodeWithText("传输").assertIsDisplayed()
            composeRule.onNodeWithText("设置").assertIsDisplayed()
        }
    }

    @Test
    fun authenticatedShellShowsBottomNavigation() {
        composeRule.setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = true)
            }
        }

        composeRule.onNodeWithText("文件").assertIsDisplayed()
        composeRule.onNodeWithText("传输").assertIsDisplayed()
        composeRule.onNodeWithText("设置").assertIsDisplayed()
    }

    @Test
    fun authenticatedShellShowsFileUploadEntry() {
        composeRule.setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = true)
            }
        }

        composeRule.onNodeWithText("我的文件").assertIsDisplayed()
        composeRule.onNodeWithText("搜索").assertIsDisplayed()
    }

    @Test
    fun authenticatedShellNavigatesToTransfersAndSettings() {
        composeRule.setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = true)
            }
        }

        composeRule.onNodeWithText("传输").performClick()
        composeRule.onNodeWithText("暂无传输任务").assertIsDisplayed()

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("清理临时预览文件").assertIsDisplayed()
        composeRule.onNodeWithText("退出登录").assertIsDisplayed()
    }

    @Test
    fun previewRouteUsesCloudShell() {
        composeRule.setContent {
            AlistClientTheme {
                PreviewScreen(
                    filePath = "missing-preview-file.txt",
                    onDownload = {},
                    onExternalOpen = {},
                )
            }
        }

        composeRule.onNodeWithText("文件预览").assertIsDisplayed()
    }

    private fun saveSessionForLaunch() {
        credentialPrefs().edit()
            .putString(SessionManager.KEY_SERVER_URL, "http://server/")
            .putString(SessionManager.KEY_USERNAME, "admin")
            .putString(SessionManager.KEY_PASSWORD, "pass")
            .putString(SessionManager.KEY_TOKEN, "tok")
            .commit()
    }

    private fun clearSavedSession() {
        credentialPrefs().edit().clear().commit()
    }

    private fun credentialPrefs() = EncryptedSharedPreferences.create(
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext,
        "secure_prefs",
        MasterKey.Builder(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
}
