package com.textvision.alistclient.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.textvision.alistclient.ui.theme.AlistTheme
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyMintBg
import com.textvision.alistclient.ui.theme.DarkMode
import com.textvision.alistclient.ui.theme.InkSoft
import com.textvision.alistclient.ui.theme.StateSuccessFg

/**
 * User identity card — 48dp round gradient avatar (first letter) + name (Fredoka titleMedium)
 * + role · server subtitle, with an optional mint VIP chip on the right.
 *
 * Per prototype §4.1: shared component for the top-of-settings card.
 * Variant of [com.textvision.alistclient.ui.components.music.CoverLetter] rendered as a circle
 * rather than a square, and composes title / subtitle / optional chip around it.
 */
@Composable
fun UserAvatarCard(
    name: String,
    role: String,
    serverName: String,
    status: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserAvatar(letter = name, size = 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = "$role · $serverName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (status != null) {
            StatusChip(label = status)
        }
    }
}

/** 48dp round gradient avatar with first uppercase letter of [letter]. */
@Composable
fun UserAvatar(
    letter: String,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.linearGradient(listOf(Brand500, Brand600)),
) {
    val firstChar = letter.firstOrNull { !it.isWhitespace() }?.toString()?.uppercase() ?: "?"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = firstChar,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.45f).sp,
        )
    }
}

@Composable
private fun StatusChip(label: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = CandyMintBg,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = StateSuccessFg,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "UserAvatarCard Light", showBackground = true, widthDp = 360)
@Composable
private fun UserAvatarCardLightPreview() {
    AlistTheme(darkMode = DarkMode.LIGHT) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UserAvatarCard(
                name = "晓源",
                role = "VIP",
                serverName = "我的云端小屋 · 在线",
                status = "VIP",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(
    name = "UserAvatarCard Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun UserAvatarCardDarkPreview() {
    AlistTheme(darkMode = DarkMode.DARK) {
        Surface(color = MaterialTheme.colorScheme.background) {
            UserAvatarCard(
                name = "Admin",
                role = "VIP",
                serverName = "我的云端小屋 · 在线",
                status = "VIP",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Preview(name = "UserAvatarCard No Status", showBackground = true, widthDp = 360)
@Composable
private fun UserAvatarCardNoStatusPreview() {
    AlistTheme() {
        Surface(color = MaterialTheme.colorScheme.background) {
            UserAvatarCard(
                name = "访客",
                role = "普通用户",
                serverName = "alist.local",
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
