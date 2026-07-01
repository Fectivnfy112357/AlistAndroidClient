package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.textvision.alistclient.ui.theme.CloudErrorContainer
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSurface
import com.textvision.alistclient.ui.theme.CloudSurfaceMuted
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary
import com.textvision.alistclient.ui.theme.cloudClickable

/**
 * 统一风格的确认弹框，替代原始 Material3 [androidx.compose.material3.AlertDialog]，
 * 与 Cloud 设计系统的圆角、配色、按钮保持一致。
 */
@Composable
fun CloudAlertDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "确定",
    dismissText: String = "取消",
    destructive: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CloudShapes.Card)
                .background(CloudSurface)
                .padding(22.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = CloudTextPrimary,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = CloudTextSecondary,
            )
            Spacer(Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DialogButton(
                    text = dismissText,
                    modifier = Modifier.weight(1f),
                    containerColor = CloudSurfaceMuted,
                    contentColor = CloudTextSecondary,
                    onClick = onDismiss,
                )
                DialogButton(
                    text = confirmText,
                    modifier = Modifier.weight(1f),
                    containerColor = if (destructive) CloudErrorContainer else CloudPrimary,
                    contentColor = if (destructive) CloudErrorText else Color.White,
                    onClick = onConfirm,
                )
            }
        }
    }
}

@Composable
private fun DialogButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .clip(CloudShapes.Control)
            .background(containerColor)
            .cloudClickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
    }
}
