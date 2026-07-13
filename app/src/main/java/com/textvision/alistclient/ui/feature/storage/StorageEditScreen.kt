package com.textvision.alistclient.ui.feature.storage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cookie
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.cookie.CookieSites
import com.textvision.alistclient.admin.cookie.WebCookieDialog
import com.textvision.alistclient.admin.storage.StorageEditUiState
import com.textvision.alistclient.admin.storage.StorageEditViewModel
import com.textvision.alistclient.ui.components.ActionButton
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.ButtonVariant
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand50
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyMintBg
import com.textvision.alistclient.ui.theme.CandyMintDeep
import com.textvision.alistclient.ui.theme.StateSuccessFg
import com.textvision.alistclient.ui.theme.StateWarnFg

@Composable
fun StorageEditScreen(
    storageId: Long,
    onBack: () -> Unit,
    viewModel: StorageEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(storageId) { viewModel.load(storageId) }
    LaunchedEffect((state as? StorageEditUiState.Form)?.saved) {
        if ((state as? StorageEditUiState.Form)?.saved == true) onBack()
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                title = "编辑存储",
                subtitle = (state as? StorageEditUiState.Form)?.storage?.mountPath,
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        when (val s = state) {
            is StorageEditUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            is StorageEditUiState.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(16.dp))
                StatusBanner(message = s.message, kind = BannerKind.ERROR)
            }
            is StorageEditUiState.Form -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    StorageInfoCard(
                        storageName = driverLabel(s.storage.driver),
                        mountPath = s.storage.mountPath,
                        enabled = s.enabled,
                    )
                    Spacer(Modifier.height(16.dp))

                    // 驱动参数卡 ─────────────────────────────────────────
                    SectionCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(16.dp),
                    ) {
                        Column {
                            Text(
                                text = "驱动参数",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 12.dp),
                            )
                            s.formItems.forEach { item ->
                                Spacer(Modifier.height(8.dp))
                                FieldRow(
                                    label = item.label,
                                    value = (s.fieldValues[item.name] ?: "").toString(),
                                    isCookie = item.name == "cookie",
                                    onValueChange = { viewModel.updateField(item.name, it) },
                                ) { captured ->
                                    if (item.name == "cookie") viewModel.updateField(item.name, captured)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    SectionCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "启用存储",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "禁用后文件将不再显示",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Switch(
                                checked = s.enabled,
                                onCheckedChange = { viewModel.setEnabled(it) },
                            )
                        }
                    }
                    s.errorMessage?.let {
                        Spacer(Modifier.height(10.dp))
                        StatusBanner(message = it, kind = BannerKind.ERROR)
                    }
                    Spacer(Modifier.height(16.dp))
                    ActionButton(
                        text = if (s.isSaving) "保存中…" else "保存修改",
                        onClick = { viewModel.save() },
                        enabled = !s.isSaving,
                        isLoading = s.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        variant = ButtonVariant.FILLED,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "保存成功后将自动返回",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

/** Driver short label from Alist driver name (AliyunDrive → 阿里云盘 etc.). */
private fun driverLabel(driver: String): String = when (driver) {
    "AliyunDrive" -> "阿里云盘"
    "Quark" -> "夸克网盘"
    "BaiduNetdisk" -> "百度网盘"
    "Local" -> "本地存储"
    else -> driver
}

/** Mint-gradient info card — 14dp corner, white icon puck + 启用 chip. */
@Composable
internal fun StorageInfoCard(
    storageName: String,
    mountPath: String,
    enabled: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(CandyMintBg, CandyMintDeep)))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Storage,
                    contentDescription = null,
                    tint = com.textvision.alistclient.ui.theme.StateSuccessFg,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = storageName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = com.textvision.alistclient.ui.theme.StateSuccessFg,
                )
                Text(
                    text = "挂载路径 · $mountPath",
                    style = MaterialTheme.typography.labelSmall,
                    color = com.textvision.alistclient.ui.theme.StateSuccessFg.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(com.textvision.alistclient.ui.theme.StateSuccessFg),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = if (enabled) "已启用" else "已禁用",
                        style = MaterialTheme.typography.labelMedium,
                        color = com.textvision.alistclient.ui.theme.StateSuccessFg,
                    )
                }
            }
        }
    }
}

/**
 * Single labelled input row — prototype `.field` (label) + `.input` (read-style box).
 * For cookie field, renders a `.cookie-field` with mint pill + 获取 button.
 */
@Composable
private fun FieldRow(
    label: String,
    value: String,
    isCookie: Boolean = false,
    onValueChange: (Any?) -> Unit,
    onCookieCaptured: (String) -> Unit = {},
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        if (isCookie) {
            CookieRow(value = value, onCookieCaptured = onCookieCaptured)
        } else {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.7f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.7f),
                    focusedIndicatorColor = Brand500,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    disabledIndicatorColor = MaterialTheme.colorScheme.outline,
                ),
            )
        }
    }
}

@Composable
private fun CookieRow(value: String, onCookieCaptured: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val hasCookie = value.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brand50)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Brand500),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (hasCookie) "已设置 · 30 天前更新" else "尚未设置 · 点右侧抓取",
            style = MaterialTheme.typography.labelMedium,
            color = Brand500,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Brand500, RoundedCornerShape(12.dp))
                .clickable { showDialog = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Cookie,
                contentDescription = null,
                tint = Brand500,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "重新获取",
                style = MaterialTheme.typography.labelMedium,
                color = Brand500,
            )
        }
    }
    if (showDialog) {
        // Default to Quark; admin site picker can override later.
        WebCookieDialog(
            site = CookieSites.Quark,
            onDismiss = { showDialog = false },
            onCookieCaptured = { cookie ->
                onCookieCaptured(cookie)
                showDialog = false
            },
        )
    }
}