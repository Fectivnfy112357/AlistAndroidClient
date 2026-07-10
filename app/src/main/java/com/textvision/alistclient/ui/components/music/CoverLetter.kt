package com.textvision.alistclient.ui.components.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 居中首字封面 — gradient background + first Unicode character of `name` rendered large.
 */
@Composable
fun CoverLetter(
    name: String,
    gradient: Brush,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier,
) {
    val firstChar = name.firstOrNull { !it.isWhitespace() }?.toString()?.uppercase() ?: "♪"
    val fontSize = (size.value * 0.52f).sp
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = firstChar,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            style = MaterialTheme.typography.displayMedium,
        )
    }
}
