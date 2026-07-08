package com.textvision.alistclient.snapshot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.textvision.alistclient.ui.theme.AlistClientTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class ThemeSnapshotTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Composable
    private fun SampleContent(text: String) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color.Magenta),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }
    }

    @Test
    fun theme_light() {
        composeRule.setContent {
            AlistClientTheme(darkTheme = false, dynamicColor = false) {
                SampleContent("Alist Theme Light")
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/theme_light.png")
    }

    @Test
    fun theme_dark() {
        composeRule.setContent {
            AlistClientTheme(darkTheme = true, dynamicColor = false) {
                SampleContent("Alist Theme Dark")
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/theme_dark.png")
    }

    @Test
    fun theme_dynamic_light() {
        composeRule.setContent {
            AlistClientTheme(darkTheme = false, dynamicColor = true) {
                SampleContent("Alist Theme Dynamic")
            }
        }
        composeRule.onRoot().captureRoboImage("src/test/snapshots/images/theme_dynamic_light.png")
    }
}