package com.textvision.alistclient.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.AppMotion
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600

enum class ButtonVariant { FILLED, TONAL, OUTLINED, TEXT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = AppMotion.SpringFast,
        label = "button-scale",
    )

    val height = when (variant) {
        ButtonVariant.TEXT -> 40.dp
        else -> 48.dp
    }

    val onClickWrapper = {
        pressed = true
        onClick()
    }

    when (variant) {
        ButtonVariant.FILLED -> {
            Surface(
                modifier = modifier
                    .scale(scale)
                    .height(height),
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                onClick = onClickWrapper,
                enabled = enabled,
            ) {
                Box(
                    Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Brand500, Brand600),
                            ),
                        )
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        } else if (leadingIcon != null) {
                            Icon(
                                leadingIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
        ButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = onClickWrapper,
                enabled = enabled,
                modifier = modifier
                    .scale(scale)
                    .height(height),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                } else if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
        ButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClickWrapper,
                enabled = enabled,
                modifier = modifier
                    .scale(scale)
                    .height(height),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
        ButtonVariant.TEXT -> {
            TextButton(
                onClick = onClickWrapper,
                enabled = enabled,
                modifier = modifier
                    .scale(scale)
                    .height(height),
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(text, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Preview(name = "Light ActionButton")
@Composable
private fun ActionButtonPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ActionButton("登录 Alist", onClick = {}, variant = ButtonVariant.FILLED)
            Spacer(Modifier.height(8.dp))
            ActionButton("取消", onClick = {}, variant = ButtonVariant.TONAL)
            Spacer(Modifier.height(8.dp))
            ActionButton("更多选项", onClick = {}, variant = ButtonVariant.OUTLINED)
            Spacer(Modifier.height(8.dp))
            ActionButton("跳过", onClick = {}, variant = ButtonVariant.TEXT)
        }
    }
}