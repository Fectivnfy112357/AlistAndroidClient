package com.textvision.alistclient

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesToLoginScreen() {
        composeRule.onNodeWithText("登录 Alist").assertIsDisplayed()
    }
}
