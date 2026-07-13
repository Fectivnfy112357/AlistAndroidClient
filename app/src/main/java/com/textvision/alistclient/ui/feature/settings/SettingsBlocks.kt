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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CleaningServices
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.textvision.alistclient.admin.form.localizedLabel
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand50
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.Brand700
import com.textvision.alistclient.ui.theme.CandyLemon
import com.textvision.alistclient.ui.theme.CandyLilac
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyMintBg
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.DarkBg
import com.textvision.alistclient.ui.theme.DarkSurface
import com.textvision.alistclient.ui.theme.Ink
import com.textvision.alistclient.ui.theme.StateError
import com.textvision.alistclient.ui.theme.StateErrorBg

// ═══════════════════════════════════════════════════════════════════════════
//  Section primitives — aligned with prototype `.card-title` + `.set-row`.
// ═══════════════════════════════════════════════════════════════════════════

/** Uppercase tracked label above a card — prototype `.card-title`. */
@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(start = 4.dp, bottom = 8.dp),
    )
}

/** Hairline divider used between `.set-row` items inside a single card. */
@Composable
internal fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Theme picker — prototype `.theme-pick` + `.theme-card`.
//  Each card shows a 3-stop swatch + icon + label.
// ═══════════════════════════════════════════════════════════════════════════

/** A theme card. Each variant supplies its own 3-stop swatch gradient. */
@Composable
internal fun ThemeCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    swatchStops: List<Color>,
    modifier: Modifier = Modifier,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    else MaterialTheme.colorScheme.surface
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            swatchStops.forEach { stop ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(stop),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Light swatch — sky-blue + white + light-blue gradient. */
internal val LightSwatch = listOf(Brand50, Color.White, Brand500.copy(alpha = 0.4f))
/** Dark swatch — deep navy 3 stops. */
internal val DarkSwatch = listOf(Ink, DarkSurface, DarkBg)
/** Follow-system swatch — gradient from light to dark. */
internal val SystemSwatch = listOf(Brand50, Brand500, Ink)

// ═══════════════════════════════════════════════════════════════════════════
//  Storage source row — prototype `.set-row` with gradient icon + chevron.
// ═══════════════════════════════════════════════════════════════════════════

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
                    .clip(RoundedCornerShape(12.dp))
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
        "local" in d -> Brush.linearGradient(listOf(CandyMint, CandyMintBg))
        "s3" in d -> Brush.linearGradient(listOf(CandyLemon, CandyPink))
        "ali" in d || "oss" in d -> Brush.linearGradient(listOf(Brand500, Brand600))
        "quark" in d -> Brush.linearGradient(listOf(CandyMint, Brand600))
        "baidu" in d -> Brush.linearGradient(listOf(CandyLilac, CandyPink))
        "gdrive" in d || "google" in d -> Brush.linearGradient(listOf(Brand600, Brand700))
        else -> Brush.linearGradient(listOf(Brand500, Brand600))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Quick-setting row + privacy password switch (prototype `.set-row`).
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun QuickSettingRow(
    item: SettingItem,
    onEdit: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    ListItemRow(
        leading = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.sparkle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(18.dp),
                )
            }
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

/** Privacy row with switch (no dialog) — prototype `.switch.on`. */
@Composable
internal fun PrivacyPasswordRow() {
    var enabled by remember { mutableStateOf(true) }
    ListItemRow(
        leading = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        title = "隐私与密码",
        subtitle = "指纹解锁 · 自动登录",
        trailing = {
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        },
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Maintenance rows — prototype `.set-row` with mint broom + blue settings.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun CleanPreviewRow(onClick: () -> Unit) {
    ListItemRow(
        leading = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.CleaningServices,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        title = "清理临时预览文件",
        subtitle = "已使用 234 MB",
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

@Composable
internal fun AdvancedSettingsRow(onClick: () -> Unit) {
    ListItemRow(
        leading = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        title = "完整设置",
        subtitle = "全部站点设置项",
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

// ═══════════════════════════════════════════════════════════════════════════
//  Logout button — prototype `.btn-ghost.btn-icon-text` with red border.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun LogoutButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.5.dp,
                color = StateErrorBg,
                shape = RoundedCornerShape(18.dp),
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
            Spacer(Modifier.width(6.dp))
            Text(
                text = "退出登录",
                style = MaterialTheme.typography.labelLarge,
                color = StateError,
            )
        }
    }
}

internal fun displayLabel(key: String): String = localizedLabel(key)