package com.textvision.alistclient.ui.feature.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.components.CloudEmptyState
import com.textvision.alistclient.ui.components.CloudPillButton
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft

/**
 * Fallback preview surface for modes that don't have an in-app renderer:
 * - [com.textvision.alistclient.preview.PreviewMode.TextTooLarge]
 * - [com.textvision.alistclient.preview.PreviewMode.External] (Video / Pdf / Archive / Other)
 * - [com.textvision.alistclient.preview.PreviewMode.Unavailable] (Folder / missing URL)
 *
 * Shows a message plus "下载" / "外部打开" actions. Both callbacks are no-ops when
 * the relevant capability is unavailable (handled by callers that null-check `downloadUrl`).
 */
@Composable
internal fun PreviewFallback(
    title: String,
    message: String,
    onDownload: () -> Unit,
    onExternalOpen: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CloudEmptyState(
            title = title,
            message = message,
            action = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CloudPillButton(
                        text = "下载",
                        leading = {
                            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                        },
                        onClick = onDownload,
                    )
                    CloudPillButton(
                        text = "外部打开",
                        containerColor = CloudPrimarySoft,
                        contentColor = CloudPrimary,
                        leading = {
                            Icon(
                                Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        },
                        onClick = onExternalOpen,
                    )
                }
            },
        )
    }
}