package com.textvision.alistclient.navigation

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNavTransitionsSourceTest {
    private val source = File("src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt").readText()

    @Test
    fun mainTabTransitionsDoNotAnimateFullScreenBackgrounds() {
        val enterSource = source.substringBefore("internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsExitTransition")
        val exitSource = source.substringAfter("internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsExitTransition")
        val mainTabEnter = enterSource.substringAfter("from.isMainTab() && to.isMainTab() ->")
            .substringBefore("to.navDepth()")
        val mainTabExit = exitSource.substringAfter("from.isMainTab() && to.isMainTab() ->")
            .substringBefore("to.navDepth()")

        assertTrue(mainTabEnter.contains("EnterTransition.None"))
        assertTrue(mainTabExit.contains("ExitTransition.None"))
    }
}
