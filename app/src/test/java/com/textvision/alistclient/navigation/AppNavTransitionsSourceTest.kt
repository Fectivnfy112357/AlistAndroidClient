package com.textvision.alistclient.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-level guards for [AppNavTransitions]. These tests intentionally read
 * the source file rather than instantiating AnimatedContentTransitionScope
 * (which would need a Compose host), so they only verify that the structural
 * invariants the navigation design relies on are preserved.
 */
class AppNavTransitionsSourceTest {
    private val source = File("src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt").readText()

    /**
     * Extracts the body of the `from.isMainTab() && to.isMainTab()` branch from
     * a transition function. The branch always lives between the tab check and
     * the next branch marker (`to.hasRoute(...)` or `to.navDepth()`), so we
     * use those as natural delimiters.
     */
    private fun mainTabBranch(rawSource: String): String =
        rawSource.substringAfter("from.isMainTab() && to.isMainTab() ->")
            .substringBefore("to.navDepth()")
            .substringBefore("to.hasRoute")

    @Test
    fun mainTabTransitionsSlideHorizontally() {
        val enterSource = source.substringBefore("hyperOsExitTransition")
        val exitSource = source.substringAfter("hyperOsExitTransition")

        val mainTabEnter = mainTabBranch(enterSource)
        val mainTabExit = mainTabBranch(exitSource)

        // Tab-to-tab swaps must use slide (not None) so the change feels like
        // a single continuous motion rather than an instant swap, which is
        // what was producing the perceived "顿" / flash-on-arrive.
        assertFalse(
            "enter branch should not be EnterTransition.None",
            mainTabEnter.contains("EnterTransition.None"),
        )
        assertFalse(
            "exit branch should not be ExitTransition.None",
            mainTabExit.contains("ExitTransition.None"),
        )
        assertTrue(mainTabEnter.contains("slideInHorizontally"))
        assertTrue(mainTabExit.contains("slideOutHorizontally"))
        // Do not fade the full screen during tab swaps: the bottom-nav bar
        // lives outside the NavHost, so a fade here would look like a flash
        // instead of a slide. Detail-page push keeps its fade-and-slide mix.
        assertFalse(mainTabEnter.contains("fadeIn"))
        assertFalse(mainTabExit.contains("fadeOut"))
    }

    @Test
    fun tabSlideSupportsBothForwardAndBackwardDirections() {
        val enterSource = source.substringBefore("hyperOsExitTransition")
        val exitSource = source.substringAfter("hyperOsExitTransition")

        val mainTabEnter = mainTabBranch(enterSource)
        val mainTabExit = mainTabBranch(exitSource)

        // Both forward (sign >= 0, returns +fullWidth) and backward (sign < 0,
        // returns -fullWidth) directions must be expressed — otherwise the
        // popEnter / popExit fallbacks would produce one-way slides that feel
        // wrong when the user reverses direction. We check both the bare
        // `fullWidth` (forward) and the negated `-fullWidth` (backward) literal
        // in each branch.
        assertTrue(
            "enter branch must handle the backward direction (-fullWidth)",
            mainTabEnter.contains("-fullWidth"),
        )
        assertTrue(
            "exit branch must handle the backward direction (-fullWidth)",
            mainTabExit.contains("-fullWidth"),
        )
        assertTrue(
            "enter branch must handle the forward direction (+fullWidth)",
            mainTabEnter.contains("fullWidth"),
        )
        assertTrue(
            "exit branch must handle the forward direction (+fullWidth)",
            mainTabExit.contains("fullWidth"),
        )
        // The branch should consult a direction helper that distinguishes
        // forward / backward — i.e. it must not be a one-way `slideInHorizontally
        // { fullWidth }` blanket animation.
        assertTrue(
            "enter branch must consult tabSwapDirection (or equivalent index compare)",
            mainTabEnter.contains("tabSwapDirection") ||
                mainTabEnter.contains("tabIndex") ||
                mainTabEnter.contains("fromIdx") ||
                mainTabEnter.contains("toIdx"),
        )
        assertTrue(
            "exit branch must consult tabSwapDirection (or equivalent index compare)",
            mainTabExit.contains("tabSwapDirection") ||
                mainTabExit.contains("tabIndex") ||
                mainTabExit.contains("fromIdx") ||
                mainTabExit.contains("toIdx"),
        )
    }
}