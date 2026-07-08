@file:OptIn(ExperimentalLayoutApi::class)

package com.textvision.alistclient.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.home.dto.PublicData
import com.textvision.alistclient.home.dto.SectionFailure
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.home.dto.ServerStatsData
import com.textvision.alistclient.home.dto.SessionData
import com.textvision.alistclient.home.dto.TaskBucket
import com.textvision.alistclient.home.dto.TaskData
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.StatusBanner

/** Card surface shared across dashboard sections. */
@Composable
internal fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
    ) {
        Box(modifier = Modifier.padding(16.dp)) { content() }
    }
}

@Composable
internal fun HeroCard(public: SectionResult<PublicData>) {
    val title = (public as? SectionResult.Ok)?.data?.siteTitle ?: "Alist"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = title.firstOrNull()?.uppercase() ?: "A",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前服务器",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun KpiRow(
    serverStats: SectionResult<ServerStatsData>,
    session: SectionResult<SessionData>,
    onRetryServerStats: () -> Unit,
    onRetrySession: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
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
    SectionCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (failed) "—" else value ?: "—",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (failed) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (failed) {
                TextButton(
                    modifier = Modifier.testTag(retryTag),
                    onClick = onRetry,
                ) { Text("重试", style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

@Composable
internal fun TaskSection(task: SectionResult<TaskData>, onRetry: () -> Unit) {
    SectionCard(modifier = Modifier.testTag("home_task_card")) {
        when (task) {
            is SectionResult.Ok -> TaskContent(task.data)
            is SectionResult.Failed -> SectionFailedHint(task.cause, onRetry)
            SectionResult.Loading -> Text(
                text = "—",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TaskContent(data: TaskData) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "后台任务",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "进行中 ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = data.runningCount.toString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (data.failedBucketIds.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "部分类型加载失败：${data.failedBucketIds.joinToString(", ") { taskLabel(it) }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
        if (data.buckets.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                data.buckets.forEach { BucketChip(it) }
            }
        }
    }
}

@Composable
private fun BucketChip(bucket: TaskBucket) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = "${taskLabel(bucket.type)} ${bucket.running}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun SectionFailedHint(failure: SectionFailure, onRetry: () -> Unit) {
    val text = when (failure) {
        SectionFailure.Network -> "数据加载失败"
        SectionFailure.Unauthorized -> "需要管理员权限"
        is SectionFailure.Server -> "服务器返回 ${failure.code}"
    }
    StatusBanner(
        kind = BannerKind.WARNING,
        message = text,
        actionLabel = "重试",
        onAction = onRetry,
    )
}

internal fun taskLabel(type: String): String = when (type) {
    "upload" -> "上传"
    "copy" -> "复制"
    "offline_download" -> "离线下载"
    "offline_download_transfer" -> "离线转存"
    "s3_transition" -> "对象存储迁移"
    "decompress" -> "解压"
    "decompress_upload" -> "解压上传"
    else -> type
}
