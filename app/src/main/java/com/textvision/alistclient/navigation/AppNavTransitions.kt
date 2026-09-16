package com.textvision.alistclient.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.AppMotion
import soup.compose.material.motion.animation.materialSharedAxisXIn
import soup.compose.material.motion.animation.materialSharedAxisXOut

internal const val AppNavMotionDurationMillis = AppMotion.DurationMediumMillis

private val AppNavTween = AppMotion.FloatTween

private val AppNavOffsetTween = AppMotion.OffsetTween

// Tab-to-tab swaps use horizontal slide with an ease-in/out curve so the
// start and end of the motion blend rather than snapping — that addresses the
// "顿" / flash-on-arrive perception that an instant swap produced. Duration
// matches [AppNavMotionDurationMillis] to keep the global nav feel consistent;
// we reuse [FastOutSlowInEasing] (already imported by [MusicPreviewOffsetTween])
// instead of introducing a new easing token in [AppMotion].
private val TabSwapTween: FiniteAnimationSpec<IntOffset> = tween(
    durationMillis = AppNavMotionDurationMillis,
    easing = FastOutSlowInEasing,
)

private fun NavDestination?.isMainTab(): Boolean = this != null && (
    hasRoute(FilesDest::class) ||
        hasRoute(HomeDest::class) ||
        hasRoute(MusicLibraryDest::class) ||
        hasRoute(TransfersDest::class) ||
        hasRoute(SettingsDest::class)
    )

/**
 * Index of a main-tab destination in the bottom-nav order
 * (Home → Files → Music → Transfers → Settings). Returns -1 for non-tab
 * destinations; the slide direction logic relies on this to decide whether a
 * swap is forward / backward / a no-op.
 */
private fun NavDestination?.tabIndex(): Int = when {
    this == null -> -1
    hasRoute(HomeDest::class) -> 0
    hasRoute(FilesDest::class) -> 1
    hasRoute(MusicLibraryDest::class) -> 2
    hasRoute(TransfersDest::class) -> 3
    hasRoute(SettingsDest::class) -> 4
    else -> -1
}

/** Navigation depth used to pick enter/exit direction: login = 0, tabs = 1, detail pages = 2. */
internal fun NavDestination?.navDepth(): Int = when {
    this != null && hasRoute(MusicPreviewDest::class) -> 2
    this == null -> 1
    hasRoute(LoginDest::class) -> 0
    hasRoute(MoveCopyPickerDest::class) ||
        hasRoute(PreviewDest::class) -> 2
    isMainTab() -> 1
    else -> 1
}

/**
 * Direction the user is moving through the bottom-tab order. +1 = forward in
 * the tab list (new tab is to the right of the old one), -1 = backward, 0 =
 * either side is not a tab or the tabs are identical.
 */
private fun tabSwapDirection(from: NavDestination?, to: NavDestination?): Int {
    val fromIdx = from.tabIndex()
    val toIdx = to.tabIndex()
    if (fromIdx < 0 || toIdx < 0 || fromIdx == toIdx) return 0
    return if (toIdx > fromIdx) 1 else -1
}

/**
 * Slide distance used by Material Motion's SharedAxis X transition
 * (see `materialSharedAxisXIn` / `materialSharedAxisXOut`). Material 3's spec
 * value is 30 dp — 75 px on the test 480-dpi / 1200-px-wide device, which
 * sits inside the 5–8 % slide window. The lambda handed to `NavHost` is
 * non-composable, so we can't read `LocalDensity` to derive the exact pixel
 * value at runtime; pinning 75 px keeps the visual identical to the spec on
 * the test device and within ±20 px on most phones.
 */
private const val SharedAxisSlidePx = 75

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsEnterTransition(): EnterTransition {
    val from = initialState.destination
    val to = targetState.destination
    val slideDistance = SharedAxisSlidePx
    return when {
        // Login → main tab: the only non shared-axis push — a vertical fade
        // slide signals "logging in, the rest of the app is loading".
        from.hasRoute(LoginDest::class) && to.navDepth() == 1 ->
            fadeIn(AppNavTween, initialAlpha = 0.92f) +
                slideInVertically(AppNavOffsetTween) { it / 28 }

        // FilesDest → FilesDest with a *different* path is a drill-down (the
        // user tapped a folder inside the file browser, not a bottom-nav tab).
        // Drill-downs share the same SharedAxis X cue as every other push so
        // the user reads a consistent "one level deeper" motion everywhere.
        from.hasRoute(FilesDest::class) && to.hasRoute(FilesDest::class) ->
            materialSharedAxisXIn(
                forward = true,
                slideDistance = slideDistance,
            )

        // Tab ↔ different tab: full-width sweep; the bottom-nav bar lives
        // outside the NavHost so this reads as M3 NavigationBar behaviour.
        // Direction (forward / backward in tab order) drives the slide sign
        // so the swap matches the user's mental model of moving right/left.
        from.isMainTab() && to.isMainTab() -> {
            val sign = tabSwapDirection(from, to)
            slideInHorizontally(TabSwapTween) { fullWidth ->
                if (sign >= 0) fullWidth else -fullWidth
            }
        }

        // Music preview: dedicated helper below so callers (and the source
        // invariant test) can reference a named transition.
        to.hasRoute(MusicPreviewDest::class) ->
            musicPreviewEnterTransition()

        // EVERY other forward transition is a drill-down navigation: folder
        // → sub-folder, file → preview, action → picker, settings → edit
        // page, etc. They all use Material Motion's SharedAxis X so the user
        // reads a consistent "I'm going one level deeper" cue everywhere.
        else -> materialSharedAxisXIn(
            forward = true,
            slideDistance = slideDistance,
        )
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsExitTransition(): ExitTransition {
    val from = initialState.destination
    val to = targetState.destination
    val slideDistance = SharedAxisSlidePx
    return when {
        // FilesDest → FilesDest drill-down: symmetric exit on the outgoing
        // SharedAxis X layer so the layer peels off rather than snapping.
        from.hasRoute(FilesDest::class) && to.hasRoute(FilesDest::class) ->
            materialSharedAxisXOut(
                forward = true,
                slideDistance = slideDistance,
            )

        // Tab ↔ different tab: see [hyperOsEnterTransition] for sign logic.
        // The exit slides off the opposite edge from the incoming enter so
        // the two layers trade places across the screen.
        from.isMainTab() && to.isMainTab() -> {
            val sign = tabSwapDirection(from, to)
            slideOutHorizontally(TabSwapTween) { fullWidth ->
                if (sign >= 0) -fullWidth else fullWidth
            }
        }

        to.hasRoute(MusicPreviewDest::class) ->
            musicPreviewExitTransition()

        // Drill-down reverse (mirror of [hyperOsEnterTransition]'s else).
        else -> materialSharedAxisXOut(
            forward = true,
            slideDistance = slideDistance,
        )
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopEnterTransition(): EnterTransition {
    val to = targetState.destination
    val slideDistance = SharedAxisSlidePx
    return when {
        // Logging out and snapping back to login is the only pop that isn't a
        // SharedAxis reverse — keep it consistent with the matching push.
        to.hasRoute(LoginDest::class) ->
            fadeIn(AppNavTween, initialAlpha = 0.94f)
        // Music preview pop: mirror of the dedicated enter transition.
        to.hasRoute(MusicPreviewDest::class) ->
            musicPreviewPopEnterTransition()
        // Every other pop enters from the trailing edge (left → right)
        // because the user is moving one level shallower.
        else -> materialSharedAxisXIn(
            forward = false,
            slideDistance = slideDistance,
        )
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopExitTransition(): ExitTransition {
    val to = targetState.destination
    val slideDistance = SharedAxisSlidePx
    return when {
        to.hasRoute(LoginDest::class) ->
            fadeOut(AppNavTween, targetAlpha = 0.88f) +
                slideOutVertically(AppNavOffsetTween) { it / 28 }
        to.hasRoute(MusicPreviewDest::class) ->
            musicPreviewPopExitTransition()
        // Mirror of [hyperOsPopEnterTransition]: leaving pages slide out to
        // the leading edge (right) during pop.
        else -> materialSharedAxisXOut(
            forward = false,
            slideDistance = slideDistance,
        )
    }
}

/**
 * Dedicated enter transition for [MusicPreviewDest]. Kept as a named helper so
 * the preview screen has a single, documented motion policy referenced by
 * tests and any future direct callers. Implemented as Material Motion's
 * SharedAxis X (forward) so it stays consistent with every other drill-down.
 */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.musicPreviewEnterTransition(): EnterTransition =
    materialSharedAxisXIn(forward = true, slideDistance = SharedAxisSlidePx)

/** Symmetric exit for [MusicPreviewDest]. See [musicPreviewEnterTransition]. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.musicPreviewExitTransition(): ExitTransition =
    materialSharedAxisXOut(forward = true, slideDistance = SharedAxisSlidePx)

/** Pop-enter for [MusicPreviewDest] — reverse direction (right → left). */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.musicPreviewPopEnterTransition(): EnterTransition =
    materialSharedAxisXIn(forward = false, slideDistance = SharedAxisSlidePx)

/** Pop-exit for [MusicPreviewDest] — reverse direction. */
internal fun AnimatedContentTransitionScope<NavBackStackEntry>.musicPreviewPopExitTransition(): ExitTransition =
    materialSharedAxisXOut(forward = false, slideDistance = SharedAxisSlidePx)