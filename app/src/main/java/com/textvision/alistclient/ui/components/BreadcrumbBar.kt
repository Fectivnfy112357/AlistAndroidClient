package com.textvision.alistclient.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BreadcrumbBar(path: String, onNavigate: (String) -> Unit) {
    val parts = path.trim('/').split('/').filter { it.isNotBlank() }
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(onClick = { onNavigate("/") }) { Text("/") }
        var current = ""
        parts.forEach { part ->
            current += "/$part"
            TextButton(onClick = { onNavigate(current) }) { Text(part) }
        }
    }
}
