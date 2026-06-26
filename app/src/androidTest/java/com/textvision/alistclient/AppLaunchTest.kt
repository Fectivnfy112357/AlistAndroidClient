package com.textvision.alistclient

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.ui.theme.AlistClientTheme
import org.junit.Rule
import org.junit.Test

class TestActivity : ComponentActivity()

class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val testRule = createAndroidComposeRule<TestActivity>()

    @Test
    fun launchesToLoginScreen() {
        composeRule.onNodeWithText("登录 Alist").assertIsDisplayed()
        composeRule.onNodeWithText("Alist Cloud").assertIsDisplayed()
        composeRule.onNodeWithText("服务器地址").assertIsDisplayed()
    }

    @Test
    fun authenticatedShellShowsBottomNavigation() {
        testRule.activity.setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = true)
            }
        }

        testRule.onNodeWithText("文件").assertIsDisplayed()
        testRule.onNodeWithText("传输").assertIsDisplayed()
        testRule.onNodeWithText("设置").assertIsDisplayed()
    }
}
