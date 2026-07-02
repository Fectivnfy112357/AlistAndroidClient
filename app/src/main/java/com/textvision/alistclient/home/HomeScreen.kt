package com.textvision.alistclient.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.theme.CloudErrorContainer
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimaryDark
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSuccessContainer
import com.textvision.alistclient.ui.theme.CloudSuccessText
import com.textvision.alistclient.ui.theme.CloudSurface
import com.textvision.alistclient.ui.theme.CloudSurfaceStrong
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary
import com.textvision.alistclient.ui.theme.CloudTextTertiary
import com.textvision.alistclient.ui.theme.CloudWarningContainer
import com.textvision.alistclient.ui.theme.CloudWarningText
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
fun HomeScreen(
    onStorageClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    val currentState = state
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "首页", subtitle = " ")
        when (currentState) {
            is HomeUiState.Loading -> LoadingSkeleton()
            is HomeUiState.Error -> ErrorState(message = currentState.message, onRetry = { viewModel.refresh() })
            is HomeUiState.Success -> SuccessContent(currentState.data, onStorageClick = { mountPath ->
                viewModel.refresh()
                onStorageClick(mountPath)
            })
        }
    }
}

@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    onStorageClick: (String) -> Unit,
) {
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "首页", subtitle = " ")
        when (state) {
            is HomeUiState.Loading -> LoadingSkeleton()
            is HomeUiState.Error -> ErrorState(message = state.message, onRetry = {})
            is HomeUiState.Success -> SuccessContent(state.data, onStorageClick = onStorageClick)
        }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().testTag("home_loading"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroSkeleton()
        Spacer(Modifier.height(8.dp))
        UsageSkeleton()
        Spacer(Modifier.height(8.dp))
        StorageSkeleton()
    }
}

@Composable
private fun HeroSkeleton() {
    CloudCard(contentPadding = PaddingValues(22.dp)) {
        Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
    }
}

@Composable
private fun UsageSkeleton() {
    CloudCard(contentPadding = PaddingValues(18.dp)) {
        Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
    }
}

@Composable
private fun StorageSkeleton() {
    CloudCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(CloudSurfaceStrong))
            Spacer(Modifier.height(0.dp))
            Column(Modifier.padding(start = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
                Box(Modifier.fillMaxWidth(0.7f).height(10.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CloudStatusBanner(text = message, kind = CloudBannerKind.Error)
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun SuccessContent(data: HomeData, onStorageClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroCard(data)
        if (data.isGuest) {
            CloudStatusBanner(text = "当前为游客身份，存储详情不可用", kind = CloudBannerKind.Info)
        }
        if (data is HomeData.Admin) {
            UsageCard(usedBytes = data.usedBytes, totalBytes = data.totalBytes)
            StorageListSection(storages = data.storages, onStorageClick = onStorageClick)
        }
    }
}

@Composable
private fun HeroCard(data: HomeData) {
    val title = data.serverTitle
    val version = data.serverVersion
    val startTime = (data as? HomeData.Admin)?.startTime
    val uptime by rememberUptime(startTime)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CloudShapes.Card)
            .background(Brush.linearGradient(listOf(CloudPrimary, CloudPrimaryDark)))
            .padding(22.dp),
    ) {
        Column {
            Text(
                text = "当前服务器",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                VersionPill(version)
                Spacer(Modifier.height(0.dp))
                Text(" · ", color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp)
                Text("已运行 $uptime", color = Color.White.copy(alpha = 0.92f), fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun VersionPill(version: String?) {
    if (version.isNullOrBlank()) return
    Box(
        modifier = Modifier
            .clip(CloudShapes.Pill)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(version, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun UsageCard(usedBytes: Long, totalBytes: Long) {
    val pct = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    CloudCard(contentPadding = PaddingValues(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("总用量", color = CloudTextSecondary, fontSize = 14.sp)
            Text("${(pct * 100).toInt()}%", color = CloudTextTertiary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatBytes(usedBytes), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CloudTextPrimary)
            Text(" / ", color = CloudTextTertiary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
            Text(formatBytes(totalBytes), fontSize = 13.sp, color = CloudTextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CloudShapes.Pill),
            color = CloudPrimary,
            trackColor = CloudSurfaceStrong,
        )
    }
}

@Composable
private fun StorageListSection(storages: List<StorageInfo>, onStorageClick: (String) -> Unit) {
    Column {
        Text(
            text = "存储 (${storages.size})",
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            color = CloudTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (storages.isEmpty()) {
            CloudCard { Text("暂无存储", modifier = Modifier.padding(20.dp), color = CloudTextTertiary) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(storages, key = { it.mountPath }) { storage ->
                    StorageCard(storage, onClick = { onStorageClick(storage.mountPath) })
                }
            }
        }
    }
}

@Composable
private fun StorageCard(storage: StorageInfo, onClick: () -> Unit) {
    val isFailed = storage.status == "fail"
    val pct = if (storage.totalBytes > 0) (storage.usedBytes.toFloat() / storage.totalBytes).coerceIn(0f, 1f) else 0f
    CloudCard(
        modifier = Modifier
            .testTag("storage_card_${storage.mountPath}")
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFailed) CloudErrorContainer else CloudPrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFailed) Icons.Filled.Cloud else Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = if (isFailed) CloudErrorText else CloudPrimary,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(storage.mountPath, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CloudTextPrimary)
                Text(driverLabel(storage), fontSize = 11.5.sp, color = CloudTextTertiary)
            }
            StatusBadge(storage.status)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.weight(1f).height(8.dp).clip(CloudShapes.Pill),
                color = if (isFailed) CloudTextTertiary else CloudPrimary,
                trackColor = if (isFailed) CloudSurfaceStrong else CloudSurfaceStrong,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = if (storage.totalBytes > 0) "${formatBytes(storage.usedBytes)}/${formatBytes(storage.totalBytes)}" else "—",
                fontSize = 12.sp,
                color = CloudTextSecondary,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String?) {
    val (container, content, text) = when (status) {
        "fail" -> Triple(CloudErrorContainer, CloudErrorText, "异常")
        "work" -> Triple(CloudSuccessContainer, CloudSuccessText, "正常")
        else -> Triple(CloudWarningContainer, CloudWarningText, "未知")
    }
    Box(
        modifier = Modifier
            .clip(CloudShapes.Pill)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = content, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun driverLabel(storage: StorageInfo): String = when (storage.driver.lowercase()) {
    "local" -> "本机存储 · Local"
    "aliyundrive" -> "阿里云盘 · Aliyundrive"
    "quark" -> "夸克网盘 · Quark"
    else -> storage.driver
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return if (v >= 100 || i == 0) "${v.toInt()} ${units[i]}" else String.format("%.1f %s", v, units[i])
}

@Composable
private fun rememberUptime(startTime: Instant?): androidx.compose.runtime.State<String> {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startTime) {
        if (startTime == null) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val text = if (startTime == null) "—" else formatUptime(now - startTime.toEpochMilliseconds())
    return remember(text) { mutableStateOf(text) }
}

private fun formatUptime(deltaMs: Long): String {
    if (deltaMs <= 0) return "—"
    val d = deltaMs.days
    val h = (deltaMs - d.inWholeMilliseconds).hours
    val m = (deltaMs - d.inWholeMilliseconds - h.inWholeMilliseconds).minutes
    return when {
        d.inWholeDays > 0 -> "${d.inWholeDays} 天 ${h.inWholeHours} 小时"
        h.inWholeHours > 0 -> "${h.inWholeHours} 小时 ${m.inWholeMinutes} 分"
        else -> "${m.inWholeMinutes} 分"
    }
}
