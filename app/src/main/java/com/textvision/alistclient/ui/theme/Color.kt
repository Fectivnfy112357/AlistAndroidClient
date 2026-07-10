package com.textvision.alistclient.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════════
// Brand: Sky Blue
// ══════════════════════════════════════════════════════════════════════════════
val Brand50  = Color(0xFFF4F9FF)
val Brand100 = Color(0xFFE7F2FF)
val Brand200 = Color(0xFFD7E9FF)
val Brand300 = Color(0xFFCDE5FF)
val Brand400 = Color(0xFFBFE0FF)
val Brand500 = Color(0xFF6FB6FF)
val Brand600 = Color(0xFF4A98E8)
val Brand700 = Color(0xFF2D7AD0)
val Brand800 = Color(0xFF9DC9FF)
val Brand900 = Color(0xFFD7E9FF)

// ══════════════════════════════════════════════════════════════════════════════
// Candy (sage / coral / butter / lilac accents)
// ══════════════════════════════════════════════════════════════════════════════
val CandyMint       = Color(0xFF9BE3C8)
val CandyMintBg     = Color(0xFFDAF6EC)
/** Deeper mint tint — used at the right edge of gradient hero cards
 *  (e.g. StorageEditScreen info card). Added in Task 24. */
val CandyMintDeep   = Color(0xFFC2EFE0)
val CandyPink       = Color(0xFFFFC4D6)
val CandyPinkBg     = Color(0xFFFFE4ED)
val CandyLemon      = Color(0xFFFFE89B)
val CandyLemonBg    = Color(0xFFFFF4CC)
val CandyLilac      = Color(0xFFD8C7FF)
val CandyLilacBg    = Color(0xFFECE2FF)

// ══════════════════════════════════════════════════════════════════════════════
// Ink (text hierarchy)
// ══════════════════════════════════════════════════════════════════════════════
val Ink         = Color(0xFF1F3A5F)
val InkSoft     = Color(0xFF6B8AB5)
val InkMute     = Color(0xFFA6BBDB)
val LineColor   = Color(0xFF7EA7E0)
val LineAlpha   = 0.18f
val Surface     = Color(0xFFFFFFFF)
val SurfaceAlpha78 = 0.78f
val SurfaceAlpha92 = 0.92f
val BgStart     = Brand50
val BgEnd       = Brand100

// ══════════════════════════════════════════════════════════════════════════════
// Semantic states (desaturated)
// ══════════════════════════════════════════════════════════════════════════════
val StateError      = Color(0xFFF49AA1)
val StateErrorBg    = Color(0xFFFFE5E8)
val StateWarn       = Color(0xFFF4C77A)
val StateWarnBg     = Color(0xFFFFF1D8)
val StateSuccess    = CandyMint
val StateSuccessBg  = CandyMintBg
val StateWarnFg     = Color(0xFF8B6A2A)
val StateSuccessFg  = Color(0xFF2D9B7C)

// ══════════════════════════════════════════════════════════════════════════════
// Dark-only tokens
// ══════════════════════════════════════════════════════════════════════════════
val DarkBg          = Color(0xFF0F2444)
val DarkSurface     = Color(0xFF1A2D52)
val DarkSurfaceHigh = Color(0xFF243E6A)
val DarkPrimaryContainer = Color(0xFF2D4F7C)
val DarkSecondaryContainer = Color(0xFF1B5A45)
val DarkTertiaryContainer  = Color(0xFF7C2E48)
val DarkErrorColor  = Color(0xFFF8B4B8)


// ══════════════════════════════════════════════════════════════════════════════
// M3 Light ColorScheme
// ══════════════════════════════════════════════════════════════════════════════
val LightScheme: ColorScheme = lightColorScheme(
    primary               = Brand500,
    onPrimary             = Color(0xFFFFFFFF),
    primaryContainer      = Brand300,
    onPrimaryContainer    = Ink,
    secondary             = CandyMint,
    onSecondary           = Color(0xFF1F5A45),
    secondaryContainer    = CandyMintBg,
    onSecondaryContainer  = Color(0xFF1B5A45),
    tertiary              = CandyPink,
    onTertiary            = Color(0xFF7C2E48),
    tertiaryContainer     = CandyPinkBg,
    onTertiaryContainer   = Color(0xFF7C2E48),
    error                 = StateError,
    onError               = Color(0xFFFFFFFF),
    errorContainer        = StateErrorBg,
    onErrorContainer      = Color(0xFFB8505C),
    background            = BgStart,
    onBackground          = Ink,
    surface               = Color(0xFFFFFFFF),
    onSurface             = Ink,
    surfaceVariant        = Color(0xFFE1EFFF),
    onSurfaceVariant      = InkSoft,
    surfaceContainerLowest   = Color(0xFFFFFFFF),
    surfaceContainerLow      = Color(0xFFFBFDFF),
    surfaceContainer         = Surface.copy(alpha = SurfaceAlpha78),
    surfaceContainerHigh     = Surface.copy(alpha = SurfaceAlpha92),
    surfaceContainerHighest  = Color(0xFFFFFFFF),
    outline               = LineColor.copy(alpha = LineAlpha),
    outlineVariant        = LineColor.copy(alpha = LineAlpha),
    scrim                 = Color(0xFF1F3A5F).copy(alpha = 0.35f),
)

// ══════════════════════════════════════════════════════════════════════════════
// M3 Dark ColorScheme
// ══════════════════════════════════════════════════════════════════════════════
val DarkScheme: ColorScheme = darkColorScheme(
    primary               = Brand800,
    onPrimary             = Ink,
    primaryContainer      = DarkPrimaryContainer,
    onPrimaryContainer    = Brand100,
    secondary             = CandyMint,
    onSecondary           = Color(0xFF1B5A45),
    secondaryContainer    = DarkSecondaryContainer,
    onSecondaryContainer  = Color(0xFFDAF6EC),
    tertiary              = CandyPink,
    onTertiary            = Color(0xFF7C2E48),
    tertiaryContainer     = DarkTertiaryContainer,
    onTertiaryContainer   = Color(0xFFFFE4ED),
    error                 = DarkErrorColor,
    onError               = Color(0xFF5C1F23),
    errorContainer        = Color(0xFF5C1F23),
    onErrorContainer      = Color(0xFFFFE5E8),
    background            = DarkBg,
    onBackground          = Brand100,
    surface               = DarkBg,
    onSurface             = Brand100,
    surfaceVariant        = DarkPrimaryContainer,
    onSurfaceVariant      = InkMute,
    surfaceContainerLowest   = Color(0xFF0A1B33),
    surfaceContainerLow      = Color(0xFF13213F),
    surfaceContainer         = DarkSurface,
    surfaceContainerHigh     = DarkSurfaceHigh,
    surfaceContainerHighest  = Color(0xFF2E4A7E),
    outline               = InkMute.copy(alpha = LineAlpha),
    outlineVariant        = InkMute.copy(alpha = LineAlpha * 0.6f),
)


// ══════════════════════════════════════════════════════════════════════════════
// Legacy aliases (preserve compile)
// ══════════════════════════════════════════════════════════════════════════════
@Deprecated("Use Brand500", ReplaceWith("Brand500"))
val IndigoBlue40 = Brand500
@Deprecated("Use Brand800", ReplaceWith("Brand800"))
val IndigoBlue80 = Brand800
@Deprecated("Use Brand300", ReplaceWith("Brand300"))
val IndigoBlue90 = Brand300
@Deprecated("Use Brand100", ReplaceWith("Brand100"))
val IndigoBlue95 = Brand100
@Deprecated("Use Color.White for onPrimary", ReplaceWith("Color.White"))
val IndigoBlue20 = Color(0xFF002D75)

@Deprecated("Use InkSoft", ReplaceWith("InkSoft"))
val Secondary40 = InkSoft
@Deprecated("Use InkMute", ReplaceWith("InkMute"))
val Secondary80 = InkMute
@Deprecated("Use CandyPink", ReplaceWith("CandyPink"))
val Tertiary40 = CandyPink

@Deprecated("Use StateError", ReplaceWith("StateError"))
val Error40 = StateError
@Deprecated("Use DarkErrorColor", ReplaceWith("DarkErrorColor"))
val Error80 = DarkErrorColor

@Deprecated("Use Ink", ReplaceWith("Ink"))
val Neutral10 = Ink
@Deprecated("Use InkSoft", ReplaceWith("InkSoft"))
val Neutral50 = InkSoft
@Deprecated("Use InkMute", ReplaceWith("InkMute"))
val Neutral80 = InkMute

@Deprecated("Use Surface.copy(alpha=0.78f)", ReplaceWith("Surface"))
val SurfaceContainer       = Surface.copy(alpha = 0.78f)
@Deprecated("Use DarkSurface", ReplaceWith("DarkSurface"))
val SurfaceContainerDark   = DarkSurface
@Deprecated("Use DarkSurfaceHigh", ReplaceWith("DarkSurfaceHigh"))
val SurfaceContainerHighDark = DarkSurfaceHigh

@Deprecated("Use MaterialTheme.colorScheme.outline", ReplaceWith(""))
val Outline = LineColor.copy(alpha = LineAlpha)
@Deprecated("Use MaterialTheme.colorScheme.outlineVariant", ReplaceWith(""))
val OutlineVariant = LineColor.copy(alpha = LineAlpha)

@Deprecated("Use tertiaryContainer", ReplaceWith(""))
val FolderTint = CandyPinkBg
@Deprecated("Use secondaryContainer", ReplaceWith(""))
val ImageTint = CandyMintBg

@Deprecated("Use AlistBlue was Brand500", ReplaceWith("Brand500"))
val AlistBlue = Brand500

// Dead-code: object LightColorScheme/DarkColorScheme removed in Task 6.
// Legacy deprecated vals above (IndigoBlue40, Secondary40, etc.) remain for
// backward compatibility with any code that has not yet migrated.
