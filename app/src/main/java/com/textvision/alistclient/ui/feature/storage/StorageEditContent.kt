package com.textvision.alistclient.ui.feature.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.textvision.alistclient.admin.cookie.CookieSites
import com.textvision.alistclient.admin.cookie.WebCookieDialog
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.admin.storage.StorageEditUiState
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.theme.Brand50
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyMintBg
import com.textvision.alistclient.ui.theme.Ink
import com.textvision.alistclient.ui.theme.InkMute
import com.textvision.alistclient.ui.theme.InkSoft
import com.textvision.alistclient.ui.theme.LineColor
import com.textvision.alistclient.ui.theme.StateSuccessFg

@Composable
internal fun StorageEditContent(
    form: StorageEditUiState.Form,
    onBack: () -> Unit,
    onFieldValueChange: (String, String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().background(Brand50).navigationBarsPadding(),
    ) {
        StorageEditHeader(driverName = driverLabel(form.storage.driver), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            StorageSummaryCard(
                storageName = driverLabel(form.storage.driver),
                mountPath = form.storage.mountPath,
                enabled = form.enabled,
            )
            Spacer(Modifier.height(16.dp))
            StorageParametersCard(
                form = form,
                onFieldValueChange = onFieldValueChange,
                onEnabledChange = onEnabledChange,
            )
            form.errorMessage?.let {
                Spacer(Modifier.height(10.dp))
                StatusBanner(message = it, kind = BannerKind.ERROR)
            }
            Spacer(Modifier.height(16.dp))
            SaveButton(isSaving = form.isSaving, onClick = onSave)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "保存成功后将自动返回",
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = InkMute,
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StorageEditHeader(driverName: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(62.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.82f))
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回", tint = Ink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("编辑存储", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = Ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(StateSuccessFg))
                Spacer(Modifier.width(5.dp))
                Text(driverName, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
    }
}

@Composable
private fun StorageSummaryCard(storageName: String, mountPath: String, enabled: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(CandyMintBg, Color(0xFFC2EFE0))))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Cloud, null, tint = StateSuccessFg, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(storageName, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold), color = Color(0xFF1B5A45))
            Text("挂载路径 · $mountPath", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = StateSuccessFg)
        }
        Box(Modifier.clip(RoundedCornerShape(99.dp)).background(Color.White.copy(alpha = 0.85f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(StateSuccessFg))
                Spacer(Modifier.width(5.dp))
                Text(if (enabled) "已启用" else "已禁用", style = MaterialTheme.typography.labelMedium, color = StateSuccessFg)
            }
        }
    }
}

@Composable
private fun StorageParametersCard(
    form: StorageEditUiState.Form,
    onFieldValueChange: (String, String) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.88f)).padding(16.dp),
    ) {
        Text("驱动参数", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp), color = InkSoft)
        Spacer(Modifier.height(12.dp))
        form.formItems.forEach { item ->
            StorageField(
                item = item,
                value = form.fieldValues[item.name]?.toString().orEmpty(),
                onValueChange = { onFieldValueChange(item.name, it) },
            )
            Spacer(Modifier.height(12.dp))
        }
        StorageEnabledRow(enabled = form.enabled, onEnabledChange = onEnabledChange)
    }
}

@Composable
private fun StorageField(item: FormItem, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(item.label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium), color = InkSoft)
        Spacer(Modifier.height(6.dp))
        if (item.name == "cookie") {
            CookieField(value = value, onCookieCaptured = onValueChange)
        } else {
            val isSelect = item is FormItem.Select || item is FormItem.MultiSelect
            val displayValue = when (item) {
                is FormItem.Select -> item.options.firstOrNull { it.first == value }?.second ?: value
                else -> value
            }
            BasicTextField(
                value = displayValue,
                onValueChange = { if (!isSelect) onValueChange(it) },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.7f))
                    .border(1.5.dp, LineColor.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                decorationBox = { innerTextField ->
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { innerTextField() }
                        if (isSelect) Icon(Icons.Outlined.ExpandMore, null, tint = InkMute, modifier = Modifier.size(16.dp))
                    }
                },
            )
        }
    }
}

@Composable
private fun CookieField(value: String, onCookieCaptured: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Brand50).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Brand500))
        Spacer(Modifier.width(10.dp))
        Text(if (value.isBlank()) "尚未设置 · 点右侧抓取" else "已设置 · 30 天前更新", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = Brand500, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White).border(1.dp, Brand500, RoundedCornerShape(12.dp)).clickable { showDialog = true }.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Cookie, null, tint = Brand500, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("重新获取", style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = Brand500)
        }
    }
    if (showDialog) WebCookieDialog(CookieSites.Quark, onDismiss = { showDialog = false }, onCookieCaptured = { onCookieCaptured(it); showDialog = false })
}

@Composable
private fun StorageEnabledRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("启用存储", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium), color = Ink)
            Text("禁用后文件将不再显示", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = InkMute)
        }
        PrototypeSwitch(enabled, onEnabledChange)
    }
}

@Composable
private fun PrototypeSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Box(
        Modifier.size(width = 40.dp, height = 22.dp).clip(RoundedCornerShape(99.dp)).background(if (checked) Brand500 else InkMute.copy(alpha = 0.5f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onCheckedChange(!checked) }.padding(2.dp),
    ) {
        Box(Modifier.size(18.dp).align(if (checked) Alignment.CenterEnd else Alignment.CenterStart).clip(CircleShape).background(Color.White))
    }
}

@Composable
private fun SaveButton(isSaving: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF6FB6FF), Color(0xFF4A98E8)))).clickable(enabled = !isSaving, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(if (isSaving) "保存中…" else "保存修改", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White) }
}

private fun driverLabel(driver: String): String = when (driver) {
    "AliyunDrive" -> "阿里云盘"
    "Quark" -> "夸克网盘"
    "BaiduNetdisk" -> "百度网盘"
    "Local" -> "本地存储"
    else -> driver
}
