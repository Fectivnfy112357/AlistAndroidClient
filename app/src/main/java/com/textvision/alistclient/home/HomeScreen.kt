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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroCard(data)
        if (data.isGuest) {
            CloudStatusBanner(text = "当前为游客身份，存储详情不可用", kind = CloudBannerKind.Info)
        }
        if (data is HomeData.Admin) {
            StorageSummaryCard(storages = data.storages)
            StorageListSection(storages = data.storages, onStorageClick = onStorageClick)
        }
    }
}

@Composable
private fun HeroCard(data: HomeData) {
    val title = data.serverTitle
    val version = data.serverVersion

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
            VersionPill(version)
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
private fun StorageSummaryCard(storages: List<StorageInfo>) {
    val total = storages.size
    val working = storages.count { it.status == "work" }
    val abnormal = total - working
    CloudCard(contentPadding = PaddingValues(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("存储概览", color = CloudTextSecondary, fontSize = 14.sp)
            Text("共 $total 个", color = CloudTextTertiary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$working", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CloudSuccessText)
            Text(" 正常", color = CloudTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            if (abnormal > 0) {
                Text(" · ", color = CloudTextTertiary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                Text("$abnormal", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CloudErrorText)
                Text(" 异常", color = CloudTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                storages.forEach { storage ->
                    StorageCard(storage, onClick = { onStorageClick(storage.mountPath) })
                }
            }
        }
    }
}

@Composable
private fun StorageCard(storage: StorageInfo, onClick: () -> Unit) {
    val isFailed = storage.status != "work"
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
        if (isFailed && !storage.status.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = storage.status,
                fontSize = 11.5.sp,
                color = CloudErrorText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String?) {
    // Alist marks a healthy storage with status == "work"; any other value
    // (including a raw driver error string) means the storage is abnormal.
    val isWork = status == "work"
    val (container, content, text) = when {
        status.isNullOrBlank() -> Triple(CloudWarningContainer, CloudWarningText, "未知")
        isWork -> Triple(CloudSuccessContainer, CloudSuccessText, "正常")
        else -> Triple(CloudErrorContainer, CloudErrorText, "异常")
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
