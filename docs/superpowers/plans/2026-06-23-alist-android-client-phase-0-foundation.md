# Phase 0 Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a buildable Android project skeleton with the agreed dependencies, app shell, DI, persistence smoke tests, secure storage smoke tests, backup rules, and FileProvider.

**Architecture:** Single Gradle module Android app. Use package boundaries under `com.textvision.alistclient`. UI is Compose Navigation with login as standalone route and authenticated bottom tabs for Files, Transfers, and Settings.

**Tech Stack:** Kotlin 2.0.21, AGP 8.7.2, Compose BOM 2024.09.03, Material 3, Hilt, Room, Retrofit/OkHttp, kotlinx-serialization, Coil 3, AndroidX Security Crypto, MockK, Turbine, MockWebServer.

## Global Constraints

- minSdk = 26; targetSdk = 34; compileSdk = 34.
- Use KSP, not KAPT.
- Use `org.jetbrains.kotlin.plugin.compose`; do not configure `composeOptions.kotlinCompilerExtensionVersion`.
- Single Gradle module for MVP.
- Package name for MVP: `com.textvision.alistclient`.
- App display name: `Alist Client`.
- Phase 0 must not implement real Alist API calls.

---

## File Structure

Create these files:

```text
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/textvision/alistclient/AlistClientApp.kt
app/src/main/java/com/textvision/alistclient/MainActivity.kt
app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt
app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt
app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt
app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt
app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt
app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt
app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt
app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt
app/src/main/java/com/textvision/alistclient/di/AppModule.kt
app/src/main/java/com/textvision/alistclient/data/local/AppDatabase.kt
app/src/main/java/com/textvision/alistclient/data/local/SmokeEntity.kt
app/src/main/java/com/textvision/alistclient/data/local/SmokeDao.kt
app/src/main/java/com/textvision/alistclient/data/secure/CredentialStore.kt
app/src/main/java/com/textvision/alistclient/data/secure/EncryptedCredentialStore.kt
app/src/main/res/values/strings.xml
app/src/main/res/values/colors.xml
app/src/main/res/xml/backup_rules.xml
app/src/main/res/xml/data_extraction_rules.xml
app/src/main/res/xml/file_paths.xml
app/src/test/java/com/textvision/alistclient/data/secure/InMemoryCredentialStoreTest.kt
app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt
```

Modify none; this is a fresh project.

---

### Task 0.1: Create Gradle project and version catalog

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`

**Interfaces:**
- Produces: Gradle project with `:app`, package `com.textvision.alistclient`, minSdk 26, targetSdk 34, compileSdk 34.
- Consumes: none.

- [ ] **Step 1: Create settings file**

Write `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AlistAndroidClient"
include(":app")
```

- [ ] **Step 2: Create root build file**

Write `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

- [ ] **Step 3: Create version catalog**

Write `gradle/libs.versions.toml` exactly from spec section `4.12`, omitting `work-runtime` from libraries because Phase 0 does not use WorkManager:

```toml
[versions]
kotlin = "2.0.21"
ksp = "2.0.21-1.0.27"
agp = "8.7.2"
compose-bom = "2024.09.03"
hilt = "2.52"
hilt-navigation-compose = "1.2.0"
retrofit = "2.11.0"
retrofit-kotlinx-serialization = "1.0.0"
okhttp = "4.12.0"
kotlinx-serialization = "1.7.3"
kotlinx-coroutines = "1.9.0"
kotlinx-datetime = "0.6.1"
room = "2.6.1"
coil = "3.0.4"
navigation-compose = "2.8.3"
security-crypto = "1.1.0-alpha06"
material3 = "1.3.0"
lifecycle = "2.8.6"
activity-compose = "1.9.3"
junit = "4.13.2"
mockk = "1.13.13"
turbine = "1.2.0"
robolectric = "4.13"
androidx-test-ext = "1.2.1"
espresso = "3.6.1"
mockwebserver = "4.12.0"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofit-kotlinx-serialization" }
okhttp-core = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinx-datetime" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
mockk-android = { group = "io.mockk", name = "mockk-android", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-test-ext" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockwebserver" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

- [ ] **Step 4: Create app Gradle file**

Write `app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.textvision.alistclient"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.textvision.alistclient"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.security.crypto)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.room.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.mockk.android)
}
```

- [ ] **Step 5: Run Gradle sync/build check**

Run:

```bash
./gradlew :app:tasks --all
```

Expected: command exits 0 and lists `assembleDebug`, `lint`, and `testDebugUnitTest` tasks.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle/libs.versions.toml app/build.gradle.kts
git commit -m "chore: create android gradle project"
```

If not in a git repository, record: `chore: create android gradle project`.

---

### Task 0.2: Add manifest, resources, backup rules, and FileProvider boundaries

**Files:**
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/xml/backup_rules.xml`
- Create: `app/src/main/res/xml/data_extraction_rules.xml`
- Create: `app/src/main/res/xml/file_paths.xml`

**Interfaces:**
- Produces: Manifest with `AlistClientApp`, `MainActivity`, Internet permission, notification permission, backup exclusions, and FileProvider authority `${applicationId}.fileprovider`.
- Consumes: package `com.textvision.alistclient` from Task 0.1.

- [ ] **Step 1: Write AndroidManifest**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".AlistClientApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.AlistClient">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
    </application>
</manifest>
```

- [ ] **Step 2: Add resources**

`app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Alist Client</string>
</resources>
```

`app/src/main/res/values/colors.xml`:

```xml
<resources>
    <color name="seed">#3F51B5</color>
</resources>
```

- [ ] **Step 3: Add backup exclusion files**

`app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="sharedpref" path="secure_prefs.xml" />
    <exclude domain="database" path="transfer_tasks.db" />
    <exclude domain="database" path="transfer_tasks.db-shm" />
    <exclude domain="database" path="transfer_tasks.db-wal" />
    <exclude domain="file" path="downloads/" />
    <exclude domain="file" path="preview/" />
</full-backup-content>
```

`app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="secure_prefs.xml" />
        <exclude domain="database" path="transfer_tasks.db" />
        <exclude domain="file" path="downloads/" />
        <exclude domain="file" path="preview/" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="secure_prefs.xml" />
        <exclude domain="database" path="transfer_tasks.db" />
    </device-transfer>
</data-extraction-rules>
```

- [ ] **Step 4: Add FileProvider paths**

`app/src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <files-path name="downloads" path="downloads/" />
    <cache-path name="preview" path="preview/" />
</paths>
```

- [ ] **Step 5: Build manifest resources**

Run:

```bash
./gradlew :app:processDebugMainManifest :app:mergeDebugResources
```

Expected: both tasks complete with `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/AndroidManifest.xml app/src/main/res
git commit -m "chore: add manifest backup rules and file provider"
```

---

### Task 0.3: Add Compose app shell and navigation stubs

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/AlistClientApp.kt`
- Create: `app/src/main/java/com/textvision/alistclient/MainActivity.kt`
- Create: `app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt`
- Create: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/screens/TransferScreen.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt`

**Interfaces:**
- Produces: `AppNavHost(startAuthenticated: Boolean)` composable.
- Produces: bottom tabs with text `文件`, `传输`, `设置` after login route.
- Consumes: Manifest points to `MainActivity` and `AlistClientApp`.

- [ ] **Step 1: Add Hilt application and MainActivity**

`AlistClientApp.kt`:

```kotlin
package com.textvision.alistclient

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AlistClientApp : Application()
```

`MainActivity.kt`:

```kotlin
package com.textvision.alistclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.textvision.alistclient.navigation.AppNavHost
import com.textvision.alistclient.ui.theme.AlistClientTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlistClientTheme {
                AppNavHost(startAuthenticated = false)
            }
        }
    }
}
```

- [ ] **Step 2: Add theme**

`Color.kt`:

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.ui.graphics.Color

val AlistBlue = Color(0xFF3F51B5)
```

`Theme.kt`:

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(primary = AlistBlue)

@Composable
fun AlistClientTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, content = content)
}
```

- [ ] **Step 3: Add routes and nav host**

`AppRoute.kt`:

```kotlin
package com.textvision.alistclient.navigation

sealed class AppRoute(val route: String) {
    data object Login : AppRoute("login")
    data object Files : AppRoute("files")
    data object Transfers : AppRoute("transfers")
    data object Settings : AppRoute("settings")
}
```

`AppNavHost.kt`:

```kotlin
package com.textvision.alistclient.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.textvision.alistclient.ui.screens.FileScreen
import com.textvision.alistclient.ui.screens.LoginScreen
import com.textvision.alistclient.ui.screens.SettingsScreen
import com.textvision.alistclient.ui.screens.TransferScreen

@Composable
fun AppNavHost(startAuthenticated: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startAuthenticated) AppRoute.Files.route else AppRoute.Login.route
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in setOf(AppRoute.Files.route, AppRoute.Transfers.route, AppRoute.Settings.route)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Files.route,
                        onClick = { navController.navigate(AppRoute.Files.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        label = { Text("文件") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Transfers.route,
                        onClick = { navController.navigate(AppRoute.Transfers.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.SyncAlt, contentDescription = null) },
                        label = { Text("传输") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == AppRoute.Settings.route,
                        onClick = { navController.navigate(AppRoute.Settings.route) { launchSingleTop = true } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("设置") }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(AppRoute.Login.route) {
                LoginScreen(onLoginSuccess = {
                    navController.navigate(AppRoute.Files.route) {
                        popUpTo(AppRoute.Login.route) { inclusive = true }
                    }
                })
            }
            composable(AppRoute.Files.route) { FileScreen() }
            composable(AppRoute.Transfers.route) { TransferScreen() }
            composable(AppRoute.Settings.route) { SettingsScreen() }
        }
    }
}
```

- [ ] **Step 4: Add screen stubs**

`LoginScreen.kt`:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Alist 登录")
        Button(onClick = onLoginSuccess) { Text("进入演示") }
    }
}
```

`FileScreen.kt`:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun FileScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("文件")
    }
}
```

`TransferScreen.kt`:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun TransferScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("传输")
    }
}
```

`SettingsScreen.kt`:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SettingsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("设置")
    }
}
```

- [ ] **Step 5: Build app shell**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient
git commit -m "feat: add compose app shell"
```

---

### Task 0.4: Add Hilt module, Room smoke database, and secure credential interface

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`
- Create: `app/src/main/java/com/textvision/alistclient/data/local/AppDatabase.kt`
- Create: `app/src/main/java/com/textvision/alistclient/data/local/SmokeEntity.kt`
- Create: `app/src/main/java/com/textvision/alistclient/data/local/SmokeDao.kt`
- Create: `app/src/main/java/com/textvision/alistclient/data/secure/CredentialStore.kt`
- Create: `app/src/main/java/com/textvision/alistclient/data/secure/EncryptedCredentialStore.kt`

**Interfaces:**
- Produces: `CredentialStore` with `saveString`, `readString`, `clearAll`.
- Produces: `AppDatabase` with `SmokeDao`; later phases add production DAOs through versioned Room migrations/destructive debug migration as specified.
- Consumes: Hilt application from Task 0.3.

- [ ] **Step 1: Add Room smoke types**

`SmokeEntity.kt`:

```kotlin
package com.textvision.alistclient.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smoke")
data class SmokeEntity(
    @PrimaryKey val id: String,
    val value: String,
)
```

`SmokeDao.kt`:

```kotlin
package com.textvision.alistclient.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SmokeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SmokeEntity)

    @Query("SELECT * FROM smoke WHERE id = :id")
    suspend fun find(id: String): SmokeEntity?
}
```

`AppDatabase.kt`:

```kotlin
package com.textvision.alistclient.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SmokeEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smokeDao(): SmokeDao
}
```

- [ ] **Step 2: Add credential store interface and implementation**

`CredentialStore.kt`:

```kotlin
package com.textvision.alistclient.data.secure

interface CredentialStore {
    fun saveString(key: String, value: String)
    fun readString(key: String): String?
    fun remove(key: String)
    fun clearAll()
}
```

`EncryptedCredentialStore.kt`:

```kotlin
package com.textvision.alistclient.data.secure

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedCredentialStore @Inject constructor(
    @ApplicationContext context: Context,
) : CredentialStore {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun saveString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun readString(key: String): String? = prefs.getString(key, null)

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }
}
```

- [ ] **Step 3: Add Hilt module**

`AppModule.kt`:

```kotlin
package com.textvision.alistclient.di

import android.content.Context
import androidx.room.Room
import com.textvision.alistclient.data.local.AppDatabase
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.data.secure.EncryptedCredentialStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CredentialModule {
    @Binds
    @Singleton
    abstract fun bindCredentialStore(impl: EncryptedCredentialStore): CredentialStore
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "transfer_tasks.db")
            .fallbackToDestructiveMigration()
            .build()
}
```

- [ ] **Step 4: Build Hilt and Room generated code**

Run:

```bash
./gradlew :app:kspDebugKotlin :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`; no Hilt missing binding errors.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/di app/src/main/java/com/textvision/alistclient/data
git commit -m "chore: add di room and secure storage foundations"
```

---

### Task 0.5: Add smoke tests and Phase 0 verification

**Files:**
- Create: `app/src/test/java/com/textvision/alistclient/data/secure/InMemoryCredentialStoreTest.kt`
- Create: `app/src/androidTest/java/com/textvision/alistclient/AppLaunchTest.kt`

**Interfaces:**
- Produces: smoke tests proving unit test and instrumentation test wiring.
- Consumes: app shell text from Task 0.3.

- [ ] **Step 1: Add unit smoke test**

`InMemoryCredentialStoreTest.kt`:

```kotlin
package com.textvision.alistclient.data.secure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryCredentialStoreTest {
    private class InMemoryCredentialStore : CredentialStore {
        private val values = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { values[key] = value }
        override fun readString(key: String): String? = values[key]
        override fun remove(key: String) { values.remove(key) }
        override fun clearAll() { values.clear() }
    }

    @Test
    fun saveReadRemoveAndClearWork() {
        val store = InMemoryCredentialStore()
        store.saveString("token", "abc")
        assertEquals("abc", store.readString("token"))

        store.remove("token")
        assertNull(store.readString("token"))

        store.saveString("serverUrl", "http://example.test/")
        store.saveString("username", "admin")
        store.clearAll()
        assertNull(store.readString("serverUrl"))
        assertNull(store.readString("username"))
    }
}
```

- [ ] **Step 2: Add instrumentation launch test**

`AppLaunchTest.kt`:

```kotlin
package com.textvision.alistclient

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesToLoginScreen() {
        composeRule.onNodeWithText("Alist 登录").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: Run unit tests**

Run:

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`; `InMemoryCredentialStoreTest` passes.

- [ ] **Step 4: Run lint**

Run:

```bash
./gradlew :app:lintDebug
```

Expected: `BUILD SUCCESSFUL` and zero ERROR-level lint findings.

- [ ] **Step 5: Run debug build**

Run:

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`; APK exists at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 6: Run instrumentation test if emulator/device is available**

Run:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected with device: `BUILD SUCCESSFUL` and `launchesToLoginScreen` passes.

If no device is connected, expected failure contains `com.android.builder.testing.api.DeviceException: No connected devices`; record this and do not claim instrumentation tests passed.

- [ ] **Step 7: Commit**

```bash
git add app/src/test app/src/androidTest
git commit -m "test: add phase zero smoke tests"
```

---

## Phase 0 Completion Gate

Run all available commands fresh:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

If an Android device or emulator is connected:

```bash
./gradlew :app:connectedDebugAndroidTest
```

Expected: `BUILD SUCCESSFUL`.

If no device is connected, record the exact no-device output and continue only with user approval.
