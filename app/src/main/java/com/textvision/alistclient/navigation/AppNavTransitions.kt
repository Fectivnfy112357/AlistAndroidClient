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
import com.textvision.alistclient.ui.theme.AppMotion

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

private val MusicPreviewOffsetTween = tween<IntOffset>(
    durationMillis = 180,
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
    this == null -> 1
    hasRoute(LoginDest::class) -> 0
    isMainTab() -> 1
    hasRoute(MoveCopyPickerDest::class) ||
        hasRoute(PreviewDest::class) ||
        hasRoute(MusicPreviewDest::class) -> 2
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

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsEnterTransition(): EnterTransition {
    val from = initialState.destination
    val to = targetState.destination
    return when {
        from.hasRoute(LoginDest::class) && to.navDepth() == 1 ->
            fadeIn(AppNavTween, initialAlpha = 0.92f) +
                slideInVertically(AppNavOffsetTween) { it / 28 }
        from.isMainTab() && to.isMainTab() -> {
            // Forward = new page sweeps in from the right; backward = from the left.
            // The bottom-nav bar lives outside the NavHost, so a pure slide (no
            // fade) keeps the chrome steady while the content scrolls underneath,
            // which is exactly the M3 NavigationBar feel.
            val sign = tabSwapDirection(from, to)
            slideInHorizontally(TabSwapTween) { fullWidth ->
                if (sign >= 0) fullWidth else -fullWidth
            }
        }
        to.hasRoute(MusicPreviewDest::class) -> musicPreviewEnterTransition()
        to.navDepth() > from.navDepth() ->
            fadeIn(AppNavTween, initialAlpha = 0.86f) +
                slideInHorizontally(AppNavOffsetTween) { width -> width / 8 }
        else ->
            fadeIn(AppNavTween, initialAlpha = 0.94f)
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsExitTransition(): ExitTransition {
    val from = initialState.destination
    val to = targetState.destination
    return when {
        from.isMainTab() && to.isMainTab() -> {
            // Forward = old page leaves to the left; backward = leaves to the right.
            val sign = tabSwapDirection(from, to)
            slideOutHorizontally(TabSwapTween) { fullWidth ->
                if (sign >= 0) -fullWidth else fullWidth
            }
        }
        to.hasRoute(MusicPreviewDest::class) -> ExitTransition.None
        to.navDepth() > from.navDepth() ->
            fadeOut(AppNavTween, targetAlpha = 0.9f)
        to.hasRoute(LoginDest::class) ->
            fadeOut(AppNavTween, targetAlpha = 0.9f) +
                slideOutVertically(AppNavOffsetTween) { it / 28 }
        else ->
            fadeOut(AppNavTween, targetAlpha = 0.9f)
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopEnterTransition(): EnterTransition {
    val from = initialState.destination
    val to = targetState.destination
    return when {
        from.hasRoute(MusicPreviewDest::class) -> EnterTransition.None
        to.navDepth() < from.navDepth() ->
            fadeIn(AppNavTween, initialAlpha = 0.94f)
        else -> hyperOsEnterTransition()
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopExitTransition(): ExitTransition {
    val from = initialState.destination
    val to = targetState.destination
    return when {
        from.hasRoute(MusicPreviewDest::class) -> musicPreviewExitTransition()
        to.navDepth() < from.navDepth() ->
            fadeOut(AppNavTween, targetAlpha = 0.88f) +
                slideOutHorizontally(AppNavOffsetTween) { width -> width / 8 }
        else -> hyperOsExitTransition()
    }
}

/** A solid playback page sweeps over the source without blending both page trees. */
private fun musicPreviewEnterTransition(): EnterTransition =
    slideInHorizontally(MusicPreviewOffsetTween) { width -> width }

private fun musicPreviewExitTransition(): ExitTransition =
    slideOutHorizontally(MusicPreviewOffsetTween) { width -> width }
