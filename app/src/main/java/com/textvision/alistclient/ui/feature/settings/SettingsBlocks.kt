package com.textvision.alistclient.ui.feature.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.textvision.alistclient.admin.form.localizedLabel
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand50
import com.textvision.alistclient.ui.theme.Brand100
import com.textvision.alistclient.ui.theme.Brand200
import com.textvision.alistclient.ui.theme.Brand300
import com.textvision.alistclient.ui.theme.Brand400
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand700
import com.textvision.alistclient.ui.theme.CandyLemon
import com.textvision.alistclient.ui.theme.CandyLemonBg
import com.textvision.alistclient.ui.theme.CandyLilacBg
import com.textvision.alistclient.ui.theme.CandyLilacDeep
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyMintBg
import com.textvision.alistclient.ui.theme.CandyMintDeep
import com.textvision.alistclient.ui.theme.CandyPinkBg
import com.textvision.alistclient.ui.theme.CandyPinkDeep
import com.textvision.alistclient.ui.theme.DarkBg
import com.textvision.alistclient.ui.theme.DarkPrimaryContainer
import com.textvision.alistclient.ui.theme.Ink
import com.textvision.alistclient.ui.theme.InkMute
import com.textvision.alistclient.ui.theme.InkSoft
import com.textvision.alistclient.ui.theme.MusicMagenta
import com.textvision.alistclient.ui.theme.MusicViolet
import com.textvision.alistclient.ui.theme.StateError
import com.textvision.alistclient.ui.theme.StateErrorBg
import com.textvision.alistclient.ui.theme.StateSuccessFg
import com.textvision.alistclient.ui.theme.StateWarnFg

@Composable
internal fun SettingsHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            modifier = Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(CandyMint),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "管理你的小窝",
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
internal fun SettingsUserCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CandyMint, Brand500))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "柚",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "柚子 · admin",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "我的云端小屋 · 在线",
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = InkSoft,
            )
        }
        Surface(shape = CircleShape, color = CandyMintBg) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(StateSuccessFg))
                Text("VIP", style = MaterialTheme.typography.labelSmall, color = StateSuccessFg)
            }
        }
    }
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.04.em,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 12.dp),
    )
}

@Composable
internal fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

@Composable
internal fun ThemeCard(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    swatchStops: List<Brush>,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            )
            .border(
                width = 2.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(10.dp)),
        ) {
            swatchStops.forEach { brush ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(brush),
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Brand700 else InkSoft,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Brand700 else InkSoft,
            )
        }
    }
}

internal val LightSwatch = listOf<Brush>(
    SolidColor(Brand50),
    SolidColor(Color.White),
    SolidColor(Brand100),
)
internal val DarkSwatch = listOf<Brush>(
    SolidColor(Ink),
    SolidColor(DarkPrimaryContainer),
    SolidColor(DarkBg),
)
internal val SystemSwatch = listOf<Brush>(
    SolidColor(Brand50),
    Brush.linearGradient(listOf(Brand50, Ink)),
    SolidColor(Ink),
)

@Composable
internal fun StorageSourceRow(
    title: String,
    subtitle: String,
    driver: String,
    onClick: () -> Unit,
) {
    val visual = storageVisual(driver)
    SettingsRow(
        icon = visual.icon,
        iconBackground = visual.background,
        iconTint = visual.tint,
        title = title,
        subtitle = subtitle,
        trailing = { SettingsChevron() },
        onClick = onClick,
    )
}

private data class StorageVisual(
    val icon: ImageVector,
    val background: Brush,
    val tint: Color,
)

private fun storageVisual(driver: String): StorageVisual {
    val value = driver.lowercase()
    return when {
        "quark" in value -> StorageVisual(
            icon = AppIcons.cloud,
            background = Brush.linearGradient(listOf(CandyMintBg, CandyMintDeep)),
            tint = StateSuccessFg,
        )
        "baidu" in value -> StorageVisual(
            icon = AppIcons.database,
            background = Brush.linearGradient(listOf(CandyLilacBg, CandyLilacDeep)),
            tint = MusicViolet,
        )
        else -> StorageVisual(
            icon = AppIcons.database,
            background = Brush.linearGradient(listOf(Brand400, Brand200)),
            tint = Brand700,
        )
    }
}

@Composable
internal fun QuickSettingRow(
    item: SettingItem,
    onEdit: (String) -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    SettingsRow(
        icon = AppIcons.sparkle,
        iconBackground = Brush.linearGradient(listOf(CandyPinkBg, CandyPinkDeep)),
        iconTint = MusicMagenta,
        title = displayLabel(item.key),
        subtitle = item.value?.takeIf(String::isNotBlank) ?: "(未设置)",
        trailing = { SettingsChevron() },
        onClick = { editing = true },
    )
    if (editing) {
        var text by remember(item.value) { mutableStateOf(item.value.orEmpty()) }
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
                TextButton(
                    onClick = {
                        onEdit(text)
                        editing = false
                    },
                ) { Text("保存") }
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
    SettingsRow(
        icon = AppIcons.shield,
        iconBackground = Brush.linearGradient(listOf(CandyLemonBg, CandyLemon)),
        iconTint = StateWarnFg,
        title = "隐私与密码",
        subtitle = "指纹解锁 · 自动登录",
        trailing = {
            PrototypeSwitch(
                checked = enabled,
                onCheckedChange = { enabled = it },
            )
        },
    )
}

@Composable
private fun PrototypeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbOffset by animateDpAsState(if (checked) 18.dp else 0.dp, label = "switchThumb")
    Box(
        modifier = modifier
            .size(width = 48.dp, height = 48.dp)
            .toggleable(checked, role = Role.Switch, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 22.dp)
                .clip(CircleShape)
                .background(
                    if (checked) Brush.linearGradient(listOf(Brand500, CandyMint))
                    else SolidColor(MaterialTheme.colorScheme.outline),
                )
                .padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
    }
}

@Composable
internal fun CleanPreviewRow(onClick: () -> Unit) {
    SettingsRow(
        icon = AppIcons.broom,
        iconBackground = Brush.linearGradient(listOf(CandyMintBg, CandyMintDeep)),
        iconTint = StateSuccessFg,
        title = "清理临时预览文件",
        subtitle = "已使用 234 MB",
        trailing = { SettingsChevron() },
        onClick = onClick,
    )
}

@Composable
internal fun AdvancedSettingsRow(onClick: () -> Unit) {
    SettingsRow(
        icon = AppIcons.settings,
        iconBackground = Brush.linearGradient(listOf(Brand400, Brand200)),
        iconTint = Brand700,
        title = "完整设置",
        subtitle = "全部站点设置项",
        trailing = { SettingsChevron() },
        onClick = onClick,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBackground: Brush,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .let { base ->
            if (onClick == null) base else base.clickable(
                role = Role.Button,
                onClickLabel = title,
                onClick = onClick,
            )
        }
        .padding(vertical = 12.dp)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = InkMute,
            )
        }
        trailing()
    }
}

@Composable
private fun SettingsChevron() {
    Icon(
        imageVector = AppIcons.chevronRight,
        contentDescription = null,
        tint = InkMute,
        modifier = Modifier.size(14.dp),
    )
}

@Composable
internal fun LogoutButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            border = BorderStroke(1.5.dp, StateErrorBg),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.logout,
                    contentDescription = null,
                    tint = StateError,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text("退出登录", style = MaterialTheme.typography.labelLarge, color = StateError)
            }
        }
    }
}

internal fun displayLabel(key: String): String = when (key) {
    "announcement" -> "站点公告"
    else -> localizedLabel(key)
}
