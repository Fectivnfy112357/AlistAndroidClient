package com.textvision.alistclient.ui.feature.home

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.feature.home.dto.HomeData
import com.textvision.alistclient.ui.feature.home.dto.PublicData
import com.textvision.alistclient.ui.feature.home.dto.SectionFailure
import com.textvision.alistclient.ui.feature.home.dto.SectionResult
import com.textvision.alistclient.ui.feature.home.dto.ServerStatsData
import com.textvision.alistclient.ui.feature.home.dto.SessionData
import com.textvision.alistclient.ui.feature.home.dto.StorageData
import com.textvision.alistclient.ui.feature.home.dto.TaskData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun fullData() = HomeData(
        publicSection = SectionResult.Ok(PublicData("My Alist", "v3.61.0", "欢迎", null, null, null, false)),
        storageSection = SectionResult.Ok(StorageData(listOf(
            StorageInfo(mountPath = "/local", driver = "Local", status = "work"),
        ))),
        serverStatsSection = SectionResult.Ok(ServerStatsData(3, 2, 0)),
        sessionSection = SectionResult.Ok(SessionData(1, 1)),
        taskSection = SectionResult.Ok(TaskData(0, 0, emptyList(), emptyList())),
    )

    @Test fun loadingShowsSkeleton() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Loading, onStorageClick = {}, onRetrySection = {}) } }
        compose.onNodeWithTag("home_loading").assertIsDisplayed()
    }

    @Test fun successRendersAllSections() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(fullData()), onStorageClick = {}, onRetrySection = {}) } }
        compose.onNodeWithText("My Alist").assertIsDisplayed()
        compose.onNodeWithTag("home_kpi_users").assertIsDisplayed()
        compose.onNodeWithTag("home_task_card").assertIsDisplayed()
        compose.onNodeWithTag("home_storage_card_/local").assertIsDisplayed()
    }

    @Test fun serverStatsFailedShowsInlineRetry() {
        val data = fullData().copy(serverStatsSection = SectionResult.Failed(SectionFailure.Network))
        var retriedKey: SectionKey? = null
        compose.setContent { MaterialTheme {
            HomeScreenContent(state = HomeUiState.Success(data), onStorageClick = {}, onRetrySection = { retriedKey = it })
        } }
        compose.onNodeWithTag("home_serverstats_retry").assertIsDisplayed()
        compose.onNodeWithTag("home_serverstats_retry").performClick()
        assertEquals(SectionKey.ServerStats, retriedKey)
    }

    @Test fun publicFailedShowsWholePageError() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Error("服务器不可用"), onStorageClick = {}, onRetrySection = {}) } }
        compose.onNodeWithText("服务器不可用").assertIsDisplayed()
    }

    @Test fun storageCardClickInvokesCallback() {
        var captured: String? = null
        compose.setContent { MaterialTheme {
            HomeScreenContent(state = HomeUiState.Success(fullData()), onStorageClick = { captured = it }, onRetrySection = {})
        } }
        compose.onNodeWithTag("home_storage_card_/local").performClick()
        assertEquals("/local", captured)
    }
}