package com.textvision.alistclient.admin.storage

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.components.ListItemRow

@Composable
fun StorageRowItem(
    storage: StorageInfo,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !storage.disabled
    ListItemRow(
        title = storage.mountPath,
        subtitle = storage.driver,
        modifier = modifier,
        onClick = onClick,
        leading = {
            Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        },
        trailing = {
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        },
    )
}
