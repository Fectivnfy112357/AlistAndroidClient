package com.textvision.alistclient.ui.theme

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val PreviewCardShape = RoundedCornerShape(24.dp)
private val PreviewPanelShape = RoundedCornerShape(28.dp)
private val PreviewPillShape = RoundedCornerShape(999.dp)

@Composable
private fun ColorSwatch(name: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.size(40.dp), shape = AppShapes.small, color = color) {}
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColorScheme() {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorSwatch("primary", cs.primary)
        ColorSwatch("primaryContainer", cs.primaryContainer)
        ColorSwatch("secondary", cs.secondary)
        ColorSwatch("tertiary", cs.tertiary)
        ColorSwatch("error", cs.error)
        ColorSwatch("surface", cs.surface)
        ColorSwatch("surfaceContainerHigh", cs.surfaceContainerHigh)
    }
}

@Preview(name = "Colors Light", showBackground = true, widthDp = 320, heightDp = 520)
@Composable
private fun ColorsLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) { ColorScheme() }
    }
}

@Preview(
    name = "Colors Dark",
    showBackground = true,
    widthDp = 320,
    heightDp = 520,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ColorsDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) { ColorScheme() }
    }
}

@Preview(name = "Typography", showBackground = true, widthDp = 360)
@Composable
private fun TypographyPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Headline Small", style = MaterialTheme.typography.headlineSmall)
                Text("Title Large", style = MaterialTheme.typography.titleLarge)
                Text("Title Medium", style = MaterialTheme.typography.titleMedium)
                Text("Body Large", style = MaterialTheme.typography.bodyLarge)
                Text("Body Medium", style = MaterialTheme.typography.bodyMedium)
                Text("Label Small", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Preview(name = "AppShapes", showBackground = true, widthDp = 360)
@Composable
private fun ShapePreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AppShapes small / medium / large")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = AppShapes.small, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp, 40.dp)) {}
                    Surface(shape = AppShapes.medium, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(72.dp, 40.dp)) {}
                    Surface(shape = AppShapes.large, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(72.dp, 40.dp)) {}
                }
                Text("AppShapes Card / Panel / Pill")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = PreviewCardShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp, 40.dp)) {}
                    Surface(shape = PreviewPanelShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(72.dp, 40.dp)) {}
                    Surface(shape = PreviewPillShape, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(72.dp, 40.dp)) {}
                }
            }
        }
    }
}

@Preview(name = "AppMotion SpringFast", showBackground = true, widthDp = 360)
@Composable
private fun MotionPreview() {
    var toggle by remember { mutableStateOf(false) }
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("AppMotion.SpringFast animated width")
                val barColor = MaterialTheme.colorScheme.primary
                val width by animateFloatAsState(
                    targetValue = if (toggle) 300f else 100f,
                    animationSpec = AppMotion.SpringFast,
                    label = "fast",
                )
                Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                    drawRect(color = barColor, size = Size(width, 40f))
                }
                Button(onClick = { toggle = !toggle }) { Text("Toggle") }
            }
        }
    }
}

@Preview(name = "Theme Light", showBackground = true, widthDp = 360)
@Composable
private fun ThemeLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Light Theme", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = {}) { Text("Primary Button") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
            }
        }
    }
}

@Preview(
    name = "Theme Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ThemeDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dark Theme", style = MaterialTheme.typography.headlineSmall)
                Button(onClick = {}) { Text("Primary Button") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
            }
        }
    }
}
