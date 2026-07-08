package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Deprecated(
    message = "使用 M3 Expressive 组件替代；Phase 4 删除",
    replaceWith = ReplaceWith("EmptyState(title, icon, message, actionLabel, onAction, modifier)"),
)
@Composable
fun CloudEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        message?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 6.dp),
                color = CloudTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        action?.let {
            Column(Modifier.padding(top = 16.dp)) { it() }
        }
    }
}

@Deprecated(
    message = "使用 M3 Expressive 组件替代；Phase 4 删除",
    replaceWith = ReplaceWith("EmptyState"),
)
@Composable
fun CloudLoadingState(text: String = "加载中") {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = text,
            modifier = Modifier.padding(top = 12.dp),
            color = CloudTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
