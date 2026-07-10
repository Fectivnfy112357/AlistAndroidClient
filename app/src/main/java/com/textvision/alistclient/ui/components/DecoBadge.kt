package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.CandyLemon
import com.textvision.alistclient.ui.theme.CandyLilac
import com.textvision.alistclient.ui.theme.CandyMint
import com.textvision.alistclient.ui.theme.CandyPink
import com.textvision.alistclient.ui.theme.InkMute

/** Corner/position of a DecoBadge relative to its parent Box. */
enum class DecoPosition(val alignment: Alignment) {
    TL(Alignment.TopStart),
    TR(Alignment.TopEnd),
    BR(Alignment.BottomEnd),
    CENTER(Alignment.Center),
}

/** Size preset for a DecoBadge. */
enum class DecoSize(val badge: Dp, val icon: Dp) {
    SM(24.dp, 12.dp),
    MD(30.dp, 15.dp),
    LG(38.dp, 19.dp),
}

/** Candy accent color for the DecoBadge icon. */
enum class DecoColor(val tint: Color) {
    PINK(CandyPink),
    MINT(CandyMint),
    LEMON(CandyLemon),
    LILAC(CandyLilac),
    BLUE(Brand500),
    ORANGE(Color(0xFFF4C77A)),
    MUTE(InkMute),
}

/**
 * 装饰徽章 — white 92% circular background + candy-colored icon.
 * Used as the bottom-right decoration on AlbumCard covers. Purely decorative.
 */
@Composable
fun DecoBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    position: DecoPosition = DecoPosition.BR,
    size: DecoSize = DecoSize.MD,
    color: DecoColor = DecoColor.PINK,
) {
    Box(
        modifier = modifier
            .size(size.badge)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.tint,
            modifier = Modifier.size(size.icon),
        )
    }
}

@Preview(name = "DecoBadge sizes")
@Composable
private fun DecoBadgePreview() {
    MaterialTheme {
        Box(Modifier.padding(16.dp)) {
            DecoBadge(AppIcons.decoStar, size = DecoSize.LG, color = DecoColor.LEMON)
        }
    }
}
