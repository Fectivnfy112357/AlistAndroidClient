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
    fun homeScreenKeepsPrototypeHeaderGradient() {
        val source = File("src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt").readText()

        assertTrue("Home screen should provide its own prototype header gradient", source.contains("HomeHeaderGradient"))
        assertTrue("Header gradient should blend into the shared background", source.contains("Color.Transparent"))
    }
}
