package com.textvision.alistclient.ui.theme

import androidx.compose.ui.graphics.Color

// ══════════════════════════════════════════════════════════════════════════════
// Brand: Indigo Blue #1F6FEB
// ══════════════════════════════════════════════════════════════════════════════
val IndigoBlue10 = Color(0xFF001A41)
val IndigoBlue20 = Color(0xFF002D75)
val IndigoBlue30 = Color(0xFF00429B)
val IndigoBlue40 = Color(0xFF1F6FEB)   // primary light
val IndigoBlue80 = Color(0xFFA9C7FF)   // primary dark
val IndigoBlue90 = Color(0xFFD8E4FE)
val IndigoBlue95 = Color(0xFFECF1FF)
val IndigoBlue99 = Color(0xFFF8F9FF)

// ══════════════════════════════════════════════════════════════════════════════
// Secondary: Slate Blue (muted neutral accent)
// ══════════════════════════════════════════════════════════════════════════════
val Secondary10 = Color(0xFF151C2C)
val Secondary20 = Color(0xFF2A3142)
val Secondary30 = Color(0xFF414759)
val Secondary40 = Color(0xFF5A6478)    // secondary light
val Secondary80 = Color(0xFFC1C4CF)   // secondary dark
val Secondary90 = Color(0xFFDEE3F2)
val Secondary95 = Color(0xFFECF1FF)
val Secondary99 = Color(0xFFF8F9FF)

// ══════════════════════════════════════════════════════════════════════════════
// Tertiary: Amber (gold accent for emphasis)
// ══════════════════════════════════════════════════════════════════════════════
val Tertiary10 = Color(0xFF271900)
val Tertiary20 = Color(0xFF422D00)
val Tertiary30 = Color(0xFF5C4200)
val Tertiary40 = Color(0xFF7C5800)     // tertiary light
val Tertiary80 = Color(0xFFFFD280)    // tertiary dark
val Tertiary90 = Color(0xFFFFDFA1)
val Tertiary95 = Color(0xFFFFF0C0)
val Tertiary99 = Color(0xFFFFF9EC)

// ══════════════════════════════════════════════════════════════════════════════
// Error: Red (M3 standard)
// ══════════════════════════════════════════════════════════════════════════════
val Error10 = Color(0xFF410002)
val Error20 = Color(0xFF690005)
val Error30 = Color(0xFF93000A)
val Error40 = Color(0xFFBA1A1A)        // error light
val Error80 = Color(0xFFFFB4AB)       // error dark
val Error90 = Color(0xFFFFDAD6)
val Error95 = Color(0xFFFFEDEA)
val Error99 = Color(0xFFFFF9F9)

// ══════════════════════════════════════════════════════════════════════════════
// Neutral (grays for text and surfaces)
// ══════════════════════════════════════════════════════════════════════════════
val Neutral10 = Color(0xFF1B1B1F)      // on-surface light
val Neutral20 = Color(0xFF2F3033)
val Neutral30 = Color(0xFF47474C)
val Neutral40 = Color(0xFF5F5F66)      // on-surface-variant light
val Neutral50 = Color(0xFF777482)      // on-secondary-container light / secondary text
val Neutral60 = Color(0xFF918F9A)      // on-tertiary-container light / tertiary text
val Neutral80 = Color(0xFFC8C6D1)      // on-surface dark
val Neutral90 = Color(0xFFE3E2E6)     // on-surface-variant dark
val Neutral95 = Color(0xFFF1EFF7)
val Neutral99 = Color(0xFFFCFBFF)

// ══════════════════════════════════════════════════════════════════════════════
// Surface Containers (light mode — on white background)
// ══════════════════════════════════════════════════════════════════════════════
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow    = Color(0xFFF8F4FB)
val SurfaceContainer       = Color(0xFFF2EFF4)
val SurfaceContainerHigh   = Color(0xFFECE9EE)
val SurfaceContainerHighest= Color(0xFFE6E3E9)

// ══════════════════════════════════════════════════════════════════════════════
// Surface Containers (dark mode — on dark background)
// ══════════════════════════════════════════════════════════════════════════════
val SurfaceContainerLowestDark = Color(0xFF1F1F23)
val SurfaceContainerLowDark    = Color(0xFF242428)
val SurfaceContainerDark       = Color(0xFF29292D)
val SurfaceContainerHighDark   = Color(0xFF333338)
val SurfaceContainerHighestDark= Color(0xFF3E3E44)

// ══════════════════════════════════════════════════════════════════════════════
// Outline
// ══════════════════════════════════════════════════════════════════════════════
val Outline       = Color(0xFF74777F)   // light
val OutlineDark   = Color(0xFF8E9099)   // dark
val OutlineVariant       = Color(0xFFC4C6D0)  // light
val OutlineVariantDark   = Color(0xFF44474F)  // dark

// ══════════════════════════════════════════════════════════════════════════════
// M3 Light ColorScheme
// ══════════════════════════════════════════════════════════════════════════════
object LightColorScheme {
    val primary               = IndigoBlue40
    val onPrimary             = Color(0xFFFFFFFF)
    val primaryContainer      = IndigoBlue90
    val onPrimaryContainer    = IndigoBlue10

    val secondary             = Secondary40
    val onSecondary           = Color(0xFFFFFFFF)
    val secondaryContainer    = Secondary90
    val onSecondaryContainer  = Secondary10

    val tertiary              = Tertiary40
    val onTertiary            = Color(0xFFFFFFFF)
    val tertiaryContainer     = Tertiary90
    val onTertiaryContainer   = Tertiary10

    val error                 = Error40
    val onError               = Color(0xFFFFFFFF)
    val errorContainer        = Error90
    val onErrorContainer      = Error10

    val background            = Color(0xFFFEFBFF)
    val onBackground          = Neutral10
    val surface               = Color(0xFFFEFBFF)
    val onSurface             = Neutral10
    val surfaceVariant        = Color(0xFFE1E2EC)
    val onSurfaceVariant      = Neutral40

    val surfaceContainerLowest    = SurfaceContainerLowest
    val surfaceContainerLow       = SurfaceContainerLow
    val surfaceContainer          = SurfaceContainer
    val surfaceContainerHigh      = SurfaceContainerHigh
    val surfaceContainerHighest   = SurfaceContainerHighest

    val outline               = Outline
    val outlineVariant        = OutlineVariant
}

// ══════════════════════════════════════════════════════════════════════════════
// M3 Dark ColorScheme
// ══════════════════════════════════════════════════════════════════════════════
object DarkColorScheme {
    val primary               = IndigoBlue80
    val onPrimary             = IndigoBlue20
    val primaryContainer      = IndigoBlue20
    val onPrimaryContainer    = IndigoBlue90

    val secondary             = Secondary80
    val onSecondary           = Secondary20
    val secondaryContainer    = Secondary30
    val onSecondaryContainer  = Secondary90

    val tertiary              = Tertiary80
    val onTertiary            = Tertiary20
    val tertiaryContainer     = Tertiary30
    val onTertiaryContainer   = Tertiary90

    val error                 = Error80
    val onError               = Error20
    val errorContainer        = Error30
    val onErrorContainer      = Error90

    val background            = Color(0xFF1B1B1F)
    val onBackground          = Neutral90
    val surface               = Color(0xFF1B1B1F)
    val onSurface             = Neutral90
    val surfaceVariant        = Color(0xFF44474F)
    val onSurfaceVariant      = Neutral80

    val surfaceContainerLowest    = SurfaceContainerLowestDark
    val surfaceContainerLow       = SurfaceContainerLowDark
    val surfaceContainer          = SurfaceContainerDark
    val surfaceContainerHigh      = SurfaceContainerHighDark
    val surfaceContainerHighest   = SurfaceContainerHighestDark

    val outline               = OutlineDark
    val outlineVariant        = OutlineVariantDark
}

// ══════════════════════════════════════════════════════════════════════════════
// Legacy aliases — preserve for existing code compatibility
// ══════════════════════════════════════════════════════════════════════════════
@Deprecated("Use LightColorScheme.primary / IndigoBlue40", ReplaceWith("IndigoBlue40"))
val AlistBlue = IndigoBlue40

@Deprecated("Use tertiaryContainer")
val FolderTint = Tertiary90

@Deprecated("Use onTertiaryContainer")
val FolderIconTint = Tertiary30

@Deprecated("Use secondaryContainer")
val ImageTint = Secondary90

@Deprecated("Use onSecondaryContainer")
val ImageIconTint = Secondary30

@Deprecated("Use SurfaceContainerHigh")
val TextTint = Color(0xFFE8F8FF)

@Deprecated("Use onSurface")
val TextIconTint = Neutral10

@Deprecated("Use SurfaceContainerHigh")
val GenericFileTint = Color(0xFFEFF2F8)

@Deprecated("Use Neutral50")
val GenericFileIconTint = Color(0xFF667085)
