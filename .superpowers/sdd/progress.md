# Progress Ledger — UI Prototype Refactor v3

> Plan: `docs/superpowers/plans/2026-07-10-ui-prototype-refactor.md`
> Spec: `docs/superpowers/specs/2026-07-10-ui-prototype-refactor-design.md`
> Branch: `feature/ui-prototype-v3` (worktree at `.worktrees/feature-ui-prototype-v3/`)
> Base: `07ccafb` (chore: add .worktrees to gitignore)

## Global Constraints (binding for all tasks)
1. compileSdk/targetSdk 34, minSdk 26, JVM 17
2. **Single file ≤ 400 lines**
3. `collectAsStateWithLifecycle()` not `collectAsState()`
4. **Zero hardcoded colors/fonts/corners outside `ui/theme/`** — plan's task listings sometimes show `Color(0xFF...)` in component code; resolve by using `MaterialTheme.colorScheme.*` always (exceptions: music placeholder gradients pre-approved in spec §4.2, and Fredoka cert array literal hex strings in `font_certs.xml`)
5. PascalCase composables; camelCase helpers; sealed `XxxUiState` / `XxxIntent`
6. Each Screen ≥ 3 `@Preview` (Light/Dark/LargeFont)
7. Existing tests must still pass; Roborazzi 0.1% tolerance (regenerate baseline)
8. No `dynamicColor`; 3-way theme persisted
9. Music = placeholder only — NO playback deps (no ExoPlayer, no MediaSession)
10. All old `IndigoBlue*` / `Secondary40` / `Tertiary40` / `Neutral*` / `Error*` / `SurfaceContainer*` / `Outline*` / `FolderTint` / `ImageTint` / `AlistBlue` become `@Deprecated` aliases pointing at new Sky Blue tokens
11. Conventional Commits (`feat(ui):`, `refactor(ui):`, `test(ui):`, `docs(spec):`); one commit per task
12. `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` must pass after every task boundary
13. All work in worktree branch `feature/ui-prototype-v3`; do NOT commit to main

## Pre-Flight Plan Review

Conflicts found:
- **Plan constraint #4 vs Task listings**: Task 8 step 1 hardcodes `Color(0xFFD8E9FF)` and `Color(0xFFFFC4D6)` / `Color(0xFF9BE3C8)` directly inside `CloudDecor()` (`Backgrounds.kt`). This violates constraint #4 (zero hardcoded colors outside `ui/theme/`). **Resolution**: implementer should either move those tints to `ui/theme/Color.kt` (preferred, e.g. add `CloudTint1`, `CloudTint2`, `CloudDecorPink`, `CloudDecorMint`) or use `MaterialTheme.colorScheme.surfaceVariant` / `tertiaryContainer` / `secondaryContainer` directly. Same applies to Task 18 step 1 `CoverLetter`/`MusicHeroCard` gradient stops — use `MaterialTheme.colorScheme.tertiaryContainer` / `secondaryContainer` / `primaryContainer` combinations.
- **Task 14 step 1 mentions `Icons.Outlined.PictureAsPdf` but the file's `private val Icons = ...` shadow at line 1634 is malformed Kotlin** (a property cannot be defined as `private val X.Y.Z`). **Resolution**: implementer should use the standard `androidx.compose.material.icons.outlined.PictureAsPdf` import directly.
- **Task 18b `AppScaffold`** signature's content lambda receives `PaddingValues` but Task 19 etc. may not pass them. Forward-compatible is fine since callers can ignore.

No other conflicts. Proceeding.

## Tasks

### Task 1: Add Google Fonts dependency (Fredoka support)
Task 1: complete (commits 07ccafb..86ce3f8, review APPROVED, 0 critical, 1 important: report mentions local.properties but diff is clean — documentation artifact not a defect)
Task 2: complete (commits 86ce3f8..236c443, review APPROVED, 0 critical/0 important; 2 deviations (D1): renamed `LightColors`/`DarkColors` → `LightScheme`/`DarkScheme` to avoid same-package top-level collision with `Theme.kt:14,46` (private vals); (D2): added deprecated `object LightColorScheme`/`DarkColorScheme` with 60 forwarding get() properties so Theme.kt keeps compiling unchanged; deviations justified — verified Theme.kt collision by reviewer; Task 6 cleanup scope; minor: brief's deprecation messages for AlistBlue/IndigoBlue20 are awkward wording verbatim from plan, not implementer's mistake)
Task 3: complete (commits 236c443..e2ef2ef, review APPROVED, 0 critical/0 important/0 minor; 1 deviation (brief omitted `import androidx.compose.ui.text.unit.em` — required for `letterSpacing = (-0.01).em` property — implementer correctly added); first attempt abandoned mid-flight (no files changed), redispatched with haiku and completed; Type.kt 137 lines, font_certs.xml 17 lines, build green)
Task 4: complete (commits e2ef2ef..6623ba9, review APPROVED, 0 critical/0 important/0 minor, no deviations; Shape.kt 25 lines, 7 Corner values (10/14/18/22/28/999.dp/CircleShape), 5 Shapes fields; implementer verified 0 `CloudShapes` callers so no alias needed)
Task 5: complete (commits 6623ba9..9b9b7b6, review APPROVED, 0 critical/0 important/0 minor; 2 justified deviations: CloudMotion @Deprecated alias for MotionTest.kt callers; retained internal DurationMediumMillis/FloatTween/OffsetTween for AppNavTransitions.kt; 103 lines, all 6 AppMotion tokens + isReducedMotion @Composable)
Task 6: complete (commits 9b9b7b6..87ef24d, review APPROVED, 0 critical/0 important/0 minor, 12 files +63/-214 (net -151 lines); 4 deviations all justified: (1) `LightScheme`/`DarkScheme` from Task 2 carry through; (2) 11 caller migrations AlistClientTheme→AlistTheme signature required (MainActivity + 4 previews + 4 test/snapshot + ThemeRepository); (3) ThemeRepository duplicate enum DarkMode deleted (single source in Theme.kt); (4) theme_dynamic_light dead test deleted; deprecated object LightColorScheme/DarkColorScheme + 60 forwarding properties deleted from Color.kt)
Task 7: complete (commits 87ef24d..71a1cea, review APPROVED, 0 critical/0 important/0 minor; 95 lines, 56 ImageVector fields across 8 categories; 1 deviation: ArrowBack uses Filled not Outlined per brief — matches AppBars.kt:45 existing pattern, not a real spec violation; 4 deprecated icon warnings non-blocking; `decoNote` empty ImageVector.Builder is intentional placeholder per brief — follow-up needed for real SVG path data)
Task 8: complete (commits 71a1cea..d11bb04, review APPROVED, 0 critical/0 important, 3 minor: missing @Deprecated annotation on legacy BottomNavItem/items-overload aliases; KDoc line 203-205 misleading parenthetical; report line count drift 237 vs 258; 2 deviations: kept BottomNavItem/items-overload as legacy (Kotlin overload erasure prevents pure alias), renamed onNavigateUp→onBack at 4 call sites; 5-tab AppBottomBar unreachable until Task 27)
Task 9: complete (commits d11bb04..df42dc1, review APPROVED, 0 findings; 89 lines, 3 composables (SkyBlueBackground/CloudDecor/AppBackground); 3 hardcoded hex literals replaced with palette tokens (Brand200/CandyPink/CandyMint) per Pre-Flight Plan Review fix; AppBackground deprecation alias for Scaffold.kt)
Task 10: complete (commits df42dc1..3e1e375, review APPROVED, 0 critical/0 important, 1 minor: unused `import androidx.compose.ui.unit.Dp` — implementer added it not brief; 36 lines; uses surfaceContainer/shapes.large/4dp shadow all from theme tokens — fully compliant with zero-hardcoded rule; no naming collision)
Task 11: complete (commits 3e1e375..ed6d5e0, review APPROVED, 0 critical/0 important, 1 minor: FILLED disabled state lacks visual dimming (brief didn't specify, out of scope); 188 lines, 6 files; 2 brief-bug fixes: deleted malformed `private fun Modifier.background` + added foundation.background import; 5 caller migrations loading→isLoading (overload erasure forces it, precedent Task 8); no raw hex, uses Brand500/Brand600/onPrimary)