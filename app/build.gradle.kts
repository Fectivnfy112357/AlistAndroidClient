plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.textvision.alistclient"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.textvision.alistclient"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.1.0-perf"

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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
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
        buildConfig = true
    }

    packaging {
        // alist: perf experiment 2026-09-16.
        // All 20 dex entries used to be deflated (74 MB of dex → 19 MB in the
        // APK). ART cannot mmap a compressed dex, so the first launch after
        // every install paid a one-off ~1.4 s `Extract dex file` stall on the
        // main thread. Storing them uncompressed lets ART mmap the dex in
        // place. Costs APK size, buys first-launch latency.
        dex {
            useLegacyPackaging = false
        }
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
    }
}

// Compose Compiler Reports & Metrics (P3 perf work):
// Run `./gradlew :app:assembleDebug` once and inspect:
//   app/build/compose_reports/        — per-class stability / skippability report
//   app/build/compose_metrics/        — module-level metrics CSV
// Pass `-PcomposeCompilerReports=true` to opt-in (off by default so a clean
// debug build isn't polluted with reports on every run).
val composeReportsEnabled = project.findProperty("composeCompilerReports") == "true"

// Kotlin 2.0+ Compose Compiler plugin (org.jetbrains.kotlin.plugin.compose)
// exposes a typed [ComposeCompilerGradlePluginExtension] with Property setters.
extensions.configure<org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension> {
    includeSourceInformation.set(composeReportsEnabled)
    if (composeReportsEnabled) {
        reportsDestination.set(layout.buildDirectory.dir("compose_reports"))
        metricsDestination.set(layout.buildDirectory.dir("compose_metrics"))
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
    implementation(libs.androidx.compose.ui.text.google.fonts)
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
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource.okhttp)
    implementation(libs.androidx.media3.ui)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    implementation(libs.androidx.datastore.preferences)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.mockk.android)
}
