package com.textvision.alistclient.ui.feature.transfer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.transfer.data.TransferEntity
import com.textvision.alistclient.transfer.model.TransferStatus
import com.textvision.alistclient.transfer.model.TransferType
import com.textvision.alistclient.ui.components.EmptyState
import com.textvision.alistclient.ui.theme.AlistTheme

/**
 * Lazy list of transfer task rows. Stateless — receives pre-filtered rows from
 * [TransferListUiState.visible] and dispatches intent callbacks up.
 *
 * 4-state rows are rendered by [TransferRow]; failed/completed rows fade to
 * 0.75 alpha per prototype spec §5.2.5.
 */
@Composable
fun TransferListContent(
    modifier: Modifier = Modifier,
    rows: List<TransferEntity>,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    emptyTitle: String,
    enabled: Boolean = true,
    emptyMessage: String? = null,
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
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
    ) {
        items(items = rows, key = { it.id }) { task ->
            TransferRow(
                item = task,
                onCancel = onCancel,
                onRetry = onRetry,
                onDelete = onDelete,
                enabled = enabled,
                modifier = Modifier.animateItem(
                    fadeInSpec = spring(stiffness = Spring.StiffnessMedium),
                    placementSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                    fadeOutSpec = spring(stiffness = Spring.StiffnessMedium),
                ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
//  Preview data
// ---------------------------------------------------------------------------

internal fun previewTask(
    id: String,
    name: String = "$id.bin",
    type: TransferType = TransferType.Upload,
    status: TransferStatus = TransferStatus.Uploading,
    bytesDone: Long = 0,
    totalBytes: Long = 100,
    failureReason: String? = null,
): TransferEntity = TransferEntity(
    id = id,
    fileName = name,
    remotePath = "/$id",
    localPath = null,
    sourceUri = null,
    bytesDone = bytesDone,
    totalBytes = totalBytes,
    type = type,
    status = status,
    failureReason = failureReason,
    createdAtMillis = 1_700_000_000_000L,
    updatedAtMillis = 1_700_000_500_000L,
)

private val previewRows = listOf(
    previewTask("u1", "家庭相册.zip", TransferType.Upload, TransferStatus.Uploading, 60, 100),
    previewTask(
        "u2", "课程讲义.pdf", TransferType.Upload, TransferStatus.Failed,
        bytesDone = 0, totalBytes = 100,
        failureReason = "网络连接超时",
    ),
    previewTask("d1", "Music.flac", TransferType.Download, TransferStatus.Downloading, 20, 100),
    previewTask("d2", "Movie.mp4", TransferType.Download, TransferStatus.Success, 100, 100),
)

private val previewEmptyRows = emptyList<TransferEntity>()

@Preview(name = "TransferList Light")
@Composable
private fun TransferListLightPreview() {
    AlistTheme {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            TransferListContent(
                rows = previewRows,
                onCancel = {},
                onRetry = {},
                onDelete = {},
                emptyTitle = "暂无传输任务",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "TransferList Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TransferListDarkPreview() {
    AlistTheme {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            TransferListContent(
                rows = previewRows,
                onCancel = {},
                onRetry = {},
                onDelete = {},
                emptyTitle = "暂无传输任务",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "TransferList Empty")
@Composable
private fun TransferListEmptyPreview() {
    AlistTheme {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            TransferListContent(
                rows = previewEmptyRows,
                onCancel = {},
                onRetry = {},
                onDelete = {},
                emptyTitle = "暂无传输任务",
                emptyMessage = "完成的传输任务会显示在这里",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "TransferList LargeFont", fontScale = 1.6f)
@Composable
private fun TransferListLargeFontPreview() {
    AlistTheme {
        androidx.compose.material3.Surface(modifier = Modifier.fillMaxSize()) {
            TransferListContent(
                rows = previewRows,
                onCancel = {},
                onRetry = {},
                onDelete = {},
                emptyTitle = "暂无传输任务",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}