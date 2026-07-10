package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.CandyLilac
import com.textvision.alistclient.ui.theme.CandyLilacBg

// ═══════════════════════════════════════════════════════════════════════════
//  Chip primitives (prototype pill chips) — shared across home / music.
// ═══════════════════════════════════════════════════════════════════════════

internal enum class ChipKind { PRIMARY, MINT, GRAY, LILAC }

@Composable
private fun chipColors(kind: ChipKind): Triple<Color, Color, Color> = when (kind) {
    ChipKind.PRIMARY -> Triple(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.primary,
    )
    ChipKind.MINT -> Triple(
        MaterialTheme.colorScheme.secondaryContainer,
        MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.secondary,
    )
    ChipKind.GRAY -> Triple(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.onSurfaceVariant,
    )
    ChipKind.LILAC -> Triple(
        CandyLilacBg,
        MaterialTheme.colorScheme.onSurface,
        CandyLilac,
    )
}

/** Small pill chip — optional leading status dot. */
@Composable
internal fun Chip(
    label: String,
    modifier: Modifier = Modifier,
    kind: ChipKind = ChipKind.GRAY,
    showDot: Boolean = false,
) {
    val (bg, fg, dot) = chipColors(kind)
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
            Spacer(Modifier.width(5.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Medium,
        )
    }
}
