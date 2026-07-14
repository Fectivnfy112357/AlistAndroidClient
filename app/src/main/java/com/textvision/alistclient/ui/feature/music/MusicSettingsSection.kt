package com.textvision.alistclient.ui.feature.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MusicSettingsSection(
    currentRoot: String,
    cacheSizeBytes: Long,
    onRootChange: (String) -> Unit,
    onClearCache: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "音乐库",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            var text by remember(currentRoot) { mutableStateOf(currentRoot) }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("音乐库根路径") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = { onRootChange(text) },
                modifier = Modifier.align(Alignment.End),
            ) { Text("保存（需重新扫描）") }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("音乐缓存", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${cacheSizeBytes / 1_000_000} MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val scope = rememberCoroutineScope()
                TextButton(onClick = { scope.launch { onClearCache() } }) { Text("清除缓存") }
            }
        }
    }
}
