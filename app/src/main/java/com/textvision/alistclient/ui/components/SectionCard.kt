package com.textvision.alistclient.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.Ink

/**
 * Glass-white card — surface container with 0.78 alpha + 22dp corner + soft shadow.
 * Default matches the prototype `.card` (frosted translucent surface).
 *
 * Set [solid] = true to switch to opaque white with a hairline border — matches the
 * prototype `.card.solid` variant used for Hero / Storage / Task cards.
 *
 * Implementation note (perf #1, 2026-09-16):
 *   Earlier this used `material3.Surface(...)` with `tonalElevation = 0.dp` and
 *   `shadowElevation = shadowElevation`. On Compose UI 1.7.x (BOM 2024.09.03)
 *   `Surface` always wraps the contents in
 *     - `CompositionLocalProvider(LocalContentColor, LocalAbsoluteTonalElevation)`
 *     - `Box(modifier = Modifier.surface(...).semantics{isContainer=true}.pointerInput(Unit){})`
 *   For a 6-card dashboard this runs six times per frame. With shadowElevation = 0.dp
 *   `Modifier.surface(...)` short-circuits to bare `Modifier` (verified against the
 *   Compose UI 1.7.3 source: `androidx.compose.ui:ui:1.7.3` `Material3.Surface.surface()`),
 *   but the LocalAbsoluteTonalElevation `CompositionLocalProvider` and the
 *   `Box(Modifier.semantics{...}.pointerInput(Unit){})` overhead still run per card.
 *   None of the dashboard call sites consume `LocalAbsoluteTonalElevation` /
 *   `LocalContentColor` overrides — `HeroServerCard`, `MetricCard`, `TaskSection`,
 *   `StorageCard` read `MaterialTheme.colorScheme.*` directly inside the
 *   `Surface` body — so the wrappers are pure overhead.
 *   Replaced with `Box(Modifier.background().clip().padding())` and the optional
 *   border; visual layout is unchanged (same shape, same background alpha, same
 *   optional hairline). Shadow stays available via `shadowElevation > 0.dp` for
 *   non-dashboard call sites.
 */
private val CardShape = RoundedCornerShape(22.dp)

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    solid: Boolean = false,
    shadowElevation: Dp = 4.dp,
    content: @Composable () -> Unit,
) {
    val shape = CardShape
    val base = if (shadowElevation > 0.dp) {
        modifier.shadow(elevation = shadowElevation, shape = shape)
    } else {
        modifier
    }
    Box(
        modifier = base
            .clip(shape)
            .background(if (solid) Color.White else Color.White.copy(alpha = 0.78f))
            .then(
                if (solid) Modifier.border(BorderStroke(1.dp, Ink.copy(alpha = 0.06f)), shape)
                else Modifier
            )
            .padding(padding),
    ) {
        content()
    }
}


