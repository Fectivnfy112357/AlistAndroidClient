package com.textvision.alistclient.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.admin.form.localizedLabel
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.CandyLemon
import com.textvision.alistclient.ui.theme.CandyLilac
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.StateError
import com.textvision.alistclient.ui.theme.StateErrorBg

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
internal fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    )
}

/** One theme card — segmented pill of 3 swatches + label + icon highlight when selected. */
@Composable
internal fun ThemeCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.surfaceContainerHigh
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .border(
                width = if (selected) 1.5.dp else 0.dp,
                color = border,
                shape = MaterialTheme.shapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(2.dp),
        ) {
            listOf(CandyLemon, CandyPink, CandyLilac).forEach { c ->
                Box(
                    modifier = Modifier
                        .size(width = 12.dp, height = 16.dp)
                        .background(c),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun StorageSourceRow(
    title: String,
    subtitle: String,
    driver: String,
    onClick: () -> Unit,
) {
    val gradient = storageGradient(driver)
    ListItemRow(
        leading = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.database,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        title = title,
        subtitle = subtitle,
        trailing = {
            Icon(
                AppIcons.chevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}

private fun storageGradient(driver: String): Brush {
    val d = driver.lowercase()
    return when {
        "local" in d -> Brush.linearGradient(listOf(CandyMint, CandyMint))
        "s3" in d -> Brush.linearGradient(listOf(CandyLemon, CandyPink))
        "ali" in d || "oss" in d -> Brush.linearGradient(listOf(CandyPink, CandyLilac))
        "gdrive" in d || "google" in d -> Brush.linearGradient(listOf(Brand500, Brand600))
        else -> Brush.linearGradient(listOf(Brand500, Brand600))
    }
}

@Composable
internal fun QuickSettingRow(
    item: SettingItem,
    onEdit: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    ListItemRow(
        leading = {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = displayLabel(item.key),
        subtitle = item.value?.takeIf { it.isNotBlank() } ?: "(未设置)",
        trailing = {
            Icon(
                AppIcons.chevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = { editing = true },
    )
    if (editing) {
        var text by remember { mutableStateOf(item.value.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text(displayLabel(item.key)) },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (item.key == "announcement") 3 else 1,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEdit(text)
                    editing = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editing = false }) { Text("取消") }
            },
        )
    }
}

@Composable
internal fun PrivacyPasswordRow() {
    var enabled by remember { mutableStateOf(true) }
    ListItemRow(
        leading = {
            Icon(
                AppIcons.shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = "隐私与密码",
        subtitle = "登录密码与生物识别",
        trailing = {
            Switch(
                checked = enabled,
                onCheckedChange = { enabled = it },
            )
        },
    )
}

@Composable
internal fun LogoutButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = StateErrorBg,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = StateError,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "退出登录",
                style = MaterialTheme.typography.labelLarge,
                color = StateError,
            )
        }
    }
}

internal fun displayLabel(key: String): String = localizedLabel(key)
