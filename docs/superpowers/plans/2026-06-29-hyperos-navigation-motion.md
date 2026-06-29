# HyperOS Navigation Motion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add soft, restrained HyperOS-style navigation motion across the Android Compose app so page switches no longer flash or hard-cut.

**Architecture:** Keep the change centralized in navigation. Create a focused transition helper beside `AppNavHost`, then wire it into the existing `NavHost` via global enter/exit/pop transition lambdas. Existing screens remain unchanged.

**Tech Stack:** Kotlin 2.0.21, Android Gradle Plugin 8.7.2, Jetpack Compose BOM 2024.09.03, Navigation Compose 2.8.3, JUnit 4, Android instrumentation Compose tests.

## Global Constraints

- Use the existing `androidx.navigation.compose.NavHost` transition APIs; do not add a new animation library.
- Motion style is soft and restrained: fade + small slide, with light scale only for login/auth entry.
- Keep the bottom bar persistent for `files`, `transfers`, and `settings` routes.
- Preserve all existing route strings in `AppRoute`.
- Run GitNexus impact before editing symbols; `AppNavHost` impact is HIGH and directly affects `MainActivity.onCreate` plus Android launch tests.
- Do not commit without running `gitnexus_detect_changes()`.

---

## File Structure

- Create `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt`
  - Owns all route-order and animation policy decisions.
  - Exposes route-order helpers and transition lambdas consumed by `AppNavHost`.
- Modify `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
  - Imports transition lambdas and attaches them once to `NavHost`.
  - Does not change screen content or navigation destinations.
- Modify `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`
  - Adds one behavior test that proves navigation still reaches Settings with animated NavHost installed.
  - Keeps existing tests intact.

---

### Task 1: Add navigation transition policy helper

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt`
- Test: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Consumes: `AppRoute.Login.route`, `AppRoute.Files.route`, `AppRoute.Transfers.route`, `AppRoute.Settings.route`, `AppRoute.MoveCopyPicker.route`, `AppRoute.Preview.route`
- Produces:
  - `internal fun appRouteDepth(route: String?): Int`
  - `internal fun mainTabIndex(route: String?): Int?`
  - `internal const val AppNavMotionDurationMillis: Int`

- [ ] **Step 1: Write the route-policy helper file**

Create `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt` with this content first. This is production code, but it is pure helper code and will be covered by the instrumentation test in Task 2 before the transition wiring is used in-app.

```kotlin
package com.textvision.alistclient.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.navigation.NavBackStackEntry

internal const val AppNavMotionDurationMillis = 240

private val MainTabRoutes = listOf(
    AppRoute.Files.route,
    AppRoute.Transfers.route,
    AppRoute.Settings.route,
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
```

- [ ] **Step 2: Verify helper compiles indirectly**

Run: `./gradlew.bat compileDebugKotlin`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit helper file**

```bash
git add app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt
git commit -m "feat: add navigation motion policy helper"
```

---

### Task 2: Wire HyperOS-style transitions into AppNavHost

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt:24-84`
- Test: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Consumes:
  - `mainTabIndex(route: String?): Int?`
  - `appRouteDepth(route: String?): Int`
  - `AppNavMotionDurationMillis`
- Produces:
  - `internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsEnterTransition(): EnterTransition`
  - `internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsExitTransition(): ExitTransition`
  - `internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopEnterTransition(): EnterTransition`
  - `internal fun AnimatedContentTransitionScope<NavBackStackEntry>.hyperOsPopExitTransition(): ExitTransition`

- [ ] **Step 1: Add one instrumentation test before wiring transitions**

In `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`, add this test after `authenticatedShellNavigatesToTransfersAndSettings()` and before `previewRouteUsesCloudShell()`:

```kotlin
    @Test
    fun animatedShellCanNavigateBackToFilesFromSettings() {
        composeRule.setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = true)
            }
        }

        composeRule.onNodeWithText("设置").performClick()
        composeRule.onNodeWithText("退出登录").assertIsDisplayed()

        composeRule.onNodeWithText("文件").performClick()
        composeRule.onNodeWithText("我的文件").assertIsDisplayed()
    }
```

- [ ] **Step 2: Run the new test to confirm current navigation baseline passes**

Run: `./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#animatedShellCanNavigateBackToFilesFromSettings'`

Expected: `BUILD SUCCESSFUL`. If it fails with a Hilt component host error, run only `./gradlew.bat compileDebugAndroidTestKotlin` and do not change production code yet; the failure is a test host issue that must be corrected before continuing.

- [ ] **Step 3: Extend AppNavTransitions with animation lambdas**

Replace the content of `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt` with:

```kotlin
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

private val AppNavOffsetTween = tween<Int>(
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
```

- [ ] **Step 4: Wire the transition lambdas into NavHost**

In `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`, change the `NavHost` call from:

```kotlin
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
            ) {
```

to:

```kotlin
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { hyperOsEnterTransition() },
                exitTransition = { hyperOsExitTransition() },
                popEnterTransition = { hyperOsPopEnterTransition() },
                popExitTransition = { hyperOsPopExitTransition() },
            ) {
```

- [ ] **Step 5: Compile production and Android test code**

Run: `./gradlew.bat compileDebugKotlin compileDebugAndroidTestKotlin`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Run focused navigation tests**

Run: `./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#authenticatedShellNavigatesToTransfersAndSettings'`

Expected: `BUILD SUCCESSFUL`

Run: `./gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.textvision.alistclient.AppLaunchTest#animatedShellCanNavigateBackToFilesFromSettings'`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit transition wiring**

```bash
git add app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
git commit -m "feat: add HyperOS-style navigation transitions"
```

---

### Task 3: Build, install, and manual device verification

**Files:**
- Modify: none unless verification exposes a concrete issue.
- Test: device `192.168.0.112:36397` if still connected.

**Interfaces:**
- Consumes: Debug APK from `app/build/outputs/apk/debug/app-debug.apk`
- Produces: Installed app with global navigation transitions.

- [ ] **Step 1: Run full unit tests**

Run: `./gradlew.bat testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Build APK**

Run: `./gradlew.bat assembleDebug`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Check GitNexus change scope**

Run GitNexus detect changes with scope `all`.

Expected changed symbols include `AppNavHost` and `AppNavTransitions.kt`; affected processes should be navigation/UI launch flows only.

- [ ] **Step 4: Install APK to phone**

Run:

```powershell
& "D:\programming\devtools\android\sdk\platform-tools\adb.exe" -s 192.168.0.112:36397 install -r "D:\programming\projects\my project\alist\app\build\outputs\apk\debug\app-debug.apk"
```

Expected: `Success`

- [ ] **Step 5: Manual visual check**

On the phone, verify:

1. Open app from launcher.
2. Switch `文件 → 传输 → 设置 → 文件`.
3. Confirm each switch uses a soft fade + small horizontal movement and no white/black flash.
4. From a file row that opens preview, confirm forward/back transitions feel like entering/leaving a deeper layer.
5. Log any remaining flash as a route pair, for example `files -> settings` or `files -> preview`.

- [ ] **Step 6: Do not commit verification-only changes**

If no code changed during verification, do not make an extra commit. If code changed to correct a verified issue, rerun Steps 1-5 and commit with:

```bash
git add app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
git commit -m "fix: tune navigation transition motion"
```

---

## Self-Review

- Spec coverage: Plan covers global NavHost transitions, route hierarchy, tab direction, login entry motion, focused tests, full unit tests, build, GitNexus scope check, and phone install/manual verification.
- Placeholder scan: No TBD/TODO placeholders remain. All code steps include concrete code.
- Type consistency: Produced helper names match the lambdas wired into `AppNavHost`; route strings come from existing `AppRoute` objects; Gradle and ADB commands use existing project paths.
