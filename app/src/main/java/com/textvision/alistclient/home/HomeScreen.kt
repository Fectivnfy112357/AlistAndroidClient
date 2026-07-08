@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.textvision.alistclient.ui.components.bottomBarInset
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.theme.CloudErrorContainer
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimaryDark
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSuccessContainer
import com.textvision.alistclient.ui.theme.CloudSuccessText
import com.textvision.alistclient.ui.theme.CloudSurfaceMuted
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
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    HomeScreenContent(
        state = state,
        isRefreshing = isRefreshing,
        onStorageClick = onStorageClick,
        onRetrySection = { viewModel.retrySection(it) },
        onRefresh = { viewModel.refresh() },
    )
}

@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    isRefreshing: Boolean = false,
    onStorageClick: (String) -> Unit,
    onRetrySection: (SectionKey) -> Unit,
    onRefresh: () -> Unit = {},
) {
    CloudScaffold(bottomInset = bottomBarInset()) {
        HomeHeader(public = (state as? HomeUiState.Success)?.data?.publicSection)
        when (state) {
            is HomeUiState.Loading -> LoadingSkeleton()
            is HomeUiState.Error -> ErrorState(message = state.message, onRetry = { onRetrySection(SectionKey.Public) })
            is HomeUiState.Success -> SuccessContent(
                data = state.data,
                isRefreshing = isRefreshing,
                onStorageClick = onStorageClick,
                onRetrySection = onRetrySection,
                onRefresh = onRefresh,
            )
        }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().testTag("home_loading"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SkeletonBlock(height = 56.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBlock(widthFraction = 1f, height = 56.dp, modifier = Modifier.weight(1f))
            SkeletonBlock(widthFraction = 1f, height = 56.dp, modifier = Modifier.weight(1f))
            SkeletonBlock(widthFraction = 1f, height = 56.dp, modifier = Modifier.weight(1f))
        }
        SkeletonBlock(height = 96.dp)
    }
}

@Composable
private fun SkeletonBlock(widthFraction: Float = 1f, height: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .background(CloudSurfaceStrong)
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
private fun HomeHeader(public: SectionResult<PublicData>?) {
    val title = (public as? SectionResult.Ok)?.data?.siteTitle ?: "Alist"
    val version = (public as? SectionResult.Ok)?.data?.siteVersion
    val isOnline = public !is SectionResult.Failed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "首页",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = CloudTextPrimary,
        )
        OnlineDot(isOnline = isOnline)
        Text(
            text = if (isOnline) "在线" else "离线",
            fontSize = 11.sp,
            color = if (isOnline) CloudSuccessText else CloudErrorText,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.weight(1f))
        if (!version.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(CloudShapes.Pill)
                    .background(CloudSurfaceMuted)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = version,
                    fontSize = 10.5.sp,
                    color = CloudTextSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        // show title (Alist) at far right as a subtle site brand tag
        Text(
            text = title,
            fontSize = 12.sp,
            color = CloudTextTertiary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun OnlineDot(isOnline: Boolean) {
    val color = if (isOnline) CloudSuccessText else CloudErrorText
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color),
    )
}

@Composable
private fun SuccessContent(
    data: HomeData,
    isRefreshing: Boolean,
    onStorageClick: (String) -> Unit,
    onRetrySection: (SectionKey) -> Unit,
    onRefresh: () -> Unit,
) {
    PullToRefreshBoxWrapper(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
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
            StorageSection(
                storageSection = data.storageSection,
                onStorageClick = onStorageClick,
                onRetry = { onRetrySection(SectionKey.Storage) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullToRefreshBoxWrapper(isRefreshing: Boolean, onRefresh: () -> Unit, content: @Composable () -> Unit) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        content()
    }
}

@Composable
private fun HeroCard(public: SectionResult<PublicData>) {
    val title = (public as? SectionResult.Ok)?.data?.siteTitle ?: "Alist"
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CloudShapes.Card)
            .background(Brush.linearGradient(listOf(CloudPrimary, CloudPrimaryDark)))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title.firstOrNull()?.uppercase() ?: "A",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "当前服务器",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(CloudShapes.Pill)
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("查看文件 →", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
            retryTag = "home_serverstats_role_retry",
        )
        KpiTile(
            modifier = Modifier.weight(1f).testTag("home_kpi_session"),
            label = "在线",
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
    CloudCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Text(label, fontSize = 11.sp, color = CloudTextTertiary)
                Spacer(Modifier.height(2.dp))
                if (failed) {
                    Text(
                        "—",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CloudTextTertiary,
                    )
                } else {
                    Text(
                        value ?: "—",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = CloudTextPrimary,
                    )
                }
            }
            if (failed) {
                TextButton(
                    modifier = Modifier.testTag(retryTag),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    onClick = onRetry,
                ) { Text("重试", fontSize = 10.sp) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("后台任务", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CloudTextPrimary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("进行中 ", fontSize = 11.sp, color = CloudTextTertiary)
            Text(
                data.runningCount.toString(),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = CloudPrimary,
            )
        }
    }
    if (data.failedBucketIds.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text(
            "部分类型加载失败：${data.failedBucketIds.joinToString(", ") { taskLabel(it) }}",
            fontSize = 10.5.sp,
            color = CloudWarningText,
        )
    }
    Spacer(Modifier.height(10.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        data.buckets.forEach { bucket -> BucketChip(bucket) }
    }
}

@Composable
private fun BucketChip(bucket: TaskBucket) {
    Box(
        modifier = Modifier
            .clip(CloudShapes.Pill)
            .background(CloudPrimarySoft)
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(
            "${taskLabel(bucket.type)} ${bucket.running}",
            fontSize = 10.5.sp,
            color = CloudPrimary,
            fontWeight = FontWeight.Medium,
        )
    }
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
private fun StorageSection(storageSection: SectionResult<StorageData>, onStorageClick: (String) -> Unit, onRetry: () -> Unit) {
    when (storageSection) {
        is SectionResult.Ok -> StorageSectionOk(storages = storageSection.data.storages, onStorageClick = onStorageClick)
        is SectionResult.Failed -> CloudCard(modifier = Modifier.testTag("home_storage_failed"), contentPadding = PaddingValues(14.dp)) {
            SectionFailedHint(failure = storageSection.cause, onRetry = onRetry)
        }
        SectionResult.Loading -> CloudCard { Text("—", modifier = Modifier.padding(20.dp), color = CloudTextTertiary) }
    }
}

@Composable
private fun StorageSectionOk(storages: List<StorageInfo>, onStorageClick: (String) -> Unit) {
    val total = storages.size
    val working = storages.count { it.status == "work" }
    val abnormal = total - working
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "存储概览",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CloudTextPrimary,
            )
            Spacer(Modifier.size(8.dp))
            Text("共 $total 个", fontSize = 11.sp, color = CloudTextTertiary)
            Spacer(Modifier.weight(1f))
            if (working > 0) {
                InlineStat(count = working, label = "正常", color = CloudSuccessText)
            }
            if (abnormal > 0) {
                Spacer(Modifier.size(8.dp))
                InlineStat(count = abnormal, label = "异常", color = CloudErrorText)
            }
        }
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
private fun InlineStat(count: Int, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            count.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.size(3.dp))
        Text(label, fontSize = 11.sp, color = CloudTextSecondary)
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
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isFailed) CloudErrorContainer else CloudPrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFailed) Icons.Filled.Cloud else Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = if (isFailed) CloudErrorText else CloudPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    storage.mountPath,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CloudTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    driverLabel(storage),
                    fontSize = 11.sp,
                    color = CloudTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusBadge(storage.status)
        }
        if (isFailed && !storage.status.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                storage.status,
                fontSize = 11.sp,
                color = CloudErrorText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 44.dp),
            )
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
            .padding(horizontal = 9.dp, vertical = 3.dp),
    ) {
        Text(text, color = content, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun driverLabel(storage: StorageInfo): String = when (storage.driver.lowercase()) {
    "local" -> "本机存储 · Local"
    "aliyundrive" -> "阿里云盘 · Aliyundrive"
    "quark" -> "夸克网盘 · Quark"
    else -> storage.driver
}

private fun taskLabel(type: String): String = when (type) {
    "upload" -> "上传"
    "copy" -> "复制"
    "offline_download" -> "离线下载"
    "offline_download_transfer" -> "离线转存"
    "s3_transition" -> "对象存储迁移"
    "decompress" -> "解压"
    "decompress_upload" -> "解压上传"
    else -> type
}
