package com.textvision.alistclient.home

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for HomeScreen — provides baseline compilation and smoke test.
 *
 * The full Compose UI tests live in HomeScreenInstrumentedTest (androidTest/):
 * - loadingShowsSkeletonAndNoHeroText
 * - successAdminShowsHeroAndStorageList
 * - successGuestShowsInfoBanner
 * - errorShowsBannerAndRetry
 * - clickingStorageCardInvokesCallback
 *
 * Those run as instrumented tests via:
 *   ./gradlew :app:connectedDebugAndroidTest --tests com.textvision.alistclient.home.HomeScreenInstrumentedTest
 *
 * They cannot live in test/ because createComposeRule() requires ActivityScenario.launch()
 * which only works in androidTest/ (instrumented test) environments.
 */
class HomeScreenTest {

    @Test
    fun homeUiStateSealedInterfaceCompiles() {
        // Verify the HomeUiState sealed interface is properly accessible
        val loading = HomeUiState.Loading
        val error = HomeUiState.Error("test")
        assertTrue(loading is HomeUiState)
        assertTrue(error is HomeUiState)
    }
}
