package com.textvision.alistclient.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Deprecated(
    message = "使用 Breadcrumb 替代；Phase 4 删除",
    replaceWith = ReplaceWith("Breadcrumb(path, onNavigate, modifier)"),
)
@Composable
fun BreadcrumbBar(path: String, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val parts = path.trim('/').split('/').filter { it.isNotBlank() }
    Row(modifier.horizontalScroll(rememberScrollState())) {
        TextButton(onClick = { onNavigate("/") }) { Text("/") }
        var current = ""
        parts.forEach { part ->
            current += "/$part"
            TextButton(onClick = { onNavigate(current) }) { Text(part) }
        }
    }
}
