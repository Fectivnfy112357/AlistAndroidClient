package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.ui.components.EmptyState

@Composable
fun TransferListContent(
    rows: List<TransferEntity>,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    emptyTitle: String,
    enabled: Boolean = true,
    emptyMessage: String? = null,
    modifier: Modifier = Modifier,
) {
    if (rows.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                title = emptyTitle,
                message = emptyMessage,
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        items(items = rows, key = { it.id }) { task ->
            TransferRow(
                item = task,
                onCancel = onCancel,
                onRetry = onRetry,
                onDelete = onDelete,
                enabled = enabled,
            )
        }
    }
}
