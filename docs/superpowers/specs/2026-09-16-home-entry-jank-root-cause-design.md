# 首页首次切换 + 滚动帧抖动 根因分析 - 2026-09-16

## Status

Implementation of the **Plan / Task Contract** for "首页首次切换 + 滚动帧抖动"
(see `docs/superpowers/plans/...` and the task contract in this conversation).
The plan mandated **trace evidence before any code change**; this document
records what the System Trace (`atrace`) actually shows and what we can and
cannot conclude from it.

## Evidence: top frame gaps in `warm-2.trace`

`tools/perf/Capture-SystemTrace.sh` writes one System Trace per round.
`warm-2.trace` is the median warm round (P95=133 ms, 31 deadline misses).

The five longest gaps between consecutive `Choreographer#doFrame` B events on
TGID 28925 (our app):

| Rank | Gap | Marker pairs that bracket it | Interpretation |
|---:|---:|---|---|
| 1 | 741 ms | `MSG_DISPATCH_APP_VISIBILITY` → next doFrame | cold-start activity resume (excluded from "首次切换到首页" path) |
| 2 | 366 ms | `dispatchInputEvent MotionEvent DOWN` → next doFrame | **first swipe DOWN** on the freshly shown Home dashboard |
| 3 | 361 ms | `ResourcesManager#applyConfigurationToResources` → next doFrame | second cold-start fragment |
| 4 | 283 ms | `MotionEvent DOWN` → next doFrame | second DOWN |
| 5 | 253 ms | `MotionEvent DOWN` UP` → next doFrame | a short tap |

Four of the top five gaps are touch events on the Home dashboard; the first
frame after a tap on Home is the actual offender. `cold-2.trace` (median cold
round, P95=113 ms, 18 deadline misses) shows the same pattern: the longest
gaps after the very first activity-resume frame are all bracketed by
`MotionEvent DOWN` on the Home dashboard.

`RenderThread` activity during these gaps is normal (B/E pairs at the expected
~8.3 ms grid once vsync is re-acquired); **GPU is not the bottleneck**. The
stall is on the app's main thread.

## What runs inside a 366 ms main-thread gap

`warm-2.trace` between ts `586882.430` (`dispatchInputEvent DOWN`) and
`586882.795` (next doFrame) carries **17439 events** keyed to TGID 28925.
Tally by top-level marker token:

| Count | Token | what it implies |
|---:|---|---|
| 262 | `FillRectOp` | rendering the dashboard's layered rectangles |
| 80  | `AtlasTextOp`  | drawing text glyphs (Fredoka numerals, labels, server name) |
| 70  | `CircularRRectOp` | rounded card backgrounds and icons |
| 49  | `Compose:recompose` | the Compose runtime invoked a recomposition |
| 35  | `TextLayout:initLayout` | a fresh text measurement (Fredoka rasterization path) |
| 16  | `Choreographer#doFrame` | sixteen vsync callbacks between the bracketing DOWNs |
| 16  | `Choreographer#scheduleVsyncLocked` | vsync resync |
| 1   | `RenderRate\|120`   | display entered 120 Hz refresh after the touch |

This is a normal profile of "Compose has been asked to invalidate several
nodes, recompose them, lay out their text, and emit GPU ops — all on the main
thread between two vsync callbacks." It is **not** a single runaway function;
it is the cumulative cost of a first-touch cascading invalidation.

## Why we cannot name a single root cause from this trace alone

System Trace (atrace) without Compose-instrumented markers cannot see inside
Compose's recomposition tree. The visible markers are limited to:

- The view system `InputDispatcher` chain (`AndroidOwner:onTouch`, etc.)
- The Choreographer dispatcher (`doFrame`, `scheduleVsyncLocked`)
- The RenderThread GPU ops
- A handful of platform markers (`Compose:recompose` / `Compose:applyChanges`,
  which are emitted by `ComposeView`/`AndroidComposeView` but **not** the
  function name that triggered them)

Without `androidx.compose.runtime.tooling.CompositionTracing` enabled in the
app build, we cannot tell which specific Composable function spent the 350 ms
on the main thread. Compose tracing is **out of scope** for this plan
(it requires modifying `app/build.gradle.kts` and rebuilding with `-PenableCompositionTracing=true`
and shipping dev-only mode).

The Plan's Stage 3 gate ("根因文档里有具体函数名 + trace 截图佐证") can
therefore only be partially satisfied. We state **what can be observed**
(below) and propose **a minimal fix whose cost/benefit is local and
reversible**.

## Observations that are still actionable

### O1 — `Modifier.shadow(elevation = 0.dp)` is **short-circuited** in Compose UI 1.7.x

**Correction (against the original draft):** `androidx.compose.ui.draw.shadow`
in Compose UI 1.7.x (BOM 2024.09, ui-android-1.7.3) short-circuits when both
`elevation == 0.dp` and `clip == false`:

```kotlin
public fun Modifier.shadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    clip: Boolean = elevation > 0.dp,
    ambientColor: Color = DefaultShadowColor,
    spotColor: Color = DefaultShadowColor,
): Modifier =
    if (elevation > 0.dp || clip) {
        this then ShadowGraphicsLayerElement(elevation, shape, clip, ambientColor, spotColor)
    } else {
        this
    }
```

Because our SectionCard call sites pass `shadowElevation = 0.dp` and do not
override `clip`, the default `clip = elevation > 0.dp = false` branch is
taken. **No `GraphicsLayer` is allocated. The original O1 hypothesis is
**incorrect**.** No Stage 4 change is needed or possible in this
direction. (Source:
https://raw.githubusercontent.com/androidx/androidx/androidx-main/compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/draw/Shadow.kt)

### O2 — `SectionAccentTitle` re-allocates its `Brush.verticalGradient` each recomposition

`app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeStorageSection.kt:71`

```kotlin
.background(Brush.verticalGradient(listOf(Brand500, Color(0xFF9BE3C8))))
```

Already noted in the Plan as a known leak. This is **off the scroll hot path**
(it sits at the top of dashboard), but it is the only allocation
opportunity that is unambiguously traceable to a specific function name and
fix-able in one line. As a control change, it is the lowest risk pre-fix
candidate.

### O3 — `LaunchedEffect(Unit) { viewModel.loadIfNeeded() }` may re-fire on restored composition

`app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt:55`

Bottom-tab `saveState = true; restoreState = true` keeps the Home
composition's state across switches. `LaunchedEffect(Unit)` does **not**
re-fire while the composition persists, but it **does** re-fire if the
composition is disposed and re-created (e.g., process restart, or save/restore
via `popUpTo(login)` clearing). The guard `if (hasLoadedInitial) return` in
`HomeViewModel.loadIfNeeded` already prevents the network cost; this
observation is recorded but no behavior change is proposed in this plan.

## Decision: which observation drives the Stage 4 change

Per the Plan rule "每次只改 1 个变量" — exactly **one** of the above should
become a code change in Stage 4, and only if the post-fix trace (Stage 5)
shows improvement that can be causally attributed to that change.

We pick **O2** (the Brush leak in `SectionAccentTitle`) as the **single
safe Stage 4 change**:

| Aspect | Justification |
|---|---|
| Specific function name in our code | yes — `SectionAccentTitle` in `HomeStorageSection.kt:71` |
| Trace evidence available | partial — only allocates inside the dashboard body, not on scroll; improvement will be indirect (pre-touch phase) |
| Reversibility | one-line cache; safe to revert via git |
| Risk budget | none — visible layout does not change |
| Scope fit | within Plan's `ui/feature/home/` allowance |

The other two observations are recorded for follow-up tickets but are **not
acted on in this plan**:

- O1 (`Modifier.shadow(0.dp)` skip) is **invalidated by source verification** —
  Compose UI 1.7.x already short-circuits this. No code change applies here.
- O3 (LaunchedEffect) is deferred because (a) it is not the dominant term in
  the trace and (b) it has a behavioral implication (loadIfNeeded semantics)
  that should not be modified without the user's explicit choice, which is
  outside this Plan's Boundary Guard.

## Acceptance for Stage 5 (post-fix measurement)

Same scripts, same device, same `Phase baseline → after` paths:

- Warm P95 must drop at least **15%** (133 → ≤ 113 ms) **or** warm deadline
  miss median must reach ≤ 2.
- Cold P95 must drop at least **10%** (109 → ≤ 98 ms).
- The post-fix `warm-{2,3}.trace` must show **fewer** `FillRectOp`,
  `CircularRRectOp`, and `TextLayout:initLayout` markers bracketed by the
  longest gap (i.e., the same metric, smaller count).

If both thresholds are missed, Stage 4 is rolled back and we re-open with a
Compose-instrumented trace.

## Risks and unresolved points

1. atrace without `Compose:recomposition` markers **cannot** distinguish the
   several candidates inside the 366 ms window. O1 vs the deeper LazyColumn
   first-measure hypothesis is undecidable from the existing trace.
2. The OS-version drift (Android 17 vs the 2026-07-15 baseline's Android 16)
   means our P95 numbers are not directly comparable to the earlier baseline.
   Only the **relative** cold↔warm comparison is meaningful.
3. The user-reported "多滑几次恢复正常" symptom cannot be reproduced cleanly
   inside the bounded trace window — after the first 4-6 swipes the
   missed-deadline count drops to near zero even on the unoptimized build.
   So the fix must improve the first 1-3 swipes, not the entire scroll span.

These are recorded for the Stage 5 report and the eventual follow-up ticket.
