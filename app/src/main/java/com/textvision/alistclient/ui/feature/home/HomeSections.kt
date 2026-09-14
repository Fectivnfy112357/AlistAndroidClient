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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
import com.textvision.alistclient.ui.theme.Brand200
import com.textvision.alistclient.ui.theme.Brand300
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.NumeralStyle

// ═══════════════════════════════════════════════════════════════════════════
//  Status chip
// ═══════════════════════════════════════════════════════════════════════════

/** Status chip with dot — online (MINT) / offline (GRAY). */
@Composable
internal fun StatusChip(label: String, kind: ChipKind) = Chip(label, kind = kind, showDot = true)

// ═══════════════════════════════════════════════════════════════════════════
//  Section title — uppercase Fredoka · ink-soft · 0.04em tracking
//  Matches prototype `.card-title` and `.section-head .title` styles.
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun HomeSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.08.em),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Hero server card — prototype `.card.solid` with cloud icon + name + chip
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun HeroServerCard(public: SectionResult<PublicData>, online: Boolean) {
    val data = (public as? SectionResult.Ok)?.data
    val serverName = data?.siteTitle ?: "Alist"
    val version = data?.siteVersion
    SectionCard(
        modifier = Modifier.testTag("home_hero"),
        padding = PaddingValues(14.dp),
        solid = true,
    ) {
        // P0 (perf #4+#30): wrap the highlight circle in `drawWithCache` so
        // the `Brush.radialGradient(...)` is allocated once per size / theme
        // change instead of once per draw. The hero card is the top of Home
        // and redraws whenever the user scrolls, so this is a hot path.
        Box(
            modifier = Modifier.drawWithCache {
                val d = 90.dp.toPx()
                val ox = 22.dp.toPx()
                val oy = -22.dp.toPx()
                val cx = size.width + ox - d / 2f
                val cy = oy + d / 2f
                val center = androidx.compose.ui.geometry.Offset(cx, cy)
                val brush = Brush.radialGradient(
                    listOf(Brand300.copy(alpha = 1f), Brand200.copy(alpha = 0.5f)),
                    center = center,
                    radius = d / 2f,
                )
                onDrawBehind {
                    drawCircle(
                        brush = brush,
                        radius = d / 2f,
                        center = center,
                    )
                }
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Gradient cloud icon square (prototype `#6FB6FF → #4A98E8`).
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(Brush.linearGradient(listOf(Brand500, Brand600))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.cloud,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = serverName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = if (version != null) "当前服务器 · $version" else "当前服务器",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(6.dp))
                StatusChip(
                    label = if (online) "在线" else "离线",
                    kind = if (online) ChipKind.MINT else ChipKind.GRAY,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Metric row (3 cards) — prototype `.metric` with Fredoka 26sp numerals
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
        MetricAccent.BRAND -> Brand200 to Brand600
        MetricAccent.MINT  -> Color(0xFFDAF6EC) to Color(0xFF2D9B7C)
        MetricAccent.PINK  -> Color(0xFFFFE4ED) to Color(0xFFC46683)
    }
    SectionCard(
        modifier = modifier.testTag("home_metric_$label"),
        padding = PaddingValues(12.dp),
        solid = true,
    ) {
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
            HomeSectionTitle(label)
            if (failed) {
                TextButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.testTag(retryTag),
                ) { Text("重试", style = MaterialTheme.typography.labelSmall) }
            } else {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value ?: "—",
                    style = NumeralStyle.value,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Background-task card — prototype: section title outside + solid card
//  with "进行中" row + Fredoka count + dot-prefix chips
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun TaskSection(task: SectionResult<TaskData>, onRetry: () -> Unit) {
    SectionCard(
        modifier = Modifier.testTag("home_task_card"),
        padding = PaddingValues(12.dp),
        solid = true,
    ) {
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
                text = "进行中",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = data.runningCount.toString(),
                style = NumeralStyle.value.copy(fontSize = 18.sp, lineHeight = 22.sp),
                color = MaterialTheme.colorScheme.primary,
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
                        else -> ChipKind.GRAY to (bucket.running > 0)
                    }
                    val label = if (bucket.running > 0)
                        "${taskLabel(bucket.type)} · ${bucket.running}"
                    else
                        taskLabel(bucket.type)
                    Chip(label, kind = kind, showDot = dot)
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