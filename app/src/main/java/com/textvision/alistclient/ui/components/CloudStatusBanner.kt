package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudErrorContainer
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudWarningContainer
import com.textvision.alistclient.ui.theme.CloudWarningText

enum class CloudBannerKind { Info, Warning, Error }

@Deprecated(
    message = "使用 M3 Expressive 组件替代；Phase 4 删除",
    replaceWith = ReplaceWith("StatusBanner(kind, message, actionLabel, onAction, modifier)"),
)
@Composable
fun CloudStatusBanner(
    text: String,
    modifier: Modifier = Modifier,
    kind: CloudBannerKind = CloudBannerKind.Info,
) {
    val colors = when (kind) {
        CloudBannerKind.Info -> BannerColors(CloudPrimarySoft, CloudPrimary, Icons.Outlined.Info)
        CloudBannerKind.Warning -> BannerColors(CloudWarningContainer, CloudWarningText, Icons.Outlined.WarningAmber)
        CloudBannerKind.Error -> BannerColors(CloudErrorContainer, CloudErrorText, Icons.Outlined.ErrorOutline)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CloudShapes.Control)
            .background(colors.container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(colors.icon, contentDescription = null, tint = colors.content)
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
            color = colors.content,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private data class BannerColors(
    val container: Color,
    val content: Color,
    val icon: ImageVector,
)
