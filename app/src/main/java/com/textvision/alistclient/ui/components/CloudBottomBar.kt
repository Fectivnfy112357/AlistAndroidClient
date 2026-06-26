package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.navigation.AppRoute
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurface
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun CloudBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(CloudShapes.BottomBar)
            .background(CloudSurface)
            .height(64.dp)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CloudBottomBarItem(
            selected = currentRoute == AppRoute.Files.route,
            icon = Icons.Default.Folder,
            label = "文件",
            onClick = { onNavigate(AppRoute.Files.route) },
        )
        CloudBottomBarItem(
            selected = currentRoute == AppRoute.Transfers.route,
            icon = Icons.Default.SyncAlt,
            label = "传输",
            onClick = { onNavigate(AppRoute.Transfers.route) },
        )
        CloudBottomBarItem(
            selected = currentRoute == AppRoute.Settings.route,
            icon = Icons.Default.Settings,
            label = "设置",
            onClick = { onNavigate(AppRoute.Settings.route) },
        )
    }
}

@Composable
private fun CloudBottomBarItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(CloudShapes.Control)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 28.dp)
                .clip(CloudShapes.Pill)
                .background(if (selected) CloudPrimarySoft else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) CloudPrimary else CloudTextSecondary,
            )
        }
        Text(
            text = label,
            modifier = Modifier.padding(top = 3.dp),
            color = if (selected) CloudTextPrimary else CloudTextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
