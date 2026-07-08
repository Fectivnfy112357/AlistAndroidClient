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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.textvision.alistclient.ui.theme.CloudMotion

internal const val AppNavMotionDurationMillis = CloudMotion.DurationMediumMillis

private val AppNavTween = CloudMotion.FloatTween

private val AppNavOffsetTween = CloudMotion.OffsetTween

private fun NavDestination?.isMainTab(): Boolean = this != null && (
    hasRoute(FilesDest::class) ||
        hasRoute(HomeDest::class) ||
        hasRoute(TransfersDest::class) ||
        hasRoute(SettingsDest::class)
    )

/** Navigation depth used to pick enter/exit direction: login = 0, tabs = 1, detail pages = 2. */
internal fun NavDestination?.navDepth(): Int = when {
    this == null -> 1
    hasRoute(LoginDest::class) -> 0
    isMainTab() -> 1
    hasRoute(MoveCopyPickerDest::class) || hasRoute(PreviewDest::class) -> 2
    else -> 1
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsEnterTransition(): EnterTransition {
    val from = initialState.destination
    val to = targetState.destination
    return when {
        from.hasRoute(LoginDest::class) && to.navDepth() == 1 ->
            fadeIn(AppNavTween, initialAlpha = 0.92f) +
                slideInVertically(AppNavOffsetTween) { it / 28 }
        from.isMainTab() && to.isMainTab() ->
            fadeIn(AppNavTween, initialAlpha = 0.82f) +
                slideInHorizontally(AppNavOffsetTween) { width -> width / 8 }
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
        from.isMainTab() && to.isMainTab() ->
            fadeOut(AppNavTween, targetAlpha = 0.82f) +
                slideOutHorizontally(AppNavOffsetTween) { width -> -width / 10 }
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
        to.navDepth() < from.navDepth() ->
            fadeIn(AppNavTween, initialAlpha = 0.94f)
        else -> hyperOsEnterTransition()
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopExitTransition(): ExitTransition {
    val from = initialState.destination
    val to = targetState.destination
    return when {
        to.navDepth() < from.navDepth() ->
            fadeOut(AppNavTween, targetAlpha = 0.88f) +
                slideOutHorizontally(AppNavOffsetTween) { width -> width / 8 }
        else -> hyperOsExitTransition()
    }
}
