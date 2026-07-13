package com.textvision.alistclient.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.Ink

/**
 * Glass-white card — surface container with 0.78 alpha + 22dp corner + soft shadow.
 * Default matches the prototype `.card` (frosted translucent surface).
 *
 * Set [solid] = true to switch to opaque white with a hairline border — matches the
 * prototype `.card.solid` variant used for Hero / Storage / Task cards.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    solid: Boolean = false,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = if (solid) Color.White else MaterialTheme.colorScheme.surfaceContainer,
        border = if (solid) BorderStroke(1.dp, Ink.copy(alpha = 0.06f)) else null,
        tonalElevation = 0.dp,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(padding),
        ) {
            content()
        }
    }
}
