package com.textvision.alistclient.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.components.ListItemRow
import com.textvision.alistclient.ui.feature.home.dto.SectionResult
import com.textvision.alistclient.ui.feature.home.dto.StorageData

@Composable
internal fun StorageSection(
    storageSection: SectionResult<StorageData>,
    onStorageClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when (storageSection) {
        is SectionResult.Ok -> StorageSectionOk(storageSection.data.storages, onStorageClick)
        is SectionResult.Failed -> SectionCard(modifier = Modifier.testTag("home_storage_failed")) {
            SectionFailedHint(storageSection.cause, onRetry)
        }
        SectionResult.Loading -> SectionCard {
            Text("—", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                text = "存储概览",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = "共 $total 个",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (working > 0) {
                Text(
                    text = "正常 $working",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (abnormal > 0) {
                Spacer(Modifier.size(10.dp))
                Text(
                    text = "异常 $abnormal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (storages.isEmpty()) {
            SectionCard { Text("暂无存储", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 1.dp,
            ) {
                Column {
                    storages.forEach { storage ->
                        StorageRow(storage) { onStorageClick(storage.mountPath) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageRow(storage: StorageInfo, onClick: () -> Unit) {
    val isFailed = storage.status != "work"
    ListItemRow(
        modifier = Modifier.testTag("home_storage_card_${storage.mountPath}"),
        leading = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isFailed) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFailed) Icons.Filled.CloudOff else Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = if (isFailed) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        title = storage.mountPath,
        subtitle = driverLabel(storage),
        trailing = { StatusBadge(storage.status) },
        onClick = onClick,
    )
}

@Composable
private fun StatusBadge(status: String?) {
    val (container, content, text) = when {
        status.isNullOrBlank() -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "未知",
        )
        status == "work" -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "正常",
        )
        else -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "异常",
        )
    }
    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

private fun driverLabel(storage: StorageInfo): String = when (storage.driver.lowercase()) {
    "local" -> "本机存储 · Local"
    "aliyundrive" -> "阿里云盘 · Aliyundrive"
    "quark" -> "夸克网盘 · Quark"
    else -> storage.driver
}
