package com.textvision.alistclient.ui.feature.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

/**
 * Plain-text preview surface — scrolling body text inside a rounded card.
 * The card border/background uses theme tokens (no raw colors). Fetches text
 * once via the suspend lambda wired to [PreviewViewModel.fetchText].
 */
@Composable
internal fun TextPreview(url: String, fetch: suspend (String) -> String) {
    var text by remember(url) { mutableStateOf("加载中") }
    LaunchedEffect(url) {
        text = fetch(url)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
    )
}

@Preview(name = "TextPreview Loading")
@Composable
private fun TextPreviewLoadingPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        TextPreview(url = "https://example.com/readme.md", fetch = { "加载中" })
    }
}

@Preview(name = "TextPreview Content")
@Composable
private fun TextPreviewContentPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        TextPreview(
            url = "https://example.com/readme.md",
            fetch = {
                """
                # 标题
                这是预览内容，多行滚动查看。占位文本用于在 Preview 中展示行高、
                内边距与主题适配。
                """.trimIndent()
            },
        )
    }
}

@Preview(name = "TextPreview Dark")
@Composable
private fun TextPreviewDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        TextPreview(
            url = "https://example.com/readme.md",
            fetch = { "深色主题预览文本。\n多行展示主题切换效果。" },
        )
    }
}