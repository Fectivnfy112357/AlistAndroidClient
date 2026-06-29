package com.textvision.alistclient.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry

internal const val AppNavMotionDurationMillis = 240

private val MainTabRoutes = listOf(
    AppRoute.Files.route,
    AppRoute.Transfers.route,
    AppRoute.Settings.route,
)

private val AppNavTween = tween<Float>(
    durationMillis = AppNavMotionDurationMillis,
    easing = FastOutSlowInEasing,
)

private val AppNavOffsetTween = tween<IntOffset>(
    durationMillis = AppNavMotionDurationMillis,
    easing = FastOutSlowInEasing,
)

internal fun mainTabIndex(route: String?): Int? = MainTabRoutes.indexOf(route).takeIf { it >= 0 }

internal fun appRouteDepth(route: String?): Int = when (route) {
    AppRoute.Login.route -> 0
    AppRoute.Files.route,
    AppRoute.Transfers.route,
    AppRoute.Settings.route -> 1
    AppRoute.MoveCopyPicker.route,
    AppRoute.Preview.route -> 2
    else -> 1
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsEnterTransition(): EnterTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    val tabDirection = tabDirection(from, to)
    return when {
        from == AppRoute.Login.route && appRouteDepth(to) == 1 ->
            fadeIn(AppNavTween, initialAlpha = 0.92f) +
                scaleIn(AppNavTween, initialScale = 0.985f) +
                slideInVertically(AppNavOffsetTween) { it / 24 }
        tabDirection != 0 ->
            fadeIn(AppNavTween, initialAlpha = 0.92f) +
                slideInHorizontally(AppNavOffsetTween) { width -> width / 14 * tabDirection }
        appRouteDepth(to) > appRouteDepth(from) ->
            fadeIn(AppNavTween, initialAlpha = 0.92f) +
                slideInHorizontally(AppNavOffsetTween) { width -> width / 10 }
        else ->
            fadeIn(AppNavTween, initialAlpha = 0.94f)
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsExitTransition(): ExitTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    val tabDirection = tabDirection(from, to)
    return when {
        tabDirection != 0 ->
            fadeOut(AppNavTween, targetAlpha = 0.88f) +
                slideOutHorizontally(AppNavOffsetTween) { width -> -width / 18 * tabDirection }
        appRouteDepth(to) > appRouteDepth(from) ->
            fadeOut(AppNavTween, targetAlpha = 0.9f) +
                scaleOut(AppNavTween, targetScale = 0.99f)
        to == AppRoute.Login.route ->
            fadeOut(AppNavTween, targetAlpha = 0.9f) +
                slideOutVertically(AppNavOffsetTween) { it / 28 }
        else ->
            fadeOut(AppNavTween, targetAlpha = 0.9f)
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopEnterTransition(): EnterTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return when {
        appRouteDepth(to) < appRouteDepth(from) ->
            fadeIn(AppNavTween, initialAlpha = 0.94f) +
                scaleIn(AppNavTween, initialScale = 0.99f)
        else -> hyperOsEnterTransition()
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopExitTransition(): ExitTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return when {
        appRouteDepth(to) < appRouteDepth(from) ->
            fadeOut(AppNavTween, targetAlpha = 0.88f) +
                slideOutHorizontally(AppNavOffsetTween) { width -> width / 10 }
        else -> hyperOsExitTransition()
    }
}

private fun tabDirection(from: String?, to: String?): Int {
    val fromIndex = mainTabIndex(from) ?: return 0
    val toIndex = mainTabIndex(to) ?: return 0
    return when {
        toIndex > fromIndex -> 1
        toIndex < fromIndex -> -1
        else -> 0
    }
}
