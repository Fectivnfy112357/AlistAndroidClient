@file:OptIn(ExperimentalLayoutApi::class)

package com.textvision.alistclient.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.components.BannerKind
import com.textvision.alistclient.ui.components.Chip
import com.textvision.alistclient.ui.components.ChipKind
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.components.StatusBanner
import com.textvision.alistclient.ui.feature.home.dto.PublicData
import com.textvision.alistclient.ui.feature.home.dto.SectionFailure
import com.textvision.alistclient.ui.feature.home.dto.SectionResult
import com.textvision.alistclient.ui.feature.home.dto.ServerStatsData
import com.textvision.alistclient.ui.feature.home.dto.SessionData
import com.textvision.alistclient.ui.feature.home.dto.TaskBucket
import com.textvision.alistclient.ui.feature.home.dto.TaskData
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand300
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600

// ═══════════════════════════════════════════════════════════════════════════
//  Status chip
// ═══════════════════════════════════════════════════════════════════════════

/** Status chip with dot — online (MINT) / offline (GRAY). */
@Composable
internal fun StatusChip(label: String, kind: ChipKind) = Chip(label, kind = kind, showDot = true)

// ═══════════════════════════════════════════════════════════════════════════
//  Hero server card
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun HeroServerCard(public: SectionResult<PublicData>, online: Boolean) {
    val data = (public as? SectionResult.Ok)?.data
    val serverName = data?.siteTitle ?: "Alist"
    val version = data?.siteVersion
    SectionCard(padding = PaddingValues(18.dp)) {
        Box {
            Box(
                Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = (-30).dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Brand300.copy(alpha = 0.6f), Color.Transparent),
                        ),
                    ),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Brush.linearGradient(listOf(Brand500, Brand600))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.cloud,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = serverName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (version != null) "当前服务器 · $version" else "当前服务器",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusChip(
                    label = if (online) "在线" else "离线",
                    kind = if (online) ChipKind.MINT else ChipKind.GRAY,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Metric row (3 cards)
// ═══════════════════════════════════════════════════════════════════════════

internal enum class MetricAccent { BRAND, MINT, PINK }

@Composable
internal fun MetricRow(
    serverStats: SectionResult<ServerStatsData>,
    session: SectionResult<SessionData>,
    onRetryServerStats: () -> Unit,
    onRetrySession: () -> Unit,
) {
    val stats = (serverStats as? SectionResult.Ok)?.data
    val sess = (session as? SectionResult.Ok)?.data
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricCard(
            label = "用户",
            value = stats?.userCount?.toString(),
            icon = AppIcons.user,
            accent = MetricAccent.BRAND,
            failed = serverStats is SectionResult.Failed,
            onRetry = onRetryServerStats,
            retryTag = "home_serverstats_retry",
            modifier = Modifier.weight(1f).testTag("home_kpi_users"),
        )
        MetricCard(
            label = "角色",
            value = stats?.roleCount?.toString(),
            icon = AppIcons.shield,
            accent = MetricAccent.MINT,
            failed = serverStats is SectionResult.Failed,
            onRetry = onRetryServerStats,
            retryTag = "home_serverstats_role_retry",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = "在线",
            value = sess?.activeCount?.toString(),
            icon = AppIcons.sparkle,
            accent = MetricAccent.PINK,
            failed = session is SectionResult.Failed,
            onRetry = onRetrySession,
            retryTag = "home_session_retry",
            modifier = Modifier.weight(1f).testTag("home_kpi_session"),
        )
    }
}

@Composable
internal fun MetricCard(
    label: String,
    value: String?,
    icon: ImageVector,
    accent: MetricAccent,
    modifier: Modifier = Modifier,
    failed: Boolean = false,
    onRetry: () -> Unit = {},
    retryTag: String = "",
) {
    val (bg, fg) = when (accent) {
        MetricAccent.BRAND -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        MetricAccent.MINT -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        MetricAccent.PINK -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
    }
    SectionCard(modifier = modifier, padding = PaddingValues(14.dp)) {
        Column {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(bg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (failed) {
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.testTag(retryTag),
                ) { Text("重试", style = MaterialTheme.typography.labelSmall) }
            } else {
                Text(
                    text = value ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Background-task card (5 chips)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun TaskSection(task: SectionResult<TaskData>, onRetry: () -> Unit) {
    SectionCard(modifier = Modifier.testTag("home_task_card"), padding = PaddingValues(14.dp)) {
        when (task) {
            is SectionResult.Ok -> TaskContent(task.data)
            is SectionResult.Failed -> SectionFailedHint(task.cause, onRetry)
            SectionResult.Loading -> Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TaskContent(data: TaskData) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "后台任务",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = data.runningCount.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
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
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                data.buckets.take(5).forEachIndexed { i, bucket ->
                    val (kind, dot) = when (i) {
                        0 -> ChipKind.PRIMARY to true
                        1 -> ChipKind.MINT to true
                        else -> ChipKind.GRAY to false
                    }
                    Chip("${taskLabel(bucket.type)} ${bucket.running}", kind = kind, showDot = dot)
                }
            }
        }
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
