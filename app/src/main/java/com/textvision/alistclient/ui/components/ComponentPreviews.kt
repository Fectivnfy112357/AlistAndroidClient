package com.textvision.alistclient.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.foundation.AppBottomBar
import com.textvision.alistclient.ui.foundation.AppScaffold
import com.textvision.alistclient.ui.foundation.AppTopBar
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.DarkMode

@Preview(name = "AppScaffold", showBackground = true)
@Composable
private fun AppScaffoldPreview() {
    AlistTheme() {
        AppScaffold(
            topBar = { AppTopBar(title = "标题") },
        ) { padding ->
            Text("Content", modifier = Modifier.padding(padding).padding(16.dp))
        }
    }
}

@Preview(
    name = "AppScaffold Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AppScaffoldDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        AppScaffold(
            topBar = { AppTopBar(title = "标题", subtitle = "暗色模式") },
        ) { padding ->
            Text("Dark Content", modifier = Modifier.padding(padding).padding(16.dp))
        }
    }
}

@Preview(name = "AppTopBar", showBackground = true)
@Composable
private fun AppTopBarPreview() {
    AlistTheme() {
        AppTopBar(title = "文件", subtitle = "/root/documents", onBack = {})
    }
}

@Preview(name = "AppBottomBar", showBackground = true)
@Composable
private fun AppBottomBarPreview() {
    AlistTheme() {
        AppBottomBar(currentRoute = "files", onNavigate = {})
    }
}

@Preview(name = "StatusBanner (all kinds)", showBackground = true, widthDp = 360)
@Composable
private fun StatusBannerPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp),
            ) {
                StatusBanner(kind = BannerKind.INFO, message = "Info 提示")
                StatusBanner(kind = BannerKind.WARNING, message = "Warning 警告")
                StatusBanner(kind = BannerKind.ERROR, message = "Error 错误", actionLabel = "重试", onAction = {})
                StatusBanner(kind = BannerKind.SUCCESS, message = "Success 成功")
            }
        }
    }
}

@Preview(name = "AppAlertDialog", showBackground = true)
@Composable
private fun AppAlertDialogPreview() {
    AlistTheme() {
        AppAlertDialog(
            title = "删除文件",
            message = "确定要删除该文件吗？此操作无法撤销。",
            confirmLabel = "删除",
            onConfirm = {},
            dismissLabel = "取消",
            onDismiss = {},
            destructive = true,
        )
    }
}

@Preview(name = "EmptyState", showBackground = true, heightDp = 240)
@Composable
private fun EmptyStatePreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            EmptyState(
                title = "空空如也",
                icon = Icons.Outlined.Folder,
                message = "此目录暂无文件",
            )
        }
    }
}

@Preview(name = "ErrorState", showBackground = true, heightDp = 240)
@Composable
private fun ErrorStatePreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            ErrorState(message = "加载失败，请检查网络", onRetry = {})
        }
    }
}

@Preview(name = "LoadingState", showBackground = true, heightDp = 240)
@Composable
private fun LoadingStatePreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            LoadingState(message = "加载中")
        }
    }
}

@Preview(name = "ListItemRow", showBackground = true, widthDp = 360)
@Composable
private fun ListItemRowPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                ListItemRow(
                    leading = { FileTypeIcon(FileCategory.FOLDER) },
                    title = "Documents",
                    subtitle = "12 项",
                    onClick = {},
                )
                ListItemRow(
                    leading = { FileTypeIcon(FileCategory.PDF) },
                    title = "report.pdf",
                    subtitle = "2.3 MB",
                    onClick = {},
                )
            }
        }
    }
}

@Preview(name = "ActionButton (variants)", showBackground = true, widthDp = 360)
@Composable
private fun ActionButtonPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(text = "Filled", onClick = {}, variant = ButtonVariant.FILLED)
                    ActionButton(text = "Tonal", onClick = {}, variant = ButtonVariant.TONAL)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(text = "Outlined", onClick = {}, variant = ButtonVariant.OUTLINED)
                    ActionButton(text = "Text", onClick = {}, variant = ButtonVariant.TEXT)
                }
                ActionButton(text = "Loading", onClick = {}, isLoading = true)
            }
        }
    }
}

@Preview(name = "SearchField", showBackground = true, widthDp = 360)
@Composable
private fun SearchFieldPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            SearchField(query = "", onQueryChange = {}, placeholder = "搜索文件", modifier = Modifier.padding(12.dp))
        }
    }
}

@Preview(name = "Breadcrumb", showBackground = true, widthDp = 360)
@Composable
private fun BreadcrumbPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Breadcrumb(path = "/root/documents/work", onNavigate = {})
        }
    }
}

@Preview(name = "FileTypeIcon Grid", showBackground = true, widthDp = 360)
@Composable
private fun FileTypeIconGridPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileTypeIcon(FileCategory.FOLDER)
                    FileTypeIcon(FileCategory.IMAGE)
                    FileTypeIcon(FileCategory.VIDEO)
                    FileTypeIcon(FileCategory.AUDIO)
                    FileTypeIcon(FileCategory.PDF)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileTypeIcon(FileCategory.TEXT)
                    FileTypeIcon(FileCategory.CODE)
                    FileTypeIcon(FileCategory.ARCHIVE)
                    FileTypeIcon(FileCategory.DOCUMENT)
                    FileTypeIcon(FileCategory.OTHER)
                }
            }
        }
    }
}
