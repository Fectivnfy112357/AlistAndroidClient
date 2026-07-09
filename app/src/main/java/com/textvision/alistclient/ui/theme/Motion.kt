package com.textvision.alistclient.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

object AppMotion {
    // Spring specs
    val SpringFast: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessHigh
    )
    val SpringMedium: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    val SpringSlow: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
    // Tween durations
    const val TweenShort: Int = 120
    const val TweenMedium: Int = 240
    const val TweenLong: Int = 400
    val Easing = FastOutSlowInEasing

    internal const val DurationShortMillis = 120
    internal const val DurationMediumMillis = 240
    internal const val DurationLongMillis = 300

    internal const val PressedScale = 0.94f
    internal const val RestScale = 1f

    internal val FloatTween: FiniteAnimationSpec<Float> = tween(
        durationMillis = DurationMediumMillis,
        easing = LinearEasing,
    )

    internal val OffsetTween: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = DurationMediumMillis,
        easing = LinearEasing,
    )
}

/**
 * @deprecated Use [AppMotion]. Retained as a forwarding alias during the
 * UI Expressive followup migration; will be removed once no production code
 * references it. See `docs/superpowers/specs/2026-07-09-followups-fixes-and-deviations-design.md` §3.2.
 */
@Deprecated("Use AppMotion", ReplaceWith("AppMotion"))
object CloudMotion {
    val SpringFast: AnimationSpec<Float> get() = AppMotion.SpringFast
    val SpringMedium: AnimationSpec<Float> get() = AppMotion.SpringMedium
    val SpringSlow: AnimationSpec<Float> get() = AppMotion.SpringSlow
    const val TweenShort: Int = AppMotion.TweenShort
    const val TweenMedium: Int = AppMotion.TweenMedium
    const val TweenLong: Int = AppMotion.TweenLong
    val Easing get() = AppMotion.Easing
    internal const val DurationShortMillis: Int = AppMotion.DurationShortMillis
    internal const val DurationMediumMillis: Int = AppMotion.DurationMediumMillis
    internal const val DurationLongMillis: Int = AppMotion.DurationLongMillis
    internal const val PressedScale: Float = AppMotion.PressedScale
    internal const val RestScale: Float = AppMotion.RestScale
    internal val FloatTween: FiniteAnimationSpec<Float> get() = AppMotion.FloatTween
    internal val OffsetTween: FiniteAnimationSpec<IntOffset> get() = AppMotion.OffsetTween
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
        targetValue = if (enabled && pressed) AppMotion.PressedScale else AppMotion.RestScale,
        animationSpec = tween(
            durationMillis = AppMotion.DurationShortMillis,
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
