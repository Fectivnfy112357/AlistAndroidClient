package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.ui.components.AppAlertDialog

private val ActiveTransferStatuses = setOf(
    TransferStatus.Waiting,
    TransferStatus.Uploading,
    TransferStatus.Downloading,
)

private val TransferEntity.statusText: String
    get() = status.displayName + (failureReason?.let { "：$it" } ?: "")

private val TransferEntity.isActive: Boolean
    get() = status in ActiveTransferStatuses

private val TransferEntity.isComplete: Boolean
    get() = status == TransferStatus.Success

@Composable
fun TransferRow(
    item: TransferEntity,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AppAlertDialog(
            title = "删除传输记录",
            message = if (item.isActive) {
                "删除后会取消当前传输，并永久删除这条记录。"
            } else {
                "将永久删除这条传输记录。"
            },
            confirmLabel = "删除",
            dismissLabel = "取消",
            onConfirm = {
                showDeleteDialog = false
                onDelete(item.id)
            },
            onDismiss = { showDeleteDialog = false },
            destructive = true,
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!item.isComplete) {
                        Text(
                            text = item.statusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (item.status == TransferStatus.Failed || item.status == TransferStatus.Interrupted) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.isActive) {
                        IconButton(
                            onClick = { onCancel(item.id) },
                            enabled = enabled,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "取消",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else if (item.status == TransferStatus.Failed || item.status == TransferStatus.Interrupted) {
                        IconButton(
                            onClick = { onRetry(item.id) },
                            enabled = enabled,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "重试",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            if (item.isActive) {
                Spacer(Modifier.height(8.dp))
                val progress = if (item.totalBytes > 0L) {
                    (item.bytesDone.toFloat() / item.totalBytes).coerceIn(0f, 1f)
                } else {
                    0f
                }
                TransferProgress(
                    progress = progress,
                    modifier = Modifier,
                )
                Text(
                    text = if (item.totalBytes > 0L) {
                        val percent = (item.bytesDone.toDouble() / item.totalBytes * 100.0).coerceIn(0.0, 100.0)
                        "${"%.1f".format(percent)}%"
                    } else {
                        "准备中"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else if (item.isComplete) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(),
                ) {
                    Text(
                        text = "完成",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
