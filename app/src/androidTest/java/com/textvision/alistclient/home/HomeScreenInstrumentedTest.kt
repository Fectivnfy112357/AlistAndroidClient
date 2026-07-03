package com.textvision.alistclient.home

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageInfo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for HomeScreen.
 *
 * Lives in androidTest/ (not test/) because createComposeRule() requires an
 * Activity launched via ActivityScenario, which is only available in instrumented
 * test environments.
 *
 * To run: ./gradlew :app:connectedDebugAndroidTest --tests com.textvision.alistclient.home.HomeScreenInstrumentedTest
 */
class HomeScreenInstrumentedTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun loadingShowsSkeletonAndNoHeroText() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Loading, onStorageClick = {}) } }
        compose.onNodeWithTag("home_loading").assertIsDisplayed()
        compose.onNodeWithText("My Alist").assertDoesNotExist()
    }

    @Test
    fun successAdminShowsHeroAndStorageList() {
        val data = HomeData.Admin(
            serverTitle = "My Alist",
            serverVersion = "v3.25.0",
            storages = listOf(StorageInfo(mountPath = "/local", driver = "Local", status = "work")),
        )
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(data), onStorageClick = {}) } }
        compose.onNodeWithText("My Alist").assertIsDisplayed()
        compose.onNodeWithText("v3.25.0").assertIsDisplayed()
        compose.onNodeWithText("/local").assertIsDisplayed()
    }

    @Test
    fun successGuestShowsInfoBanner() {
        val data = HomeData.Guest("My Alist", "v3.25.0", PublicSettings("My Alist", null, "v3.25.0"))
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(data), onStorageClick = {}) } }
        compose.onNodeWithText("当前为游客身份，存储详情不可用").assertIsDisplayed()
        compose.onNodeWithText("存储详情不可用").assertIsDisplayed()
    }

    @Test
    fun errorShowsBannerAndRetry() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Error("服务器不可用"), onStorageClick = {}) } }
        compose.onNodeWithText("服务器不可用").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
    }

    @Test
    fun clickingStorageCardInvokesCallback() {
        val data = HomeData.Admin(
            serverTitle = "My Alist", serverVersion = "v3",
            storages = listOf(StorageInfo(mountPath = "/local", driver = "Local")),
        )
        var captured: String? = null
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(data), onStorageClick = { captured = it }) } }
        compose.onNodeWithTag("storage_card_/local").performClick()
        assertEquals("/local", captured)
    }
}
