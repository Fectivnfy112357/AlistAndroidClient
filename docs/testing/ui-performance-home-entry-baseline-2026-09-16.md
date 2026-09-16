# UI Performance Baseline - Home Entry Cold/Warm - 2026-09-16

## Scope

This baseline measures **first-switch-to-Home** scroll jank on the unoptimized-on-this-front build at commit `54cf138f51444aa5b4cb81048f587dc359015f4c` (parent of the workspace state at session start). Two scenarios are distinguished:

- **cold**: force-stop + start MainActivity + tap Home tab + 5×up-swipe + 5×down-swipe.
- **warm**: force-stop + start MainActivity + tap each tab in order (Files / Music / Transfers / Settings) + tap Home + 5×up-swipe + 5×down-swipe.

The split captures two distinct user-visible "首次切换到首页" paths: cold is the
process-cold path; warm matches the realistic path of "用户先在 Files/Music 流
转一圈后,再切回首页" and is what the user originally described.

## Device conditions

| Property | Value |
|---|---|
| ADB serial | `adb-e331270f-wMfuVU._adb-tls-connect._tcp` |
| Model | `25102RK69C` |
| Android | **17** (SDK 37) — note: baseline-07-15 was Android 16 |
| Resolution | 1200 × 2608 |
| Supported refresh | 60 / 90 / 120 Hz |
| Active render rate (gesture) | 120 Hz (`renderFrameRate 60.000004` idle, switches to 120.0 during touch sequences) |
| App version | `0.1.0-perf` (versionCode 2) |
| First install | 2026-09-15 17:14:11 |

> **Cross-baseline caveat**: the earlier baseline at `docs/testing/ui-performance-baseline-2026-07-15.md` was taken on Android 16 via Wi-Fi ADB at `192.168.0.109:43453`. This baseline runs on Android 17 via `adb-tls-connect._tcp`. Numbers are **not directly comparable** to the July baseline; the relative cold↔warm comparison on this commit is still valid and is what the user's report covers.

## Deterministic coordinates

| Interaction | Coordinate |
|---|---:|
| Home tab | `(132, 2460)` |
| Files tab | `(365, 2460)` |
| Music tab | `(600, 2460)` |
| Transfers tab | `(833, 2460)` |
| Settings tab | `(1065, 2460)` |
| Swipe up | `(600, 2050) → (600, 500)` |
| Swipe down | `(600, 500) → (600, 2050)` |

Same as baseline-07-15; swipes are 250 ms each, no delay between up and down.

## Three-round results

`dumpsys gfxinfo com.textvision.alistclient reset` is called **before** the scenario. All numbers below come from `dumpsys gfxinfo` after the scenario. Times in milliseconds.

### cold scenario

| Round | Frames | P50 | P90 | P95 | P99 | Slow UI | Deadline missed |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 171 | 13 | 89 | 109 | 150 | 16 | 16 |
| 2 | 161 | 15 | 81 | 113 | 250 | 17 | 18 |
| 3 | 165 | 18 | 81 | 109 | 150 | 16 | 17 |

Cold median (across 3 rounds): **P50 15 ms / P95 109 ms / deadline miss 17**.

### warm scenario

| Round | Frames | P50 | P90 | P95 | P99 | Slow UI | Deadline missed |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | 209 | 19 | 109 | 125 | 150 | 30 | 30 |
| 2 | 223 | 16 | 109 | 133 | 250 | 30 | 31 |
| 3 | 215 | 17 | 109 | 133 | 150 | 32 | 33 |

Warm median (across 3 rounds): **P50 17 ms / P95 133 ms / deadline miss 31**.

### Observation

- Warm is significantly worse than cold on every percentile and on deadline miss.
  The longer scenario window includes more frames; the additional ~30 deadline
  misses compared to cold are concentrated around the first Home tab click
  (Home view-model cold create + first LazyColumn measure + first network result
  arriving mid-frame).
- The cold / warm difference matches the user's report: the worst frames are
  not during cold process start, but during the in-app "首次切换到首页" path.
- Both scenarios include the cold process start, so cold cannot be attributed
  purely to "cold start" cost; both quantify the cost of the Home view-model
  creation in this session.

## Median baseline (used as the acceptance comparator)

| Scenario | P50 | P95 | Deadline miss |
|---|---:|---:|---:|
| home-cold-scroll | 15 ms | 109 ms | 17 |
| home-warm-scroll | 17 ms | 133 ms | 31 |

Acceptance for the post-fix measurement (Stage 5):

- Warm P95 must drop at least **15%** (133 → ≤ 113 ms) **or** the warm deadline
  miss median must reach ≤ 2.
- Cold P95 must drop at least **10%** (109 → ≤ 98 ms).

## Trace files

Six System Trace files (`atrace -t N -o file` sync mode) at
`build/perf/baseline/{cold,warm}-{1,2,3}.trace`. Categories enabled:

```
sched gfx view input freq idle res am
```

`workq` and other kernel-internal categories are excluded because they require
root. File sizes (raw trace text):

| File | Bytes | Window |
|---|---:|---|
| `cold-1.trace` | 76 591 060 | 12 s |
| `cold-2.trace` | 73 866 591 | 12 s |
| `cold-3.trace` | 91 961 448 | 12 s |
| `warm-1.trace` | 94 829 984 | 18 s |
| `warm-2.trace` | 135 483 862 | 18 s |
| `warm-3.trace` | 98 201 381 | 18 s |

Each file is a `TRACE:` text-format file, openable in Android Studio → Profiler →
System Trace (drag the .trace file in, or `tools/profiler/atrace-studio-loader`).

### What the traces contain by phase name (objective, no conclusions)

- Process spawn (cold only): `am_proc_start` for `com.textvision.alistclient`.
- Activity boot: `VRI-Splash Screen com.textvision.alistclient/...` then
  `VRI-MainActivity` first insertion.
- View system: full SurfaceFlinger / SurfaceView composition; first
  `BufferTX` on MainActivity VRI around `~1.0s` after app start (cold).
- RenderThread work: a long tail of `RenderThread` slices over the scenario
  window (≈40 k–45 k per cold round).
- Category `gfx` does include `DrawLayer` markers for our VRI; these are
  naturally inflated by the touch-stream period.

> The remainder of this document is descriptive only; the **root-cause
> analysis** is in `docs/superpowers/specs/2026-09-16-home-entry-jank-root-cause-design.md`
> (Stage 3). This baseline intentionally stops at the objective data; do not
> derive conclusions here.

## Reproduction

```bash
source tools/dev-env.sh

# gfxinfo measurement
./tools/perf/Measure-HomeScroll-ColdWarm.sh -Scenario cold -Rounds 3 -Phase baseline
./tools/perf/Measure-HomeScroll-ColdWarm.sh -Scenario warm -Rounds 3 -Phase baseline

# System Trace (one round at a time; takes ~30 s each)
for s in cold warm; do
  for r in 1 2 3; do
    ./tools/perf/Capture-SystemTrace.sh -Scenario $s -Round $r -Phase baseline
  done
done
```
