package com.textvision.alistclient.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.CandyMint

/**
 * 首页专属问候顶栏（替代通用 AppTopBar，对齐 prototype §9.2）：
 *   - "早上好 ✨" 18sp Fredoka SemiBold
 *   - 副标题 11sp + 6dp mint 在线圆点（在线时），离线用 warn 黄点
 *   - 右侧两个 32dp 圆形 icon-btn：白底刷新 + 蓝色渐变搜索
 */
@Composable
internal fun HomeGreeting(
    serverTitle: String,
    isOnline: Boolean,
    onRefresh: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "早上好 ✨",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OnlineDot(isOnline = isOnline)
                Spacer(Modifier.size(6.dp))
                Text(
                    text = if (isOnline) "已连接 · $serverTitle" else "离线 · $serverTitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f))
                .testTag("home_topbar_refresh"),
        ) {
            Icon(
                imageVector = AppIcons.refresh,
                contentDescription = "刷新",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.size(6.dp))
        IconButton(
            onClick = onSearch,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary)
                .testTag("home_topbar_search"),
        ) {
            Icon(
                imageVector = AppIcons.search,
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun OnlineDot(isOnline: Boolean) {
    val color = if (isOnline) CandyMint else MaterialTheme.colorScheme.tertiary
    Box(
        modifier = Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(color),
    )
}