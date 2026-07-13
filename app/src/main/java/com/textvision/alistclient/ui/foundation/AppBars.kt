package com.textvision.alistclient.ui.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons

/**
 * Sticky-style top bar — title + subtitle + optional back + trailing actions.
 * Mimics prototype glass-white background with optional elevation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    subtitleIsOffline: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(AppIcons.back, contentDescription = "返回", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (subtitleIsOffline) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.tertiary,
                                ),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        }
    }
}

/**
 * Bottom navigation — 5 tabs: Home / Files / **Music** / Transfers / Settings.
 * Active state: capsule background (`primaryContainer`) + tinted icon (`primary`).
 */
@Composable
fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        BottomItem("home",      "首页", AppIcons.home),
        BottomItem("files",     "文件", AppIcons.file),
        BottomItem("music",     "音乐", AppIcons.musicNote),
        BottomItem("transfers", "传输", AppIcons.transfer),
        BottomItem("settings",  "设置", AppIcons.settings),
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.id
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .clickable { onNavigate(item.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class BottomItem(
    val id: String,
    val label: String,
    val icon: ImageVector,
)