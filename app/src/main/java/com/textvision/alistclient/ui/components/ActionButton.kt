package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class ButtonVariant { FILLED, TONAL, OUTLINED, TEXT }

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val effectiveEnabled = enabled && !loading
    val content: @Composable RowScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(text)
        }
    }

    when (variant) {
        ButtonVariant.FILLED -> Button(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            enabled = effectiveEnabled,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            content = content,
        )
        ButtonVariant.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            enabled = effectiveEnabled,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            content = content,
        )
        ButtonVariant.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            enabled = effectiveEnabled,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            content = content,
        )
        ButtonVariant.TEXT -> TextButton(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            enabled = effectiveEnabled,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            content = content,
        )
    }
}
