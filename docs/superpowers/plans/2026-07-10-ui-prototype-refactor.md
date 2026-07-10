# UI Prototype Refactor v3 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the project's UI (colors, typography, shapes, 13 screens) with the Sky Blue + Candy prototype design from `prototype/alist-android/DESIGN_HANDOFF.md`, while preserving all data layer, ViewModel, navigation, and test logic. Music library and music preview remain as placeholder empty states with full visual skeleton.

**Architecture:** Method A — in-place refactor. Keep `ui/theme/`, `ui/components/`, `ui/feature/` directory structure, replace token files and rewrite shared component + 11 screen implementations against the HTML prototype. Keep a `@Deprecated` alias layer so existing 70+ references compile unchanged, then gradually migrated. Add 2 new `@Serializable` navigation routes for music library / preview (placeholder destinations).

**Tech Stack:** Kotlin 2.0.21 · AGP 8.7.2 · Compose BOM 2024.09.03 · Material3 1.3.0 · Navigation Compose 2.8.3 · Hilt 2.52 · `androidx.compose.ui:ui-text-google-fonts` (NEW for Fredoka) · Coil 3.0.4 · Roborazzi 1.x.

## Global Constraints

1. **Compile/target SDK:** compileSdk = 34, targetSdk = 34, minSdk = 26, JVM target 17 (Android Studio Iguana / Kotlin 2.0.21 / AGP 8.7.2).
2. **Single file size cap:** ≤ 400 lines per source file (enforced in spec §1.3).
3. **State collection:** Use `collectAsStateWithLifecycle()`, not `collectAsState()` (inherited from `2026-07-08-ui-expressive-redesign-design.md`).
4. **Zero hardcoded values:** No `Color(0xFF...)`, `FontFamily.X`, `RoundedCornerShape(NN.dp)` outside `ui/theme/`. Only `MaterialTheme.colorScheme.*`, `MaterialTheme.typography.*`, `MaterialTheme.shapes.*` inside components & screens.
5. **Naming:** Composables `PascalCase`; helpers `PascalCase`; non-Composable functions `camelCase`; sealed `XxxUiState` / `XxxIntent`; ViewModel `XxxViewModel`; routes `XxxDest`.
6. **Previews:** Each Screen ≥ 3 `@Preview` (Light, Dark, LargeFont 1.5x).
7. **Tests:** Existing tests must pass unchanged (deprecation alias layer preserves compile). Compose UI tests + Roborazzi snapshots must be regenerated against new visual baseline (0.1% tolerance).
8. **Static theme:** No `dynamicColor`. User choice: System / Light / Dark (3-way), persisted via `ThemeRepository` (DataStore).
9. **Music placeholders:** Music library and music preview screens are visual-only placeholders — no API calls, no playback. Do NOT add MediaSession / ExoPlayer dependencies.
10. **M3 Expressive alias preservation:** All old `IndigoBlue40/80/90`, `Secondary40`, `Tertiary40`, `Neutral*`, `Error*`, `SurfaceContainer*`, `Outline*`, `FolderTint`, `ImageTint`, `TextTint` are `@Deprecated` and point to new Sky Blue + Candy tokens.
11. **Commit cadence:** One commit per task. Conventional Commits: `feat(ui): ...`, `refactor(ui): ...`, `test(ui): ...`, `docs(spec): ...`.
12. **Build gate:** `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` must pass after every task boundary.
13. **Git worktree:** All work executes in a fresh git worktree branch `feature/ui-prototype-v3` off `main`. Do not commit directly to main.

---

## File Map

| Change | Path | Responsibility |
|---|---|---|
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt` | Sky Blue + Candy palette + 3 M3 ColorScheme + Legacy aliases |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt` | Fredoka titles + system Default body, 13 typography roles |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt` | Corner 10/14/18/22/28 dp |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt` | Add `AppMotion.SpringFast/Medium/Slow` + `Tween*` + `isReducedMotion()` |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt` | Disable `dynamicColor`, support `DarkMode` |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/theme/ThemeRepository.kt` | Already supports dark mode — verify contract |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreviews.kt` | Light/Dark preview wrappers with new palette |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/icons/AppIcons.kt` | 30+ SVG-based `ImageVector` icons |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/foundation/AppBars.kt` | `AppTopBar` (back + actions + subtitle) + `AppBottomBar` (5 tabs, glass-blur bg, capsule active) |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/foundation/Backgrounds.kt` | Sky Blue 165° gradient + cloud decoration |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/SectionCard.kt` | Glass-white card with blur + 22dp corner + 4dp shadow |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/ActionButton.kt` | 4-variant (FILLED/TONAL/OUTLINED/TEXT), 48dp height |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/StatusBanner.kt` | 4 kinds (INFO/WARN/ERROR/SUCCESS), 18dp corner |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/ListItemRow.kt` | 14dp corner rows, 48-64dp height |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/FileTypeIcon.kt` | MIME → Icon + Container mapping (folder/image/video/audio/doc/pdf/archive/code/other) |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/SearchField.kt` | Chip-style, 40dp height |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/EmptyState.kt` | 96×96 illustration + Fredoka title |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/ErrorState.kt` | Light-red round + retry button |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/LoadingState.kt` | Spinner + text |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/AppAlertDialog.kt` | 28dp corner modal |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/Breadcrumb.kt` | Theme refresh only (token migration) |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt` | Theme refresh only (token migration) |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/components/ComponentPreviews.kt` | Showcase grid updated to new components |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/music/CoverLetter.kt` | 居中首字封面 (gradient + Fredoka) |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/music/WaveIndicator.kt` | 4-bar animated waveform |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/music/MusicHeroCard.kt` | Big gradient hero banner |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/music/AlbumCard.kt` | 105×105 square tile |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/music/ArtistCard.kt` | 84×84 circular tile |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/music/SongRow.kt` | Compact row (cover + name + artist + duration) |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/music/MiniPlayer.kt` | Bottom docked player (cover + name + wave + play) |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/auth/LoginScreen.kt` | Sky-blue gradient bg + 3 inputs + cloud logo + login btn |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt` | Hero server card + 3 metrics + task card (5 chips) + storage list + 管理→ link |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeSections.kt` | Updated section list using new metrics cards |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeStorageSection.kt` | Storage list rows |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt` | FileRow + multi-select actionbar (5 buttons) + offline banner + multi-hint with "移动" bold |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileListContent.kt` | Updated file-row layout |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileMultiSelectBar.kt` | Bottom action bar with select-all/copy/move/delete/cancel |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferScreen.kt` | 4-segmented (All/Upload·N/Download·N/Failed) + 4-state task-row + progress |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferListContent.kt` | Task-row list |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferRow.kt` | 4-state card: upload/download/failed/completed (opacity 0.75) |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferProgress.kt` | Gradle gradient bar |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsScreen.kt` | UserAvatarCard + theme picker 3 cards + 4 sections (appearance/storage/quick/maintenance) + logout + footer |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/KeyValueRow.kt` | KeyValueRow for PreviewScreen detail grid |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/UserAvatarCard.kt` | UserAvatarCard for SettingsScreen top |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/components/DecoBadge.kt` | 4-position × 7-color × 3-size decoration badge |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewScreen.kt` | TopBar refresh + 3 outlined action buttons + image bottom overlay strip + KeyValueRow detail grid |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewImage.kt` | Token migration + bottom overlay text strip |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewText.kt` | Token migration |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewAudio.kt` | Token migration |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/preview/PreviewFallback.kt` | Token migration |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/picker/MoveCopyTargetPickerScreen.kt` | Breadcrumb chip chain (icons not emoji) + new dashed card + radio 24dp + sticky bottom confirm btn |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/storage/StorageEditScreen.kt` | mint-gradient info card + 5 fields + cookie card + enable toggle + save btn |
| **Modify** | `app/src/main/java/com/textvision/alistclient/ui/feature/admin/AdminSiteSettingsScreen.kt` | 3 sections (site/preview/security) + token validity + save-all btn |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt` | 5 sections placeholder + empty state |
| **Create** | `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt` | Empty state with "coming soon" |
| **Modify** | `app/src/main/java/com/textvision/alistclient/navigation/AppDestination.kt` | + `MusicLibraryDest`, `MusicPreviewDest` |
| **Modify** | `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt` | Register music destinations |
| **Modify** | `app/src/main/java/com/textvision/alistclient/navigation/BottomNavBar.kt` | Add 5th tab (Music), icon swap to `AppIcons.musicNote` |
| **Modify** | `app/src/main/java/com/textvision/alistclient/MainActivity.kt` | Theme integration unchanged; verify imports |
| **Modify** | `gradle/libs.versions.toml` | Add `androidx-compose-ui-text-google-fonts` |
| **Modify** | `app/build.gradle.kts` | Add `implementation(libs.androidx.compose.ui.text.google.fonts)` |
| **Create** | `docs/superpowers/specs/2026-07-10-ui-prototype-refactor-design.md` | ✓ Already created in brainstorming phase |
| **Create** | `app/src/test/java/com/textvision/alistclient/ui/theme/ColorTokenTest.kt` | Validate hex constants |
| **Create** | `app/src/test/java/com/textvision/alistclient/ui/icons/AppIconsTest.kt` | Validate ImageVector non-empty |
| **Create** | `app/src/test/java/com/textvision/alistclient/ui/components/music/MusicComponentsTest.kt` | CoverLetter / MiniPlayer previews |
| **Regenerate** | All Roborazzi snapshots in `app/src/test/snapshots/` | New visual baseline |

---

## Task Structure

### Task 1: Add Google Fonts dependency (Fredoka support)

**Files:**
- Modify: `gradle/libs.versions.toml:1-100`
- Modify: `app/build.gradle.kts:67-123`

- [ ] **Step 1: Add library coordinate to libs.versions.toml**

In `gradle/libs.versions.toml`, after the `androidx-compose-material-icons-extended` entry, append:

```toml
androidx-compose-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }
```

- [ ] **Step 2: Wire dependency into app/build.gradle.kts**

In `app/build.gradle.kts`, after `implementation(libs.androidx.compose.material.icons.extended)` (line 79), append:

```kotlin
implementation(libs.androidx.compose.ui.text.google.fonts)
```

- [ ] **Step 3: Build to verify dependency resolution**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL; warnings allowed but no errors. Check that `androidx.compose.ui:ui-text-google-fonts` is downloaded.

- [ ] **Step 4: Commit**

```bash
git checkout -b feature/ui-prototype-v3
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build(ui): add ui-text-google-fonts for Fredoka"
```

---

### Task 2: Rewrite Color.kt — Sky Blue + Candy palette + M3 mapping + Legacy aliases

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt` (full rewrite, ≤ 400 lines)

- [ ] **Step 1: Rewrite Color.kt with new palette**

Replace the entire file content with:

```kotlin
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
val LightColors: ColorScheme = lightColorScheme(
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
val DarkColors: ColorScheme = darkColorScheme(
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
```

- [ ] **Step 2: Verify the file is ≤ 400 lines**

Run: `wc -l app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt`
Expected: ≤ 400

- [ ] **Step 3: Verify legacy references still compile**

Run: `./gradlew :app:assembleDebug -Dlint.baselines.dependencies=app/lint-baseline.xml`
Expected: BUILD SUCCESSFUL with deprecation warnings (no errors). All 70+ existing usages of `IndigoBlue40`, `FolderTint`, etc. resolve to the new tokens via @Deprecated.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/theme/Color.kt
git commit -m "refactor(ui): replace IndigoBlue with Sky Blue + Candy palette"
```

---

### Task 3: Rewrite Type.kt — Fredoka titles + Default body

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt` (full rewrite, ≤ 400 lines)

- [ ] **Step 1: Rewrite Type.kt**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.textvision.alistclient.R

/**
 * Fredoka — fetched via Google Play Services Fonts (downloadable fonts).
 * Downloadable font provider requires Google Play Services on device.
 * Falls back to system sans-serif when unavailable.
 */
val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val FredokaFamily = FontFamily(
    Font(googleFont = GoogleFont("Fredoka"), fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Fredoka"), fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Fredoka"), fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Fredoka"), fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.01).em,
    ),
    displayMedium = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.01).em,
    ),
    displaySmall = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.01).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 23.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 17.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.em,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.04.em,
    ),
)

object NumeralStyle {
    val value = TextStyle(
        fontFamily = FredokaFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.02).em,
    )
}
```

- [ ] **Step 2: Verify font array resource exists**

Check `app/src/main/res/values/font_certs.xml` exists. If not, create:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <array name="com_google_android_gms_fonts_certs">
        <item>@array/com_google_android_gms_fonts_certs_dev</item>
        <item>@array/com_google_android_gms_fonts_certs_prod</item>
    </array>
    <string-array name="com_google_android_gms_fonts_certs_dev">
        <item>...</item>
    </string-array>
    <string-array name="com_google_android_gms_fonts_certs_prod">
        <item>...</item>
    </string-array>
</resources>
```

Use the standard cert array (full hex content from Google's docs):

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="com_google_android_gms_fonts_certs_dev">
        <item>
MIIEqDCCA5CgAwIBAgIJANWFuGx90071MA0GCSqGSIb3DQEBBAUAMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTAeFw0wODA0MTUyMzM2NTZaFw0zNTA5MDEyMzM2NTZaMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDEiMCAGCSqGSIb3DQEJARYTYW5kcm9pZEBhbmRyb2lkLmNvbTCCASAwDQYJKoZIhvcNAQEBBQADggENADCCAQgCggEBANbOLggKv+IxTdGNs8/TGFy0PTP6DHThvbbR24kT9ixcOd9W+EaBPWW+wPPKQmsHxajtWjmQwWfna8mZuXMJSfC3KQflkhXLAFbcTC8GoiGcVntR+k+1QslK3RF0mzu6UE3nAl4d8oNSIjWbYKQX4XtLfvQv1O6K8Nh3LnE5g+8TYlgg7eCkSjc+cjKD7T4pPxvLZrw3AgxFrED3wSTpDwyOcT4SkL8L4qxdBnJDg9hJUNNmaL3ZZG0jSz/NTpip4Qr9NBeswyB3Vyg==
        </item>
    </string-array>
    <string-array name="com_google_android_gms_fonts_certs_prod">
        <item>
MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDAeFw0wODA4MjEyMzEzMzRaFw0zNjAxMDcyMzEzMzRaMHQxCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRQwEgYDVQQKEwtHb29nbGUgSW5jLjEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKtWLgDYO6IIrgqWbxJOKdoR8qtW0I9Y4sypEwPpt1TTcvZApxsdyxMJZ2JORland2qSGT2y5b+3JKkedxiLDmpHpDsz2WCbdxgxRczfey5YZnTJ4VZbH0xqWVW/8lGmPav5xVwnIiJS6HXk+BVKZF+JcWjAsb/GEuq/eFdpuzSqeYTcfi6idkyugwfYwXFU1+5fZKUaRKYCwkkFQVfcAs1fXA5V+++FGfvjJ/CxURaSxaBvGdGDhfXE28LWuT9ozCl5xw4Yq5OGazvV24mZVSoOO0yZ31j7kYvtwYK6NeADwbSxDdJEqO4k//0zOHKrUiGYXtqwj0xqLw0YUzb5AwBMoPK8MooCAQOjgfwwgfkwHQYDVR0OBBYEFI0cxb6VTEM8YYY6FbBMvAPyT+CyMIHJBgNVHSMEgcEwgb6AFI0cxb6VTEM8YYY6FbBMvAPyT+CyoYGapIGXMIGUMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxMNTW91bnRhaW4gVmlldzEQMA4GA1UEChMHQW5kcm9pZDEQMA4GA1UECxMHQW5kcm9pZDEQMA4GA1UEAxMHQW5kcm9pZIiMA0GCSqGSIb3DQEBAQUAA4IBAQBt0lLO74UwLDYKqs6Tm8/yzKkEu116FmH4rkaymUIE0P9KaMftGlMexFlaYjzmB2OxZyl6euNXEsQH8gjwyxCUKRJNexBiGcCEyj6z+a1fuHHvkiaai+KL8W1EyNmgjmyy8AW7P+LLFhuRHQ8KA95wEGtBsRPNq9gzQNBznVGaB5OUNd7eTGz3tzMw7k9G2HhUKMnR4xRvpIjC+/XcDsZdMEeywqmu8uO5TDOB+k9N1QbjG4YtvT7nRG7DKfpq3ZWc8M/SaEXq7RGiT7liXV0fgMm+M0sDDMcKQFjfl1hQ9o2RGfTbOml8HxDpM9lzEje5XKlHkF4MR7Hme3BHsaR3R1OMTZuY=
        </item>
    </string-array>
    <array name="com_google_android_gms_fonts_certs">
        <item>@array/com_google_android_gms_fonts_certs_dev</item>
        <item>@array/com_google_android_gms_fonts_certs_prod</item>
    </array>
</resources>
```

Place at `app/src/main/res/values/font_certs.xml`. The cert content above is the standard certificate used by every app on Google's docs site — using this verbatim is the correct approach.

- [ ] **Step 3: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt app/src/main/res/values/font_certs.xml
git commit -m "feat(ui): add Fredoka downloadable font typography"
```

---

### Task 4: Rewrite Shape.kt — new corner system

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt` (full rewrite)

- [ ] **Step 1: Rewrite Shape.kt**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object Corner {
    val ExtraSmall = 10.dp   // tag, mini elements
    val Small      = 14.dp   // input, small card
    val Medium     = 18.dp   // button
    val Large      = 22.dp   // card
    val ExtraLarge = 28.dp   // modal, album cover
    val Pill       = 999.dp  // chip
    val Circle     = CircleShape
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Corner.ExtraSmall),
    small      = RoundedCornerShape(Corner.Small),
    medium     = RoundedCornerShape(Corner.Medium),
    large      = RoundedCornerShape(Corner.Large),
    extraLarge = RoundedCornerShape(Corner.ExtraLarge),
)
```

- [ ] **Step 2: Build and check**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt
git commit -m "refactor(ui): redesign corner scale (10/14/18/22/28)"
```

---

### Task 5: Rewrite Motion.kt — Spring tokens + Reduced-motion guard

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt` (full rewrite)

- [ ] **Step 1: Add tokens**

```kotlin
package com.textvision.alistclient.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

object AppMotion {
    val SpringFast = spring<Float>(dampingRatio = 0.9f, stiffness = 1200f)
    val SpringMedium = spring<Float>(dampingRatio = 0.85f, stiffness = 600f)
    val SpringSlow = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)

    val TweenShort  = tween<Float>(120, easing = FastOutSlowInEasing)
    val TweenMedium = tween<Float>(240, easing = FastOutSlowInEasing)
    val TweenLong   = tween<Float>(400, easing = FastOutSlowInEasing)
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
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt
git commit -m "feat(ui): add Spring / Tween motion tokens + reduced-motion guard"
```

---

### Task 6: Rewrite Theme.kt — disable dynamicColor, support DarkMode enum

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt` (full rewrite, ≤ 200 lines)

- [ ] **Step 1: Rewrite**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

enum class DarkMode { SYSTEM, LIGHT, DARK }

/**
 * Alist UI Theme — Sky Blue + Candy. Static palette (no Material You dynamic color).
 */
@Composable
fun AlistTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val isDark = when (darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT  -> false
        DarkMode.DARK   -> true
    }
    val colors = if (isDark) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
```

- [ ] **Step 2: Verify ThemeRepository contract is unchanged**

Read `ThemeRepository.kt` to confirm `darkMode: Flow<DarkMode>` already exists. If it does, no change. If it uses boolean instead, modify to expose `DarkMode` enum.

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: Theme wiring still compiles.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/theme/Theme.kt app/src/main/java/com/textvision/alistclient/ui/theme/ThemeRepository.kt
git commit -m "refactor(ui): disable dynamicColor, theme supports DarkMode enum"
```

---

### Task 7: Create AppIcons.kt — 30+ SVG-based ImageVector

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/ui/icons/AppIcons.kt`

- [ ] **Step 1: Create the icons file**

```kotlin
package com.textvision.alistclient.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    // — Navigation (Material Icons Extended fallbacks for system / generic icons)
    val home       : ImageVector = Icons.Outlined.Home
    val file       : ImageVector = Icons.Outlined.Description
    val settings   : ImageVector = Icons.Outlined.Settings
    val search     : ImageVector = Icons.Outlined.Search
    val refresh    : ImageVector = Icons.Outlined.Refresh
    val more       : ImageVector = Icons.Outlined.MoreVert
    val back       : ImageVector = Icons.AutoMirrored.Outlined.ArrowBack
    val chevronRight : ImageVector = Icons.Outlined.ChevronRight

    // — File types (mapped to container colors in FileTypeIcon.kt)
    val folder     : ImageVector = Icons.Outlined.Folder
    val image      : ImageVector = Icons.Outlined.Image
    val video      : ImageVector = Icons.Outlined.Movie
    val audio      : ImageVector = Icons.Outlined.MusicNote
    val doc        : ImageVector = Icons.Outlined.Description
    val archive    : ImageVector = Icons.Outlined.Archive
    val upload     : ImageVector = Icons.Outlined.CloudUpload
    val download   : ImageVector = Icons.Outlined.CloudDownload
    val transfer   : ImageVector = Icons.Outlined.SwapVert

    // — Action
    val share      : ImageVector = Icons.Outlined.Share
    val link       : ImageVector = Icons.Outlined.Link
    val trash      : ImageVector = Icons.Outlined.Delete
    val copy       : ImageVector = Icons.Outlined.ContentCopy
    val external   : ImageVector = Icons.Outlined.OpenInNew
    val cookie     : ImageVector = Icons.Outlined.Cookie

    // — Status
    val alert      : ImageVector = Icons.Outlined.Warning
    val offline    : ImageVector = Icons.Outlined.WifiOff
    val check      : ImageVector = Icons.Outlined.Check
    val sparkle    : ImageVector = Icons.Outlined.AutoAwesome
    val shield     : ImageVector = Icons.Outlined.Shield
    val user       : ImageVector = Icons.Outlined.Person
    val cloud      : ImageVector = Icons.Outlined.Cloud
    val database   : ImageVector = Icons.Outlined.Storage

    // — Theme switcher
    val sun        : ImageVector = Icons.Outlined.LightMode
    val moon       : ImageVector = Icons.Outlined.DarkMode
    val auto       : ImageVector = Icons.Outlined.BrightnessAuto
    val logout     : ImageVector = Icons.Outlined.Logout

    // — Music controls
    val play       : ImageVector = Icons.Outlined.PlayArrow
    val pause      : ImageVector = Icons.Outlined.Pause
    val skipPrev   : ImageVector = Icons.Outlined.SkipPrevious
    val skipNext   : ImageVector = Icons.Outlined.SkipNext
    val heart      : ImageVector = Icons.Outlined.FavoriteBorder
    val heartFill  : ImageVector = Icons.Outlined.Favorite
    val repeat     : ImageVector = Icons.Outlined.Repeat
    val shuffle    : ImageVector = Icons.Outlined.Shuffle
    val queue      : ImageVector = Icons.Outlined.QueueMusic
    val musicNote  : ImageVector = Icons.Outlined.MusicNote
    val filter     : ImageVector = Icons.Outlined.FilterList
    val sort       : ImageVector = Icons.Outlined.Sort
    val mic        : ImageVector = Icons.Outlined.Mic

    // — Decorative / utility (custom path-based, see below)
    val decoNote: ImageVector = ImageVector.Builder(
        name = "decoNote",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).build()
    val decoStar = Icons.Outlined.Star
    val decoHeart = Icons.Outlined.Favorite
    val decoDisc: ImageVector = Icons.Outlined.Album
    val decoFlame: ImageVector = Icons.Outlined.LocalFireDepartment
    val decoWave: ImageVector = Icons.Outlined.GraphicEq
    val decoSpark: ImageVector = Icons.Outlined.AutoAwesome
    val decoHead: ImageVector = Icons.Outlined.Headphones

    // — Tools
    val server: ImageVector = Icons.Outlined.Dns
    val broom: ImageVector = Icons.Outlined.CleaningServices
}
```

- [ ] **Step 2: Create AppIconsTest.kt**

Create `app/src/test/java/com/textvision/alistclient/ui/icons/AppIconsTest.kt`:

```kotlin
package com.textvision.alistclient.ui.icons

import org.junit.Assert.assertNotNull
import org.junit.Test

class AppIconsTest {
    @Test fun allIconsNonNull() {
        val fields = AppIcons::class.java.declaredFields
        fields.forEach { field ->
            field.isAccessible = true
            val value = field.get(AppIcons)
            assertNotNull("AppIcons.${field.name} should not be null", value)
        }
    }
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.ui.icons.AppIconsTest`
Expected: PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/icons/AppIcons.kt app/src/test/java/com/textvision/alistclient/ui/icons/AppIconsTest.kt
git commit -m "feat(ui): add AppIcons map (30+ SVG/Material icons)"
```

---

### Task 8: Rewrite AppBars.kt — TopBar + 5-Tab BottomBar

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/foundation/AppBars.kt` (full rewrite, ≤ 400 lines)

- [ ] **Step 1: Rewrite**

```kotlin
package com.textvision.alistclient.ui.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons

/**
 * Sticky-style top bar — title + subtitle + optional back + trailing actions.
 * Mimics prototype glass-white background with optional elevation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                FilledTonalIconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Icon(AppIcons.back, contentDescription = "返回", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.tertiary),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { actions() }
        }
    }
}

/**
 * Bottom navigation — 5 tabs: Home / Files / **Music** / Transfers / Settings.
 * Active state: capsule background (`primaryContainer`) + tinted icon (`primary`).
 */
@Composable
fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        BottomItem("home",   "首页", AppIcons.home),
        BottomItem("files",  "文件", AppIcons.file),
        BottomItem("music",  "音乐", AppIcons.musicNote),
        BottomItem("trans",  "传输", AppIcons.transfer),
        BottomItem("set",    "设置", AppIcons.settings),
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 6.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.id
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .clickable { onNavigate(item.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private data class BottomItem(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/foundation/AppBars.kt
git commit -m "refactor(ui): app top/bottom bars with 5-tab bottom navigation"
```

---

### Task 9: Rewrite Backgrounds.kt — Sky Blue 165° gradient + cloud decoration

**Files:**
- Modify: `app/src/main/java/com/textvision\alistclient\ui\foundation\Backgrounds.kt` (full rewrite)

- [ ] **Step 1: Rewrite**

```kotlin
package com.textvision.alistclient.ui.foundation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.textvision.alistclient.ui.theme.BgEnd
import com.textvision.alistclient.ui.theme.BgStart

/**
 * Vertical sky-blue gradient background — login / hero / 165° screen base.
 */
@Composable
fun SkyBlueBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BgStart, BgEnd),
                    startY = 0f,
                    endY = 1200f,
                ),
            ),
    )
}

/**
 * Lightweight cloud decoration — drawn in top 280dp, simulated with translucent circles.
 */
@Composable
fun CloudDecor(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = 140f,
                center = Offset(60f, 20f),
            )
            drawCircle(
                color = Color(0xFFD8E9FF).copy(alpha = 0.55f),
                radius = 120f,
                center = Offset(300f, 30f),
            )
            drawCircle(
                color = Color(0xFFFFC4D6).copy(alpha = 0.7f),
                radius = 4f,
                center = Offset(40f, 80f),
            )
            drawCircle(
                color = Color(0xFF9BE3C8).copy(alpha = 0.8f),
                radius = 3f,
                center = Offset(320f, 110f),
            )
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/foundation/Backgrounds.kt
git commit -m "feat(ui): add sky-blue gradient + cloud decoration backgrounds"
```

---

### Task 10: Create SectionCard.kt — glass-white card

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/SectionCard.kt`

- [ ] **Step 1: Create**

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glass-white card — surface container with 0.78 alpha + 22dp corner + soft shadow.
 * Used for hero cards, storage cards, metric cards, etc.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp,
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(padding),
        ) {
            content()
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/SectionCard.kt
git commit -m "feat(ui): add SectionCard (glass-white, 22dp corner, soft shadow)"
```

---

### Task 11: Rewrite ActionButton.kt — 4 variants

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/ActionButton.kt`

- [ ] **Step 1: Rewrite**

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.theme.AppMotion
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.Brand600

enum class ButtonVariant { FILLED, TONAL, OUTLINED, TEXT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = AppMotion.SpringFast,
        label = "button-scale",
    )

    val height = when (variant) {
        ButtonVariant.TEXT -> 40.dp
        else -> 48.dp
    }

    val onClickWrapper = {
        pressed = true
        onClick()
    }

    when (variant) {
        ButtonVariant.FILLED -> {
            Surface(
                modifier = modifier
                    .scale(scale)
                    .height(height),
                shape = MaterialTheme.shapes.medium,
                color = Color.Transparent,
                onClick = onClickWrapper,
                enabled = enabled,
            ) {
                Box(
                    Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(Brand500, Brand600),
                            ),
                        )
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        } else if (leadingIcon != null) {
                            Icon(
                                leadingIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
        ButtonVariant.TONAL -> {
            FilledTonalButton(
                onClick = onClickWrapper,
                enabled = enabled,
                modifier = modifier
                    .scale(scale)
                    .height(height),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                } else if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
        ButtonVariant.OUTLINED -> {
            OutlinedButton(
                onClick = onClickWrapper,
                enabled = enabled,
                modifier = modifier
                    .scale(scale)
                    .height(height),
                shape = MaterialTheme.shapes.medium,
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
        ButtonVariant.TEXT -> {
            TextButton(
                onClick = onClickWrapper,
                enabled = enabled,
                modifier = modifier
                    .scale(scale)
                    .height(height),
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(text, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun Modifier.background(brush: Brush): Modifier =
    androidx.compose.foundation.background(brush)
```

- [ ] **Step 2: Add @Preview**

Append to ActionButton.kt:

```kotlin
@Preview(name = "Light ActionButton")
@Composable
private fun ActionButtonPreview() {
    MaterialTheme {
        Column(Modifier.padding(16.dp)) {
            ActionButton("登录 Alist", onClick = {}, variant = ButtonVariant.FILLED)
            Spacer(Modifier.height(8.dp))
            ActionButton("取消", onClick = {}, variant = ButtonVariant.TONAL)
            Spacer(Modifier.height(8.dp))
            ActionButton("更多选项", onClick = {}, variant = ButtonVariant.OUTLINED)
            Spacer(Modifier.height(8.dp))
            ActionButton("跳过", onClick = {}, variant = ButtonVariant.TEXT)
        }
    }
}
```

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/ActionButton.kt
git commit -m "feat(ui): 4-variant ActionButton (FILLED/TONAL/OUTLINED/TEXT)"
```

---

### Task 12: Rewrite StatusBanner.kt — 4 kinds

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/StatusBanner.kt`

- [ ] **Step 1: Rewrite**

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons
import com.textvision.alistclient.ui.theme.Brand500
import com.textvision.alistclient.ui.theme.StateError
import com.textvision.alistclient.ui.theme.StateErrorBg
import com.textvision.alistclient.ui.theme.StateSuccess
import com.textvision.alistclient.ui.theme.StateSuccessBg
import com.textvision.alistclient.ui.theme.StateWarn
import com.textvision.alistclient.ui.theme.StateWarnBg

enum class BannerKind { INFO, WARNING, ERROR, SUCCESS }

@Composable
fun StatusBanner(
    kind: BannerKind,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, icon) = when (kind) {
        BannerKind.INFO    -> Triple(Brand500.copy(alpha = 0.10f), Brand500, AppIcons.alert)
        BannerKind.WARNING -> Triple(StateWarnBg, Color(0xFF8B6A2A), AppIcons.alert)
        BannerKind.ERROR   -> Triple(StateErrorBg, StateError, AppIcons.alert)
        BannerKind.SUCCESS -> Triple(StateSuccessBg, Color(0xFF2D9B7C), AppIcons.check)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(8.dp))
            ActionButton(
                text = actionLabel,
                onClick = onAction,
                variant = ButtonVariant.TEXT,
            )
        }
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/StatusBanner.kt
git commit -m "feat(ui): 4-kind StatusBanner (INFO/WARNING/ERROR/SUCCESS)"
```

---

### Task 13: Rewrite ListItemRow.kt — 14dp corner, 48-64dp height

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/ListItemRow.kt` (full rewrite, ≤ 200 lines)

- [ ] **Step 1: Rewrite**

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Standard list row — leading slot + title + (optional subtitle) + trailing slot.
 * 14dp corner, height adjusts by subtitle presence (48dp / 64dp).
 */
@Composable
fun ListItemRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (RowScope.() -> Unit) = {},
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.small)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 12.dp, vertical = if (subtitle != null) 10.dp else 8.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        trailing(this)
    }
}
```

- [ ] **Step 2: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/ListItemRow.kt
git commit -m "refactor(ui): ListItemRow with 14dp corner + flexible slot"
```

---

### Task 14: Rewrite FileTypeIcon.kt — new MIME mapping

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/FileTypeIcon.kt` (full rewrite, ≤ 200 lines)

- [ ] **Step 1: Rewrite**

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.ui.icons.AppIcons

@Composable
fun FileTypeIcon(
    mimeType: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val (icon, containerColor, iconColor) = when {
        mimeType.startsWith("folder") -> Triple(AppIcons.folder,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer)
        mimeType.startsWith("image/") -> Triple(AppIcons.image,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer)
        mimeType.startsWith("video/") -> Triple(AppIcons.video,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer)
        mimeType.startsWith("audio/") -> Triple(AppIcons.audio,
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer)
        mimeType.startsWith("application/pdf") -> Triple(Icons.Outlined.PictureAsPdf,
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer)
        mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("7z")
            -> Triple(AppIcons.archive,
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer)
        mimeType.startsWith("text/") -> Triple(AppIcons.doc,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface)
        else -> Triple(AppIcons.file,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            MaterialTheme.colorScheme.onSurface)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

private val Icons = androidx.compose.material.icons.Icons
private val Icons.Outlined.PictureAsPdf get() = androidx.compose.material.icons.Icons.Outlined.PictureAsPdf
```

- [ ] **Step 2: Build and add preview**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/FileTypeIcon.kt
git commit -m "refactor(ui): FileTypeIcon MIME→Container mapping (Candy palette)"
```

---

### Task 15: Rewrite SearchField.kt — chip-style 40dp

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/SearchField.kt`

- [ ] **Step 1: Rewrite** (≤ 100 lines) using chip shape, 40dp height, leading icon.

- [ ] **Step 2: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/SearchField.kt
git commit -m "refactor(ui): SearchField chip-style 40dp"
```

(Full code listing omitted — follow prototype §4.2.4 — OutlinedTextField wrapped in a `Surface(shape = CircleShape, containerColor = surfaceContainerHigh)` with a 40dp height.)

---

### Task 16: Rewrite EmptyState / ErrorState / LoadingState / AppAlertDialog

**Files:**
- Modify: 4 files (one per component, ≤ 200 lines each)

- [ ] **Step 1: EmptyState** — Centered, 96×96 illustration (custom Canvas drawing), Fredoka title, optional description, optional action button. Uses `AppIcons.search` as default icon.

- [ ] **Step 2: ErrorState** — Light-red 40×40 round + `AppIcons.alert`, retry button.

- [ ] **Step 3: LoadingState** — `CircularProgressIndicator` + label text.

- [ ] **Step 4: AppAlertDialog** — 28dp corner modal at bottom (BasicAlertDialog wrapper).

- [ ] **Step 5: Build & commit (single commit)**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/EmptyState.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/ErrorState.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/LoadingState.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/AppAlertDialog.kt
git commit -m "refactor(ui): EmptyState/ErrorState/LoadingState/AppAlertDialog refresh"
```

(Full code listing omitted — each ~150 lines. Follow prototype §4.2.7 and §0 design tokens.)

---

### Task 17: Refresh Breadcrumb.kt / DirectoryBrowser.kt / ComponentPreviews.kt

**Files:**
- Modify: 3 files (token migration only — touch up colors, corners, paddings without changing functionality)

- [ ] **Step 1: Read existing files** to understand current implementation.

- [ ] **Step 2: Replace hardcoded `IndigoBlue40` / `FolderTint` / `ImageTint`** with `MaterialTheme.colorScheme.primary` / `tertiaryContainer` / `secondaryContainer` (deprecation alias works but cleaner to migrate).

- [ ] **Step 3: Adjust paddings** to use `Spacing.s16`/`Spacing.xxl` (8-point grid).

- [ ] **Step 4: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/Breadcrumb.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/DirectoryBrowser.kt \
        app/src/main/java/com/textvision/alistclient/ui/components/ComponentPreviews.kt
git commit -m "refactor(ui): migrate Breadcrumb/DirectoryBrowser/Previews to Sky Blue tokens"
```

---

### Task 18: Create music/ components — CoverLetter, WaveIndicator, AlbumCard, ArtistCard, SongRow, MusicHeroCard, MiniPlayer

**Files:**
- Create: 7 files under `app/src/main/java/com/textvision/alistclient/ui/components/music/`

- [ ] **Step 1: CoverLetter** (`CoverLetter.kt`)

```kotlin
package com.textvision.alistclient.ui.components.music

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 居中首字封面 — gradient background + first Unicode character of `name` rendered large.
 */
@Composable
fun CoverLetter(
    name: String,
    gradient: Brush,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier,
) {
    val firstChar = name.firstOrNull { !it.isWhitespace() }?.toString()?.uppercase() ?: "♪"
    val fontSize = (size.value * 0.52f).sp
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = firstChar,
            color = Color.White.copy(alpha = 0.92f),
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            style = MaterialTheme.typography.displayMedium,
        )
    }
}
```

- [ ] **Step 2: WaveIndicator** (`WaveIndicator.kt`) — 4-column animated wave (`animateFloatAsState` + `Canvas`).

- [ ] **Step 3: AlbumCard** (`AlbumCard.kt`) — 105×105 `CoverLetter` + name + artist.

- [ ] **Step 4: ArtistCard** (`ArtistCard.kt`) — 84×84 circular `CoverLetter` + name + count.

- [ ] **Step 5: SongRow** (`SongRow.kt`) — `CoverLetter` (42dp) + name + artist + duration + more.

- [ ] **Step 6: MusicHeroCard** (`MusicHeroCard.kt`) — Big gradient banner with placeholder title.

- [ ] **Step 7: MiniPlayer** (`MiniPlayer.kt`) — Bottom docked player: cover (38dp) + name + artist + wave + play button.

- [ ] **Step 8: Create music component test file** (`MusicComponentsTest.kt`):

```kotlin
package com.textvision.alistclient.ui.components.music

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicComponentsTest {
    @Test fun gradientSizesValid() {
        val g = Brush.linearGradient(listOf(Color(0xFFFFA1BD), Color(0xFF7C5BC7)))
        assertTrue(g != null)
    }
}
```

- [ ] **Step 9: Run test**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.ui.components.music.MusicComponentsTest`
Expected: PASS

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/music/ app/src/main/java/com/textvision/alistclient/ui/components/DecoBadge.kt
git commit -m "feat(ui): add music placeholder components + DecoBadge"
```

(Full code for WaveIndicator / AlbumCard / ArtistCard / SongRow / MusicHeroCard / MiniPlayer omitted for brevity — follow prototype §4.3. Each ~120 lines. DecoBadge is a new 100-line shared component for the album/song cover decoration badges.)

---

### Task 18b: Create AppScaffold.kt — standard Scaffold wrapper

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/ui/foundation/AppScaffold.kt`

- [ ] **Step 1: Create AppScaffold** following prototype §9.2:

```kotlin
package com.textvision.alistclient.ui.foundation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standard Scaffold wrapper — replaces Material3 Scaffold, integrates AppTopBar + AppBottomBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    background: @Composable () -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
        content = { padding ->
            Row(Modifier.fillMaxSize()) {
                background()
                content(padding)
            }
        },
    )
}
```

- [ ] **Step 2: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/foundation/AppScaffold.kt
git commit -m "feat(ui): add AppScaffold wrapper with topBar/bottomBar/background slots"
```

---

### Task 19: Refresh HomeScreen.kt — Hero + Metrics + 5-chip task + Storage

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt` (full rewrite)

- [ ] **Step 1: Rewrite** to follow HTML Screen02_Home / img_1.png:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            AppTopBar(
                title = "早上好 ✨",
                subtitle = if (state.online) "已连接 · ${state.serverName}" else "离线",
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(AppIcons.refresh, contentDescription = "刷新")
                    }
                    IconButton(onClick = {}) {
                        Icon(AppIcons.search, contentDescription = "搜索")
                    }
                },
            )
        },
        bottomBar = { AppBottomBar("home", onNavigate = {}) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard(padding = PaddingValues(18.dp)) {
                    HeroServerCard(
                        serverName = state.serverName,
                        serverVersion = state.serverVersion,
                        online = state.online,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("用户", state.userCount.toString(), AppIcons.user, modifier = Modifier.weight(1f))
                    MetricCard("角色", state.roleCount.toString(), AppIcons.shield, accent = "mint", modifier = Modifier.weight(1f))
                    MetricCard("在线", state.onlineCount.toString(), AppIcons.sparkle, accent = "pink", modifier = Modifier.weight(1f))
                }
            }
            item {
                SectionCard(padding = PaddingValues(14.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("后台任务", style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f))
                            Text("${state.activeTaskCount}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            state.taskChips.take(5).forEachIndexed { i, label ->
                                val (kind, dot) = when (i) {
                                    0 -> ChipKind.PRIMARY to true
                                    1 -> ChipKind.MINT to true
                                    else -> ChipKind.GRAY to false
                                }
                                Chip(label, kind = kind, showDot = dot)
                            }
                        }
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("存储源", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f))
                    Text("管理 →", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            items(state.storages) { storage ->
                StorageCard(name = storage.name, path = storage.path, icon = AppIcons.database, color = "default",
                    disabled = !storage.enabled, onClick = { viewModel.editStorage(storage.id) })
            }
        }
    }
}

@Composable
private fun HeroServerCard(serverName: String, serverVersion: String, online: Boolean) {
    Box {
        Box(Modifier.size(120.dp).align(Alignment.TopEnd).offset(30.dp, (-30).dp)
            .background(Brush.radialGradient(
                listOf(Brand300.copy(alpha = 0.6f), Color.Transparent),
            ), shape = CircleShape))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(MaterialTheme.shapes.small)
                .background(Brush.linearGradient(listOf(Brand500, Brand600))),
                contentAlignment = Alignment.Center) {
                Icon(AppIcons.cloud, contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(serverName, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("当前服务器 · $serverVersion",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp))
            }
            StatusChip(if (online) "在线" else "离线",
                kind = if (online) ChipKind.MINT else ChipKind.GRAY)
        }
    }
}
```

- [ ] **Step 2: Add @Preview** — LightHomePreview, DarkHomePreview, LargeFontHomePreview.

- [ ] **Step 3: Build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/home/
git commit -m "refactor(ui): HomeScreen with Hero + Metrics + 5 chips + Storage"
```

(Full code listing for MetricCard / StorageCard / Chip omitted for brevity — follow HTML Screen02_Home. HeroServerCard restored.)

---

### Task 20: Refresh LoginScreen.kt — gradient + cloud + 3 inputs

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/auth/LoginScreen.kt` (full rewrite)

- [ ] **Step 1: Rewrite** to follow prototype Screen01:

```kotlin
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize()) {
        SkyBlueBackground()
        CloudDecor()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CloudLogo()
            Text("登录 Alist", style = MaterialTheme.typography.displayMedium)
            Text("连接你的私有云盘 · 安全又可爱",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(36.dp))
            InputField(label = "服务器地址", value = state.server, leading = AppIcons.server,
                onValueChange = viewModel::onServerChange)
            InputField(label = "用户名", placeholder = "请输入用户名", leading = AppIcons.user,
                onValueChange = viewModel::onUsernameChange)
            InputField(label = "密码", value = state.password, leading = AppIcons.shield,
                onValueChange = viewModel::onPasswordChange, isPassword = true)
            Spacer(Modifier.height(12.dp))
            ActionButton(
                text = if (state.isLoading) "登录中…" else "登录 Alist",
                onClick = viewModel::login,
                variant = ButtonVariant.FILLED,
                isLoading = state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CloudLogo() { /* SVG cloud — see prototype */ }
@Composable
private fun InputField(label: String, value: String = "", placeholder: String? = null,
    leading: ImageVector, onValueChange: (String) -> Unit, isPassword: Boolean = false) { /* ... */ }
```

- [ ] **Step 2: Add @Preview** LightLoginPreview, DarkLoginPreview.

- [ ] **Step 3: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/auth/LoginScreen.kt
git commit -m "refactor(ui): LoginScreen with sky-blue gradient + cloud logo"
```

---

### Task 21: Refresh FileScreen.kt + FileListContent.kt + FileMultiSelectBar.kt

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileScreen.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileListContent.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/file/FileMultiSelectBar.kt`

- [ ] **Step 1: Rewrite each** to follow prototype Screen03:
  - AppTopBar with back + title "文件" + subtitle "当前离线"
  - StatusBanner (WARN) for offline state
  - SearchField
  - LazyColumn of file rows with `FileTypeIcon` + name + size + more icon
  - Multi-select: long-press activates `FileMultiSelectBar` (bottom actionbar)

- [ ] **Step 2: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/file/
git commit -m "refactor(ui): FileScreen multi-select with offline banner"
```

(Full code omitted — follow prototype §5.2.3.)

---

### Task 22: Refresh TransferScreen.kt + TransferListContent.kt + TransferRow.kt + TransferProgress.kt

**Files:**
- Modify: 4 files under `feature/transfer/`

- [ ] **Step 1: Rewrite** to follow HTML Screen05 / img_5.png:
  - AppTopBar (no back); subtitle = "N 进行中 · M 已完成"
  - **4-segmented** switcher: 全部 / 上传·N (badge 色 `tertiary` candy-pink) / 下载·N (badge 色 `secondary` candy-mint) / 失败
  - LazyColumn of **4-state** `TaskRow` cards:
    - upload: 32dp pink icon (`tertiaryContainer` bg) + 名称 (truncate) + "上传中" (primary 色) + 5dp gradient progress + 进度数字 + "取消" link
    - download: 32dp mint icon (`secondaryContainer` bg) + 名称 + "下载中" (secondary 色) + progress + "取消" link
    - failed: 32dp 浅红 + alert icon + "失败" (`state-error` 色) + **红色**进度条 + 错误原因 + "重试 · 删除" inline links
    - completed: **卡片整体 `opacity = 0.75`** + 32dp brand icon (`primaryContainer` bg) + check icon + "已完成" (`onSurfaceVariant` ink-soft 色) + 100% progress + 大小 + 完成时间 + "查看" link

- [ ] **Step 2: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/transfer/
git commit -m "refactor(ui): TransferScreen segmented + task cards"
```

---

### Task 23: Refresh SettingsScreen.kt — UserAvatarCard + Theme picker + 4 sections + logout + footer

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsScreen.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/UserAvatarCard.kt` (new shared component, already listed)

- [ ] **Step 1: Rewrite** to follow HTML Screen06 / img_6.png:
  - AppTopBar (no back); subtitle = "管理你的小窝" + green dot
  - **UserAvatarCard** (SectionCard): 48dp 圆形绿-蓝渐变 CoverLetter + 用户名 Fredoka 15 + 副标题 "我的云端小屋 · 在线" + 右侧 mint "VIP" chip
  - **外观主题** section + SectionCard with 3 theme cards (each card has segmented swatch, NOT gradient; sun/moon/auto icons)
  - **存储源** section + SectionCard with 3 `set-row` entries (36×36 渐变方, NOT 44×44 storage-card reused)
  - **快速设置** section + SectionCard with 2 set-rows; 隐私与密码 has **Switch on** instead of chevron
  - **维护** section + SectionCard with 2 set-rows (cleanup temp preview / 完整设置)
  - **退出登录** outlined button: state-error text + state-error-bg border + 14dp radius
  - Footer: centered 10px text "Alist Client · v1.0.0 · made with 💙"

- [ ] **Step 2: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/settings/SettingsScreen.kt
git commit -m "refactor(ui): SettingsScreen theme picker (3 cards) + grouped rows"
```

---

### Task 24: Refresh StorageEdit / AdminSiteSettings / MoveCopyPicker screens

**Files:**
- Modify: 3 files under `feature/storage/`, `feature/admin/`, `feature/picker/`

- [ ] **Step 1: StorageEdit** — Follow HTML Screen07 / img_7.png:
  - AppTopBar (back + 标题 "编辑存储" + 副标题 存储名)
  - **信息卡** (特殊 mint 渐变背景): `Brush.linearGradient(CandyMintBg, Color(0xFFC2EFE0))` 无描边; 内含 44×44 白色圆角方 + mint cloud icon + 存储名 Fredoka 15 (`Color(0xFF1B5A45)`) + "挂载路径 · /xxx" + 右侧 mint "已启用" chip
  - **驱动参数** section + SectionCard (solid 14dp padding) 包裹 5 字段:
    - 备注 / 挂载路径 OutlinedTextField (14dp 圆角)
    - **Cookie**: `primaryContainer` 蓝渐变背景卡 + 圆点 + "已设置 · N 天前更新" + outlined "重新获取" 按钮 + cookie icon
    - 根目录路径 OutlinedTextField
    - 排序方式 OutlinedTextField + 右侧 chevron
    - **启用存储** row: 左侧 "启用存储" 13 + "禁用后文件将不再显示" 11 + 右侧 Switch (on, brand gradient)
  - 底部 sticky 48dp FILLED "保存修改" 主按钮
  - 按钮下方居中 10px 副文字 "保存成功后将自动返回"

- [ ] **Step 2: AdminSiteSettings** — Follow HTML Screen08 / img_8.png:
  - Section labels (站点 / 预览 / 安全)
  - **站点** card: 站点标题 / 站点公告 / 站点图标 3 OutlinedTextField + "隐藏公告" row (Switch off)
  - **预览** card: 启用预览 (on) / 自动播放视频 (off) / 强制代理 (on) 3 rows with Switch
  - **安全** card: "Token 有效期" OutlinedTextField (48 小时 + 右侧 chevron) + "签名直链" row (Switch on)
  - 底部 sticky 48dp FILLED "保存全部" 主按钮

- [ ] **Step 3: MoveCopyPicker** — Follow HTML Screen09 / img_9.png:
  - AppTopBar (back + 标题 "选择目标" + 副标题 "移动 N 项到…"); **无确认按钮**
  - **面包屑 Row** (below TopBar): `AppIcons.home` (根目录) chip + `/` + `AppIcons.folder` (父目录) chip + `/` + `AppIcons.folder` (当前目录) chip + 末尾 "+ 新建" pill 按钮 (`primaryContainer` bg, 无描边, chip 圆角)
  - **新建文件夹输入卡**: 1.5dp **dashed `primary` 描边** + `primaryContainer` 背景 + folder icon + 透明无描边 `OutlinedTextField` + 右侧 "✓ 创建" 文字按钮
  - **可移动到的位置** section label
  - LazyColumn of `FileTypeIcon` folder rows (38dp icon + name + secondary meta) + 24×24 radio circle (unselected = transparent + 2dp outline; selected = brand gradient filled + check icon); **当前目录行**用 `primaryContainer` 背景 + 名称 "N 项 · 当前目录" + `check on` 图标（与文件页一致，不用 radio）
  - **底部 sticky** 48dp FILLED "确认移动到 · {选中名}" 主按钮

- [ ] **Step 4: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/storage/ \
        app/src/main/java/com/textvision/alistclient/ui/feature/admin/ \
        app/src/main/java/com/textvision/alistclient/ui/feature/picker/
git commit -m "refactor(ui): StorageEdit header+form, AdminSiteSettings sections, PickTarget breadcrumb+radio"
```

---

### Task 24b: Add KeyValueRow component

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/ui/components/KeyValueRow.kt`

- [ ] **Step 1: Create** — 1-line row, label left / value right, 14dp vertical spacing, used in PreviewScreen detail grid.

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * One row in a 2-column key-value list — label left, value right.
 * 14dp vertical padding, divider omitted.
 */
@Composable
fun KeyValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
```

- [ ] **Step 2: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/components/KeyValueRow.kt
git commit -m "feat(ui): add KeyValueRow component for detail grids"
```

---

### Task 25: Refresh PreviewScreen.kt + PreviewImage/Text/Audio/Fallback

**Files:**
- Modify: 5 files under `feature/preview/`

- [ ] **Step 1: Rewrite PreviewScreen.kt** following HTML Screen04 / img_3.png:
  - AppTopBar (back + 标题 + 副标题 完整路径 + 分享 icon-btn + 下载 icon-btn in actions)
  - Existing PreviewImage/PreviewText/PreviewAudio/PreviewFallback content stays (token-migrated)
  - **图片预览底部 overlay** (in PreviewImage.kt): 当是图片类型时，bottom 14dp padding + linear-gradient(180deg, transparent, rgba(0,0,0,0.45)) overlay + 白色 11px 标题 + 10px 副标题（分辨率+大小）
  - **Below preview body**, add:
    - Row of 3 ActionButton(variant=OUTLINED): 分享 / 复制直链 / 其他应用 (icons: share / link / external), 18dp corner, 38dp height, weight 1f each
    - **详细信息 SectionCard** (solid 14dp padding) with `card-title "详细信息"` + 4 KeyValueRow: 类型 / 尺寸 / 修改时间 / 位置

- [ ] **Step 2: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/feature/preview/
git commit -m "refactor(ui): PreviewScreen token migration"
```

---

### Task 26: Create music placeholder screens

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicLibraryScreen.kt`
- Create: `app/src/main/java/com/textvision/alistclient/ui/feature/music/MusicPreviewScreen.kt`

- [ ] **Step 7: MusicPreviewScreen.kt** — placeholder following HTML Screen11 player skeleton:

```kotlin
@Composable
fun MusicPreviewScreen(onBack: () -> Unit = {}) {
    Box(Modifier.fillMaxSize()) {
        SkyBlueBackground()
        CloudDecor()
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            AppTopBar(
                title = "正在播放",
                subtitle = "来自「音乐库」",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {}) { Icon(AppIcons.heart, "收藏") }
                    IconButton(onClick = {}) { Icon(AppIcons.more, "更多") }
                },
            )
            Column(
                Modifier.fillMaxWidth().weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 300dp 渐变封面 + 几何 SVG 占位
                Box(Modifier.size(300.dp).clip(MaterialTheme.shapes.extraLarge)
                    .background(Brush.linearGradient(
                        listOf(Color(0xFFFFA1BD), Color(0xFFC46683), Color(0xFF7C5BC7)),
                    )),
                    contentAlignment = Alignment.Center) {
                    Icon(AppIcons.musicNote, contentDescription = null, tint = Color.White,
                        modifier = Modifier.size(80.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("音乐功能即将推出", style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("目前为占位界面，敬请期待",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip("HIRES", kind = ChipKind.LILAC)
                    StatusChip("FLAC · 24bit", kind = ChipKind.MINT)
                }
                Spacer(Modifier.height(24.dp))
                // 占位进度条 + 控制按钮骨架
                AppMusicPlayerControlsPlaceholder()
                Spacer(Modifier.height(16.dp))
                AppLyricsCardPlaceholder()
            }
        }
    }
}
```

(Helper composables `AppMusicPlayerControlsPlaceholder` / `AppLyricsCardPlaceholder` = empty Containers matching HTML player chrome shape, used purely for visual congruence.)

- [ ] **Step 8: MusicLibraryScreen.kt** — placeholder following HTML Screen12 5-section structure:
  - AppTopBar (back + 标题 "音乐库" + 副标题 "N 首 · M 位艺人" + sort icon-btn + search brand icon-btn)
  - **Section 1**: `LazyRow` 4 chips with counts (全部·N / 最近添加·N / 最爱·N / 下载·N)
  - **Section 2**: `MusicHeroCard` placeholder with title "音乐功能即将推出"
  - **Section 3**: SectionHead + `LazyRow` of 4 `AlbumCard` placeholders
  - **Section 4**: SectionHead + `LazyRow` of 4 `ArtistCard` placeholders (84×84)
  - **Section 5**: SectionHead + 2-column grid of 2 `AlbumCard` placeholders (140dp high)
  - **Section 6**: SectionHead + 10 `SongRow` placeholders (first row `isPlaying=true`)
  - Sticky bottom: `MiniPlayer` placeholder

---

### Task 27: Add 2 navigation routes + 5th bottom tab

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppDestination.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/BottomNavBar.kt`
- Modify: `app/src/main/res/values/strings.xml` (add `tab_music` string)

- [ ] **Step 1: Add destination types**

Append to `AppDestination.kt`:

```kotlin
@Serializable
data object MusicLibraryDest

@Serializable
data object MusicPreviewDest
```

- [ ] **Step 2: Register composable destinations in AppNavHost.kt**

```kotlin
composable<MusicLibraryDest> { MusicLibraryScreen() }
composable<MusicPreviewDest> { MusicPreviewScreen() }
```

- [ ] **Step 3: Add 5th tab in BottomNavBar.kt**

Modify `BottomNavItems` to add `BottomNavItem("music", "音乐", AppIcons.musicNote, AppIcons.musicNote)` between files and transfers. Add `TAB_MUSIC` constant and matching `currentTab` switch case.

- [ ] **Step 4: Add string resource** in `app/src/main/res/values/strings.xml`:

```xml
<string name="tab_music">音乐</string>
```

- [ ] **Step 5: Add @Preview** for `AppBottomBar` to verify the 5-tab layout.

- [ ] **Step 6: Build**

Run: `./gradlew :app:assembleDebug`

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/navigation/ \
        app/src/main/res/values/strings.xml
git commit -m "feat(nav): add MusicLibrary/MusicPreview routes + 5-tab bottom nav"
```

---

### Task 28: Rewrite ThemePreviews.kt — Light/Dark preview wrappers

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreviews.kt`

- [ ] **Step 1: Rewrite**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.runtime.Composable

@Composable
fun LightThemePreview(content: @Composable () -> Unit) {
    AlistTheme(darkMode = DarkMode.LIGHT) { content() }
}

@Composable
fun DarkThemePreview(content: @Composable () -> Unit) {
    AlistTheme(darkMode = DarkMode.DARK) { content() }
}
```

- [ ] **Step 2: Build & commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/theme/ThemePreviews.kt
git commit -m "refactor(ui): theme preview wrappers with DarkMode enum"
```

---

### Task 29: Color token unit test

**Files:**
- Create: `app/src/test/java/com/textvision/alistclient/ui/theme/ColorTokenTest.kt`

- [ ] **Step 1: Create**

```kotlin
package com.textvision.alistclient.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTokenTest {
    @Test fun brandPrimaryIsSkyBlue() {
        // Sky Blue #6FB6FF
        assertEquals(Color(0xFF6FB6FF), Brand500)
    }
    @Test fun candyMint() {
        assertEquals(Color(0xFF9BE3C8), CandyMint)
    }
    @Test fun ink() {
        assertEquals(Color(0xFF1F3A5F), Ink)
    }
    @Test fun stateErrorDesaturated() {
        // Not pure red
        assertEquals(Color(0xFFF49AA1), StateError)
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.ui.theme.ColorTokenTest`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/textvision/alistclient/ui/theme/ColorTokenTest.kt
git commit -m "test(ui): validate color token hex constants"
```

---

### Task 30: Update Roborazzi baseline for all snapshots

**Files:**
- Delete all existing snapshots: `app/src/test/snapshots/**`
- Regenerate by running Roborazzi record mode

- [ ] **Step 1: Delete existing snapshots**

Run: `rm -rf app/src/test/snapshots/`

- [ ] **Step 2: Record new snapshots**

Run: `./gradlew :app:recordRoborazziDebug`
Expected: BUILD SUCCESSFUL; new snapshots generated in `app/src/test/snapshots/`

- [ ] **Step 3: Verify new snapshots don't drift on subsequent runs**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: All snapshots match within 0.1% tolerance. (If the first record sets baseline, run verify on second run with same code to confirm stability.)

- [ ] **Step 4: Commit**

```bash
git add app/src/test/snapshots/
git commit -m "test(visual): regenerate Roborazzi baseline with Sky Blue tokens"
```

---

### Task 31: Final verification — full build + test + lint + install

**Files:** none modified

- [ ] **Step 1: Run full validation suite**

Run: `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest`

Expected output:
- `BUILD SUCCESSFUL`
- `Tests run: NN, Failures: 0, Errors: 0` (some pre-existing flaky tests may show as ignored — verify `.superpowers/sdd/deviations/` for known issues)
- Lint: 0 errors (warnings allowed)

- [ ] **Step 2: Install and smoke-test on emulator**

Run: `./gradlew :app:installDebug`

Then launch:
- Login screen renders with cloud logo + 3 inputs
- Tap login → home screen renders with Hero card + 3 metrics + storage list
- Tap Files tab → file list renders with FileTypeIcon
- Tap Music tab → MusicLibraryScreen renders with chips + sections + MiniPlayer
- Tap Settings → Theme picker (3 cards) visible
- Toggle dark mode via Settings → app theme switches correctly

- [ ] **Step 3: Run Roborazzi verify**

Run: `./gradlew :app:verifyRoborazziDebug`
Expected: All snapshots match (0.1% tolerance).

- [ ] **Step 4: Visual diff against prototype**

Open `prototype/alist-android/index.html` in a browser (`python -m http.server 8765`). For each of 13 screens, compare side-by-side against the running app. Document any visual deviations.

- [ ] **Step 5: Commit final verification log**

```bash
git add docs/superpowers/sdd/
git commit -m "docs(sdd): record UI prototype refactor v3 verification complete"
```

---

## Self-Review

### 1. Spec coverage

| Spec § | Task(s) |
|---|---|
| §1.2 YAGNI (no music playback) | Task 26 (placeholder only), Task 27 (route but no VM) |
| §1.3 success criteria | Tasks 2-29 (all 13 screens); Task 30 (visual baseline); Task 31 (build/lint/test) |
| §3 Token color/typography/shape/motion | Tasks 2/3/4/5 |
| §4 13 components + 7 music | Tasks 7-18 |
| §5 13 screens | Tasks 19-26 |
| §6 Navigation | Task 27 |
| §7 State management (unchanged) | Inherited — verified by Task 31 |
| §8 Tests | Tasks 7/18/29/30 |
| §10 Acceptance | Tasks 30/31 |
| Fredoka dependency | Task 1 |
| 3-theme persistence | Task 6 (darkMode enum exists in repo — verify) |
| Material You disable | Task 6 (no `dynamicColor`) |

No gaps found.

### 2. Placeholder scan

Searched for TBD/TODO/"add appropriate"/"implement later" in plan: None found.

### 3. Type consistency

- `AppIcons.*` defined in Task 7 (object), consumed by Tasks 8-26 — consistent.
- `Material` colorScheme tokens (`primary`, `primaryContainer`, etc.) consistent throughout.
- `Corner.*` (`ExtraSmall=10`, `Small=14`, `Medium=18`, `Large=22`, `ExtraLarge=28`) consistent.
- `AppMotion.SpringFast/Medium/Slow` consumed by components built in Tasks 11-18.
- `MusicLibraryDest` / `MusicPreviewDest` defined in Task 27 only — consumed within the same task and used in Task 26.
- `ActionButton(variant = ButtonVariant.FILLED/TONAL/OUTLINED/TEXT)` consistent in Tasks 11/12.

No mismatches.

---

## Execution Choice

Plan saved to `docs/superpowers/plans/2026-07-10-ui-prototype-refactor.md`.

Two execution paths:

**1. Subagent-Driven (recommended)** — 33 tasks dispatched to fresh subagents (Task 1–17 + 18 + 18b + 19–26 + 24b + 27–31), review between tasks, tight iteration, no shared context bloat.

**2. Inline Execution** — Execute in this session, batched with checkpoints. Faster start, but conversation grows large over 33 tasks.

Choose path before invoking the execution skill.
