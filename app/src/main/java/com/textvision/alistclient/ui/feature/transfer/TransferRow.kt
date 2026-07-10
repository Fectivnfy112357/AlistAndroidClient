package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import com.textvision.alistclient.ui.components.AppAlertDialog
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.StateError
import com.textvision.alistclient.ui.theme.StateSuccessFg

/**
 * Per-state color tokens. All derive from theme/M3 — no hardcoded hex.
 */
private data class StateColors(
    val iconBg: Color,
    val iconFg: Color,
    val statusColor: Color,
    val progressBrush: Brush,
    val isCompleted: Boolean,
)

@Composable
private fun colorsFor(item: TransferEntity): StateColors {
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onTertiary = MaterialTheme.colorScheme.onTertiaryContainer
    val secondary = MaterialTheme.colorScheme.secondary
    val onSecondary = MaterialTheme.colorScheme.onSecondaryContainer
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onPrimaryContainer = MaterialTheme.colorScheme.onPrimaryContainer
    val errorContainer = MaterialTheme.colorScheme.errorContainer
    val onErrorContainer = MaterialTheme.colorScheme.onErrorContainer
    val errorFg = MaterialTheme.colorScheme.error
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    return when {
        item.status == TransferStatus.Failed ||
            item.status == TransferStatus.Interrupted -> StateColors(
            iconBg = errorContainer,
            iconFg = onErrorContainer,
            statusColor = errorFg,
            progressBrush = Brush.horizontalGradient(listOf(errorFg, errorFg)),
            isCompleted = false,
        )
        item.status == TransferStatus.Success -> StateColors(
            iconBg = primaryContainer,
            iconFg = onPrimaryContainer,
            statusColor = onSurfaceVariant,
            // 100% mint→brand gradient frozen at full
            progressBrush = Brush.horizontalGradient(listOf(CandyMint, Brand500)),
            isCompleted = true,
        )
        item.type == TransferType.Upload -> StateColors(
            iconBg = MaterialTheme.colorScheme.tertiaryContainer,
            iconFg = onTertiary,
            statusColor = primary,
            progressBrush = Brush.horizontalGradient(listOf(CandyPink, CandyMint)),
            isCompleted = false,
        )
        else -> StateColors(
            iconBg = MaterialTheme.colorScheme.secondaryContainer,
            iconFg = onSecondary,
            statusColor = MaterialTheme.colorScheme.secondary,
            progressBrush = Brush.horizontalGradient(listOf(CandyMint, Brand500)),
            isCompleted = false,
        )
    }
}

private val TransferEntity.isActive: Boolean
    get() = status in TransferStatus.activeStatuses

@Composable
fun TransferRow(
    modifier: Modifier = Modifier,
    item: TransferEntity,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    enabled: Boolean = true,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val colors = colorsFor(item)

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

    val cardAlpha = if (colors.isCompleted) 0.75f else 1f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

            // ── Header row: icon + name + status + action links ───────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // State-specific icon container (32dp)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    val icon = when {
                        item.status == TransferStatus.Failed ||
                            item.status == TransferStatus.Interrupted -> AppIcons.alert
                        item.status == TransferStatus.Success -> AppIcons.check
                        item.type == TransferType.Upload -> AppIcons.upload
                        else -> AppIcons.download
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.iconFg,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = when {
                            item.status == TransferStatus.Failed -> {
                                val reason = item.failureReason?.takeIf { it.isNotBlank() }
                                if (reason != null) "失败 · $reason" else "失败"
                            }
                            item.status == TransferStatus.Interrupted -> {
                                val reason = item.failureReason?.takeIf { it.isNotBlank() }
                                if (reason != null) "已中断 · $reason" else "已中断"
                            }
                            item.status == TransferStatus.Success -> "已完成"
                            else -> item.status.displayName
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ── Active progress bar (gradient) ────────────────────────────
            AnimatedVisibility(
                visible = item.isActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    val progress = if (item.totalBytes > 0L) {
                        (item.bytesDone.toFloat() / item.totalBytes).coerceIn(0f, 1f)
                    } else 0f
                    TransferProgress(
                        progress = progress,
                        brush = colors.progressBrush,
                    )
                    Spacer(Modifier.height(6.dp))
                    val percentText = if (item.totalBytes > 0L) {
                        val percent = (item.bytesDone.toDouble() / item.totalBytes * 100.0)
                            .coerceIn(0.0, 100.0)
                        "${"%.1f".format(percent)}%"
                    } else "准备中"
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Completed meta line (size + completed-time) ───────────────
            if (item.status == TransferStatus.Success) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "${humanizeBytes(item.totalBytes)} · 已完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Action links (retry / delete / cancel / 查看) ─────────────
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    item.status == TransferStatus.Failed ||
                        item.status == TransferStatus.Interrupted -> {
                        ActionLink(
                            text = item.status.retryLabel ?: "重试",
                            enabled = enabled,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { onRetry(item.id) },
                        )
                        ActionLink(
                            text = "删除",
                            enabled = true,
                            color = MaterialTheme.colorScheme.error,
                            onClick = { showDeleteDialog = true },
                        )
                    }
                    item.status == TransferStatus.Success -> {
                        ActionLink(
                            text = "删除",
                            enabled = true,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showDeleteDialog = true },
                        )
                    }
                    item.isActive -> {
                        ActionLink(
                            text = "取消",
                            enabled = enabled,
                            color = MaterialTheme.colorScheme.error,
                            onClick = { onCancel(item.id) },
                        )
                        ActionLink(
                            text = "删除",
                            enabled = true,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showDeleteDialog = true },
                        )
                    }
                    else -> {
                        // Cancelled / Waiting fallback
                        ActionLink(
                            text = "删除",
                            enabled = true,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { showDeleteDialog = true },
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
//  ActionLink — text button styled as inline link
// ---------------------------------------------------------------------------

@Composable
private fun ActionLink(
    text: String,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) color else color.copy(alpha = 0.38f),
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
    )
}

private fun humanizeBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unit = 0
    while (size >= 1024.0 && unit < units.lastIndex) {
        size /= 1024.0
        unit++
    }
    return if (unit == 0) "${bytes} ${units[0]}" else "%.1f %s".format(size, units[unit])
}