package com.textvision.alistclient.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.components.Chip
import com.textvision.alistclient.ui.components.ChipKind
import com.textvision.alistclient.ui.components.SectionCard
import com.textvision.alistclient.ui.feature.home.dto.SectionResult
import com.textvision.alistclient.ui.feature.home.dto.StorageData
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600

/** Section title row: 4dp vertical accent bar + "存储源" + "管理 →" link. */
@Composable
internal fun StorageHeader(onManage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            // Prototype `.section-head .title .accent` 4dp gradient bar.
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 14.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Brush.verticalGradient(listOf(Brand500, Color(0xFF9BE3C8)))),
            )
            Spacer(Modifier.width(6.dp))
            HomeSectionTitle("存储源")
        }
        Text(
            text = "管理 →",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onManage)
                .testTag("home_storage_manage"),
        )
    }
}

@Composable
internal fun StorageEmptyOrFailed(
    storageSection: SectionResult<StorageData>,
    onRetry: () -> Unit,
) {
    when (storageSection) {
        is SectionResult.Failed -> SectionCard(
            modifier = Modifier.testTag("home_storage_failed"),
            solid = true,
        ) { SectionFailedHint(storageSection.cause, onRetry) }
        SectionResult.Loading -> SectionCard(solid = true) {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is SectionResult.Ok -> if (storageSection.data.storages.isEmpty()) {
            SectionCard(solid = true) { Text("暂无存储", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/**
 * Single storage entry — gradient icon square + name + path + status chip / chevron.
 * Gradient palette matches prototype `.storage-card .ico` rules per driver family.
 */
@Composable
internal fun StorageCard(storage: StorageInfo, onClick: () -> Unit) {
    val disabled = storage.status != "work"
    val (gradient, tint) = storageGradient(storage.driver)
    SectionCard(
        modifier = Modifier
            .testTag("home_storage_card_${storage.mountPath}")
            .clickable(onClick = onClick),
        padding = PaddingValues(12.dp),
        solid = true,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = storageIcon(storage.driver),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = driverDisplayName(storage.driver),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = storageSubtitle(storage),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (disabled) {
                Chip("已禁用", kind = ChipKind.GRAY)
            } else {
                Box(
                    Modifier.size(28.dp).clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        AppIcons.chevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** Prototype palette: blue / mint / lilac / pink per driver family. */
private fun storageGradient(driver: String): Pair<List<Color>, Color> = when (driver.lowercase()) {
    "local" -> listOf(Color(0xFFFFE4ED), Color(0xFFFFD1DD)) to Color(0xFFC46683)
    "baidunetdisk", "baidu" -> listOf(Color(0xFFECE2FF), Color(0xFFDDD0FF)) to Color(0xFF7C5BC7)
    "quark" -> listOf(Color(0xFFDAF6EC), Color(0xFFC2EFE0)) to Color(0xFF2D9B7C)
    else -> listOf(Color(0xFFBFE0FF), Color(0xFFD7E9FF)) to Brand600
}

private fun storageIcon(driver: String) = when (driver.lowercase()) {
    "local" -> AppIcons.archive
    "baidunetdisk", "baidu" -> AppIcons.database
    "quark" -> AppIcons.cloud
    else -> AppIcons.database
}

private fun driverDisplayName(driver: String): String = when (driver.lowercase()) {
    "local" -> "本地存储"
    "aliyundrive" -> "阿里云盘"
    "quark" -> "夸克网盘"
    "baidunetdisk", "baidu" -> "百度网盘"
    else -> driver.ifBlank { "未知存储" }
}

private fun storageSubtitle(storage: StorageInfo): String {
    val status = when (storage.status) {
        "work" -> "已挂载"
        "disabled" -> "已禁用"
        else -> storage.status
    }
    return "${storage.mountPath} · $status"
}