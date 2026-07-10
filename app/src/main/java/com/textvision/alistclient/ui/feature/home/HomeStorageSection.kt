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

/** Section title row: "存储源" + "管理 →" link. */
@Composable
internal fun StorageHeader(onManage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "存储源",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "管理 →",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onManage),
        )
    }
}

@Composable
internal fun StorageEmptyOrFailed(
    storageSection: SectionResult<StorageData>,
    onRetry: () -> Unit,
) {
    when (storageSection) {
        is SectionResult.Failed -> SectionCard(modifier = Modifier.testTag("home_storage_failed")) {
            SectionFailedHint(storageSection.cause, onRetry)
        }
        SectionResult.Loading -> SectionCard {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        is SectionResult.Ok -> if (storageSection.data.storages.isEmpty()) {
            SectionCard { Text("暂无存储", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** Single storage entry — gradient icon square + name + path + status chip / chevron. */
@Composable
internal fun StorageCard(storage: StorageInfo, onClick: () -> Unit) {
    val disabled = storage.status != "work"
    SectionCard(
        modifier = Modifier
            .testTag("home_storage_card_${storage.mountPath}")
            .clickable(onClick = onClick),
        padding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(Brush.linearGradient(listOf(Brand500, Brand600))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    AppIcons.database,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = storage.mountPath,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = driverLabel(storage),
                    style = MaterialTheme.typography.labelSmall,
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

private fun driverLabel(storage: StorageInfo): String = when (storage.driver.lowercase()) {
    "local" -> "本机存储 · Local"
    "aliyundrive" -> "阿里云盘 · Aliyundrive"
    "quark" -> "夸克网盘 · Quark"
    "baidunetdisk", "baidu" -> "百度网盘 · Baidu"
    else -> storage.driver.ifBlank { storage.mountPath }
}
