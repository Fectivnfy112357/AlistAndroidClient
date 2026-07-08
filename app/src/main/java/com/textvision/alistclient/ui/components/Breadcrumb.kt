package com.textvision.alistclient.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun Breadcrumb(
    path: String,
    onNavigate: (path: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = path.split("/").filter { it.isNotEmpty() }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier.horizontalScroll(scrollState),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onNavigate("/") }) {
            Text("/", style = MaterialTheme.typography.bodyMedium)
        }
        var currentPath = ""
        segments.forEach { segment ->
            Text(
                text = "›",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            currentPath += "/$segment"
            TextButton(onClick = { onNavigate(currentPath) }) {
                Text(segment, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
