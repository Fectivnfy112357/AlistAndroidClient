package com.textvision.alistclient.ui.feature

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class PrototypeScreenSourceTest {
    @Test
    fun loginScreenKeepsPrototypeBrandFooter() {
        val source = File("src/main/java/com/textvision/alistclient/ui/feature/auth/LoginScreen.kt").readText()

        assertTrue("Login screen should render the prototype's candy dot footer", source.contains("LoginCandyDots"))
        assertTrue("Login screen should retain the Alist onboarding hint", source.contains("新用户？"))
        assertTrue("Login fields should use the prototype's pale white surface", source.contains("unfocusedContainerColor"))
    }

    @Test
    fun homeScreenKeepsPrototypeGreetingHeader() {
        val screen = File("src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt").readText()
        val greeting = File("src/main/java/com/textvision/alistclient/ui/feature/home/HomeGreeting.kt").readText()

        assertTrue("Home screen should render its prototype greeting header", screen.contains("HomeGreeting("))
        assertTrue("Greeting should retain the prototype welcome copy", greeting.contains("早上好 ✨"))
        assertTrue("Greeting actions should remain testable", greeting.contains("home_topbar_refresh") && greeting.contains("home_topbar_search"))
    }
}
