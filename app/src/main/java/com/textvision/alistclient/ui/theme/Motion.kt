package com.textvision.alistclient.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset

object AppMotion {
    val SpringFast = spring<Float>(dampingRatio = 0.9f, stiffness = 1200f)
    val SpringMedium = spring<Float>(dampingRatio = 0.85f, stiffness = 600f)
    val SpringSlow = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)

    val TweenShort  = tween<Float>(120, easing = FastOutSlowInEasing)
    val TweenMedium = tween<Float>(240, easing = FastOutSlowInEasing)
    val TweenLong   = tween<Float>(400, easing = FastOutSlowInEasing)

    internal const val DurationMediumMillis = 240
    internal val FloatTween: FiniteAnimationSpec<Float> = tween(
        durationMillis = DurationMediumMillis,
        easing = LinearEasing,
    )
    internal val OffsetTween: FiniteAnimationSpec<IntOffset> = tween(
        durationMillis = DurationMediumMillis,
        easing = LinearEasing,
    )
}

@Composable
fun isReducedMotion(): Boolean {
    val context = LocalContext.current
    val scale = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)
    return scale == 0f
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
    val TweenShort: Int = 120
    val TweenMedium: Int = 240
    val TweenLong: Int = 400
    val Easing = FastOutSlowInEasing
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
        targetValue = if (enabled && pressed) 0.94f else 1f,
        animationSpec = tween(
            durationMillis = 120,
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
