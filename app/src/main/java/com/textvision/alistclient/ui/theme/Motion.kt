package com.textvision.alistclient.ui.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset

internal object CloudMotion {
    const val DurationShortMillis = 120
    const val DurationMediumMillis = 240
    const val DurationLongMillis = 300

    const val PressedScale = 0.94f
    const val RestScale = 1f

    val FloatTween: FiniteAnimationSpec<Float> = tween(
        durationMillis = DurationMediumMillis,
        easing = LinearEasing,
    )

    val OffsetTween: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = DurationMediumMillis,
        easing = LinearEasing,
    )
}

@Stable
fun Modifier.cloudClickable(
    enabled: Boolean = true,
    role: Role? = null,
    indication: Indication? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) CloudMotion.PressedScale else CloudMotion.RestScale,
        animationSpec = tween(
            durationMillis = CloudMotion.DurationShortMillis,
            easing = LinearEasing,
        ),
        label = "cloud click scale",
    )

    Modifier
        .scale(scale)
        .clickable(
            enabled = enabled,
            role = role,
            interactionSource = interactionSource,
            indication = indication ?: LocalIndication.current,
            onClick = onClick,
        )
}
