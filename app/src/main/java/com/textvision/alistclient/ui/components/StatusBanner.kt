package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.StateError
import com.textvision.alistclient.ui.theme.StateErrorBg
import com.textvision.alistclient.ui.theme.StateSuccessBg
import com.textvision.alistclient.ui.theme.StateSuccessFg
import com.textvision.alistclient.ui.theme.StateWarnBg
import com.textvision.alistclient.ui.theme.StateWarnFg

enum class BannerKind { INFO, WARNING, ERROR, SUCCESS }

@Composable
fun StatusBanner(
    kind: BannerKind,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, icon) = when (kind) {
        BannerKind.INFO    -> Triple(Brand500.copy(alpha = 0.10f), Brand500, AppIcons.alert)
        BannerKind.WARNING -> Triple(StateWarnBg, StateWarnFg, AppIcons.alert)
        BannerKind.ERROR   -> Triple(StateErrorBg, StateError, AppIcons.alert)
        BannerKind.SUCCESS -> Triple(StateSuccessBg, StateSuccessFg, AppIcons.check)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            ActionButton(
                text = actionLabel,
                onClick = onAction,
                variant = ButtonVariant.TEXT,
            )
        }
    }
}
