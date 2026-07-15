package com.textvision.alistclient.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavigationPolicyTest {
    @Test
    fun sameTabDoesNotNavigate() {
        assertFalse(shouldNavigateToTab(currentTab = "music", targetTab = "music"))
    }

    @Test
    fun differentTabNavigates() {
        assertTrue(shouldNavigateToTab(currentTab = "home", targetTab = "music"))
    }
}
