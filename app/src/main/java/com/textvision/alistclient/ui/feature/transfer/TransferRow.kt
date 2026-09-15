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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

/**
 * Per-state color tokens. All derive from theme/M3 — no hardcoded hex.
 *
 * P3: marked [@Stable] so Compose treats this as skippable when callers cache
 * it via [remember]. Previously the `colorsFor()` helper constructed a fresh
 * instance on every recomposition; with progress ticking at ~3 Hz on a 50-row
 * transfer list that meant 150 StateColors objects/sec just to render the
 * same colour table.
 */
@Stable
private data class StateColors(
    val iconBg: Color,
    val iconFg: Color,
    val statusColor: Color,
    val progressBrush: Brush,
    val isCompleted: Boolean,
    val icon: ImageVector,
)

// P2: per-state brushes are constant for a given (status, type) pair, so hoist
// them to top-level `val`s. Previously each `TransferRow` recomposition that
// hit `colorsFor` re-allocated 1-2 new `Brush.horizontalGradient(listOf(...))`
// objects; on a 50-row transfer list with progress ticking at ~3Hz after the
// upstream `distinctUntilChanged`, that was ~150 Brush allocations/sec.
private val FailedProgressBrush: Brush =
    Brush.horizontalGradient(listOf(Color(0xFFB3261E), Color(0xFFB3261E)))
private val SuccessProgressBrush: Brush =
    Brush.horizontalGradient(listOf(CandyMint, Brand500))
private val UploadProgressBrush: Brush =
    Brush.horizontalGradient(listOf(CandyPink, CandyMint))
private val DownloadProgressBrush: Brush =
    Brush.horizontalGradient(listOf(CandyMint, Brand500))

/**
 * P3: returns the [StateColors] for a given status/type pair. Theme colours
 * still come from [MaterialTheme.colorScheme], so we keep this a composable.
 * The state+type pair typically stays stable for the lifetime of a row
 * (transitions go Active → Success/Failed/Cancelled, never Upload ↔ Download),
 * so [remember(item.status, item.type)] keeps the result skippable.
 */
@Composable
private fun rememberStateColors(item: TransferEntity): StateColors {
    val scheme = MaterialTheme.colorScheme
    val isFailed = item.status == TransferStatus.Failed ||
        item.status == TransferStatus.Interrupted
    val isSuccess = item.status == TransferStatus.Success
    val isUpload = item.type == TransferType.Upload
    return remember(item.status, item.type) {
        when {
            isFailed -> StateColors(
                iconBg = scheme.errorContainer,
                iconFg = scheme.onErrorContainer,
                statusColor = scheme.error,
                progressBrush = FailedProgressBrush,
                isCompleted = false,
                icon = AppIcons.alert,
            )
            isSuccess -> StateColors(
                iconBg = scheme.primaryContainer,
                iconFg = scheme.onPrimaryContainer,
                statusColor = scheme.onSurfaceVariant,
                progressBrush = SuccessProgressBrush,
                isCompleted = true,
                icon = AppIcons.check,
            )
            isUpload -> StateColors(
                iconBg = scheme.tertiaryContainer,
                iconFg = scheme.onTertiaryContainer,
                statusColor = scheme.primary,
                progressBrush = UploadProgressBrush,
                isCompleted = false,
                icon = AppIcons.upload,
            )
            else -> StateColors(
                iconBg = scheme.secondaryContainer,
                iconFg = scheme.onSecondaryContainer,
                statusColor = scheme.secondary,
                progressBrush = DownloadProgressBrush,
                isCompleted = false,
                icon = AppIcons.download,
            )
        }
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

    // P3: cache every derivable per-row value by (status, type, bytesDone,
    // totalBytes) — progress ticks at ~3 Hz and the recompositions fire on
    // every tick. Without this, even a "loading" tick of 50 rows would rebuild
    // the StateColors, statusText, icon, etc. for rows whose inputs didn't change.
    val colors = rememberStateColors(item)
    val isActive = remember(item.status) { item.isActive }
    val statusText = remember(item.status, item.failureReason) {
        when {
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
        }
    }

    // P3: progress numeric fields. Only the `progress` Float actually drives
    // [TransferProgress]'s `animateFloatAsState`; everything else is a String.
    val progress = remember(item.bytesDone, item.totalBytes) {
        if (item.totalBytes > 0L) {
            (item.bytesDone.toFloat() / item.totalBytes).coerceIn(0f, 1f)
        } else 0f
    }
    val percentText = remember(item.bytesDone, item.totalBytes) {
        if (item.totalBytes > 0L) {
            val percent = (item.bytesDone.toDouble() / item.totalBytes * 100.0)
                .coerceIn(0.0, 100.0)
            "${"%.1f".format(percent)}%"
        } else "准备中"
    }
    val completedMeta = remember(item.totalBytes, item.status) {
        if (item.status == TransferStatus.Success) "${humanizeBytes(item.totalBytes)} · 已完成"
        else null
    }

    // P3: stabilise parent-supplied callbacks. Same reasoning as the list
    // scope — these are the leaf row, so any unstable lambda here would force
    // a full row recomposition on every progress tick.
    val onCancelState by rememberUpdatedState(onCancel)
    val onRetryState by rememberUpdatedState(onRetry)
    val onDeleteState by rememberUpdatedState(onDelete)

    // P3: pre-bind click handlers that always take item.id. Stable lambdas,
    // so the `when` block below doesn't churn closures each frame.
    val onCancelClick = remember(item.id) { { onCancelState(item.id) } }
    val onRetryClick = remember(item.id) { { onRetryState(item.id) } }
    val onDeleteClick = remember(item.id) { { showDeleteDialog = true } }

    if (showDeleteDialog) {
        AppAlertDialog(
            title = "删除传输记录",
            message = if (isActive) {
                "删除后会取消当前传输，并永久删除这条记录。"
            } else {
                "将永久删除这条传输记录。"
            },
            confirmLabel = "删除",
            dismissLabel = "取消",
            onConfirm = {
                showDeleteDialog = false
                onDeleteState(item.id)
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
                    Icon(
                        imageVector = colors.icon,
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
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // ── Active progress bar (gradient) ────────────────────────────
            AnimatedVisibility(
                visible = isActive,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    TransferProgress(
                        progress = progress,
                        brush = colors.progressBrush,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = percentText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ── Completed meta line (size + completed-time) ───────────────
            completedMeta?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = it,
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
                            onClick = onRetryClick,
                        )
                        ActionLink(
                            text = "删除",
                            enabled = true,
                            color = MaterialTheme.colorScheme.error,
                            onClick = onDeleteClick,
                        )
                    }
                    item.status == TransferStatus.Success -> {
                        ActionLink(
                            text = "删除",
                            enabled = true,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onDeleteClick,
                        )
                    }
                    isActive -> {
                        ActionLink(
                            text = "取消",
                            enabled = enabled,
                            color = MaterialTheme.colorScheme.error,
                            onClick = onCancelClick,
                        )
                        ActionLink(
                            text = "删除",
                            enabled = true,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onDeleteClick,
                        )
                    }
                    else -> {
                        // Cancelled / Waiting fallback
                        ActionLink(
                            text = "删除",
                            enabled = true,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onDeleteClick,
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