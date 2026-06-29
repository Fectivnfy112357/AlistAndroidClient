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
import com.textvision.alistclient.ui.theme.CloudMotion

internal const val AppNavMotionDurationMillis = CloudMotion.DurationMediumMillis

private val MainTabRoutes = listOf(
    AppRoute.Files.route,
    AppRoute.Transfers.route,
    AppRoute.Settings.route,
)

private val AppNavTween = CloudMotion.FloatTween

private val AppNavOffsetTween = CloudMotion.OffsetTween

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
    return when {
        from == AppRoute.Login.route && appRouteDepth(to) == 1 ->
            fadeIn(AppNavTween, initialAlpha = 0.92f) +
                slideInVertically(AppNavOffsetTween) { it / 28 }
        mainTabIndex(from) != null && mainTabIndex(to) != null ->
            fadeIn(AppNavTween, initialAlpha = 0.82f) +
                slideInHorizontally(AppNavOffsetTween) { width -> width / 8 }
        appRouteDepth(to) > appRouteDepth(from) ->
            fadeIn(AppNavTween, initialAlpha = 0.86f) +
                slideInHorizontally(AppNavOffsetTween) { width -> width / 8 }
        else ->
            fadeIn(AppNavTween, initialAlpha = 0.94f)
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsExitTransition(): ExitTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return when {
        mainTabIndex(from) != null && mainTabIndex(to) != null ->
            fadeOut(AppNavTween, targetAlpha = 0.82f) +
                slideOutHorizontally(AppNavOffsetTween) { width -> -width / 10 }
        appRouteDepth(to) > appRouteDepth(from) ->
            fadeOut(AppNavTween, targetAlpha = 0.9f)
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
            fadeIn(AppNavTween, initialAlpha = 0.94f)
        else -> hyperOsEnterTransition()
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopExitTransition(): ExitTransition {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return when {
        appRouteDepth(to) < appRouteDepth(from) ->
            fadeOut(AppNavTween, targetAlpha = 0.88f) +
                slideOutHorizontally(AppNavOffsetTween) { width -> width / 8 }
        else -> hyperOsExitTransition()
    }
}
