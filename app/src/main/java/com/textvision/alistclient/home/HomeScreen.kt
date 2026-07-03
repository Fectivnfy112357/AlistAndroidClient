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
import com.textvision.alistclient.home.dto.PublicData
import com.textvision.alistclient.home.dto.SectionFailure
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.home.dto.ServerStatsData
import com.textvision.alistclient.home.dto.SessionData
import com.textvision.alistclient.home.dto.StorageData
import com.textvision.alistclient.home.dto.TaskData
import com.textvision.alistclient.home.dto.TaskBucket
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
    HomeScreenContent(
        state = state,
        onStorageClick = onStorageClick,
        onRetrySection = { viewModel.retrySection(it) },
    )
}

@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    onStorageClick: (String) -> Unit,
    onRetrySection: (SectionKey) -> Unit,
) {
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "首页", subtitle = " ")
        when (state) {
            is HomeUiState.Loading -> LoadingSkeleton()
            is HomeUiState.Error -> ErrorState(message = state.message, onRetry = { onRetrySection(SectionKey.Public) })
            is HomeUiState.Success -> SuccessContent(data = state.data, onStorageClick = onStorageClick, onRetrySection = onRetrySection)
        }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().testTag("home_loading"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBlock(height = 28.dp)
        SkeletonBlock(widthFraction = 0.6f, height = 14.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonBlock(height = 16.dp)
        SkeletonBlock(widthFraction = 0.5f, height = 12.dp)
    }
}

@Composable
private fun SkeletonBlock(widthFraction: Float = 1f, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CloudStatusBanner(text = message, kind = CloudBannerKind.Error)
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun SuccessContent(data: HomeData, onStorageClick: (String) -> Unit, onRetrySection: (SectionKey) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroCard(data.publicSection)
        KpiRow(
            serverStats = data.serverStatsSection,
            session = data.sessionSection,
            onRetryServerStats = { onRetrySection(SectionKey.ServerStats) },
            onRetrySession = { onRetrySection(SectionKey.Session) },
        )
        TaskCard(
            task = data.taskSection,
            onRetry = { onRetrySection(SectionKey.Task) },
        )
        StorageSection(storages = data.storages, onStorageClick = onStorageClick, onRetry = { onRetrySection(SectionKey.Storage) })
    }
}

@Composable
private fun HeroCard(public: SectionResult<PublicData>) {
    val title = (public as? SectionResult.Ok)?.data?.siteTitle ?: "Alist"
    val version = (public as? SectionResult.Ok)?.data?.siteVersion
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CloudShapes.Card)
            .background(Brush.linearGradient(listOf(CloudPrimary, CloudPrimaryDark)))
            .padding(22.dp),
    ) {
        Column {
            Text("当前服务器", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!version.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(CloudShapes.Pill)
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) { Text(version, color = Color.White, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun KpiRow(
    serverStats: SectionResult<ServerStatsData>,
    session: SectionResult<SessionData>,
    onRetryServerStats: () -> Unit,
    onRetrySession: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiTile(
            modifier = Modifier.weight(1f).testTag("home_kpi_users"),
            label = "用户",
            value = (serverStats as? SectionResult.Ok)?.data?.userCount?.toString(),
            failed = serverStats is SectionResult.Failed,
            onRetry = onRetryServerStats,
            retryTag = "home_serverstats_retry",
        )
        KpiTile(
            modifier = Modifier.weight(1f),
            label = "角色",
            value = (serverStats as? SectionResult.Ok)?.data?.roleCount?.toString(),
            failed = serverStats is SectionResult.Failed,
            onRetry = onRetryServerStats,
            retryTag = "home_serverstats_retry",
        )
        KpiTile(
            modifier = Modifier.weight(1f).testTag("home_kpi_session"),
            label = "在线会话",
            value = (session as? SectionResult.Ok)?.data?.activeCount?.toString(),
            failed = session is SectionResult.Failed,
            onRetry = onRetrySession,
            retryTag = "home_session_retry",
        )
    }
}

@Composable
private fun KpiTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String?,
    failed: Boolean,
    onRetry: () -> Unit,
    retryTag: String,
) {
    CloudCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(label, fontSize = 11.sp, color = CloudTextTertiary)
        Spacer(Modifier.height(6.dp))
        when {
            failed -> {
                Text("—", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CloudTextTertiary)
                Spacer(Modifier.height(4.dp))
                TextButton(modifier = Modifier.testTag(retryTag), onClick = onRetry) { Text("重试", fontSize = 11.sp) }
            }
            else -> Text(value ?: "—", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CloudTextPrimary)
        }
    }
}

@Composable
private fun TaskCard(task: SectionResult<TaskData>, onRetry: () -> Unit) {
    CloudCard(modifier = Modifier.testTag("home_task_card"), contentPadding = PaddingValues(14.dp)) {
        when (task) {
            is SectionResult.Ok -> TaskCardContent(task.data)
            is SectionResult.Failed -> SectionFailedHint(failure = task.cause, onRetry = onRetry)
            SectionResult.Loading -> Text("—", color = CloudTextTertiary)
        }
    }
}

@Composable
private fun TaskCardContent(data: TaskData) {
    Text("后台任务", fontSize = 13.sp, color = CloudTextSecondary)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("进行中 ", fontSize = 12.sp, color = CloudTextTertiary)
        Text(data.runningCount.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CloudPrimary)
    }
    if (data.failedBucketIds.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text("部分类型加载失败：${data.failedBucketIds.joinToString(", ")}", fontSize = 10.sp, color = CloudWarningText)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        data.buckets.forEach { bucket -> BucketChip(bucket) }
    }
}

@Composable
private fun BucketChip(bucket: TaskBucket) {
    Box(
        modifier = Modifier
            .clip(CloudShapes.Pill)
            .background(CloudPrimarySoft)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) { Text("${bucket.type} ${bucket.running}", fontSize = 10.sp, color = CloudPrimary) }
}

@Composable
private fun SectionFailedHint(failure: SectionFailure, onRetry: () -> Unit) {
    val text = when (failure) {
        SectionFailure.Network -> "数据加载失败"
        SectionFailure.Unauthorized -> "需要管理员权限"
        is SectionFailure.Server -> "服务器返回 ${failure.code}"
    }
    CloudStatusBanner(text = text, kind = CloudBannerKind.Warning)
    TextButton(onClick = onRetry) { Text("重试", fontSize = 11.sp) }
}

@Composable
private fun StorageSection(storages: List<StorageInfo>, onStorageClick: (String) -> Unit, onRetry: () -> Unit) {
    Column {
        StorageSummaryStrip(storages = storages, onRetry = onRetry)
        if (storages.isEmpty()) {
            CloudCard { Text("暂无存储", modifier = Modifier.padding(20.dp), color = CloudTextTertiary) }
        } else {
            storages.forEach { storage ->
                StorageCard(storage, onClick = { onStorageClick(storage.mountPath) })
            }
        }
    }
}

@Composable
private fun StorageSummaryStrip(storages: List<StorageInfo>, onRetry: () -> Unit) {
    val total = storages.size
    val working = storages.count { it.status == "work" }
    val abnormal = total - working
    CloudCard(contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("存储概览", color = CloudTextSecondary, fontSize = 14.sp)
            Text("共 $total 个", color = CloudTextTertiary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$working", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CloudSuccessText)
            Text(" 正常", color = CloudTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            if (abnormal > 0) {
                Text(" · ", color = CloudTextTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
                Text("$abnormal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CloudErrorText)
                Text(" 异常", color = CloudTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun StorageCard(storage: StorageInfo, onClick: () -> Unit) {
    val isFailed = storage.status != "work"
    CloudCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_storage_card_${storage.mountPath}")
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp),
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
            Spacer(Modifier.height(8.dp))
            Text(storage.status, fontSize = 11.5.sp, color = CloudErrorText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatusBadge(status: String?) {
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
    ) { Text(text, color = content, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) }
}

private fun driverLabel(storage: StorageInfo): String = when (storage.driver.lowercase()) {
    "local" -> "本机存储 · Local"
    "aliyundrive" -> "阿里云盘 · Aliyundrive"
    "quark" -> "夸克网盘 · Quark"
    else -> storage.driver
}