# UI Performance Optimization Report - 2026-07-15

## Scope

This report compares the unoptimized baseline at `f7b71ddfbc1e3600cd0d967535889f402fc7af36` with the final debug build after Tasks 1-6 and the Task 7 validation fixes. Every scenario used the fixed Wi-Fi ADB serial `192.168.0.109:43453`, the same deterministic input script, and three rounds. Values are medians; lower is better. Percentage delta is `(final - baseline) / baseline`, so a negative value is an improvement.

## Build and device

| Property | Value |
|---|---|
| Device | `25102RK69C`, Android 16, 1200 x 2608 |
| Required ADB serial | `192.168.0.109:43453` |
| Baseline commit | `f7b71ddfbc1e3600cd0d967535889f402fc7af36` |
| Optimization commits | `69025e3`, `b31a994`, `7aeef91`, `5e01223`, `00bc14a` |
| Measurement tooling commit | `9825270` |
| Final APK SHA-256 | `05B6FBC3EA122C8474BA7B2A0CA13CCDB0943A456C12A53AC2BFE85B33CBC618` |
| Baseline conditions | 71% / 29.5 C start, 69% / 32.2 C end |
| Final conditions | 65% / 31.7 C start, 64% / 32.1 C end |
| Refresh configuration | Peak refresh rate 120 Hz; supported 60 / 90 / 120 Hz |
| Final gesture evidence | 24/60 samples at 120.00001 Hz; 36/60 at 60.000004 Hz |

The final refresh capture includes startup and idle boundaries. It confirms that the device entered 120 Hz during the gesture, but the run was not continuously fixed at 120 Hz. No stronger 120 Hz claim is made.

## Median comparison

Each cell is `baseline -> final (delta)`.

| Scenario | P50 | P90 | P95 | P99 | Slow UI | Deadline missed |
|---|---:|---:|---:|---:|---:|---:|
| home-scroll | 8 -> 8 (0.0%) | 13 -> 14 (+7.7%) | 21 -> 23 (+9.5%) | 48 -> 53 (+10.4%) | 6 -> 6 (0.0%) | 7 -> 6 (-14.3%) |
| settings-scroll | 8 -> 8 (0.0%) | 17 -> 14 (-17.6%) | 22 -> 21 (-4.5%) | 53 -> 48 (-9.4%) | 7 -> 10 (+42.9%) | 7 -> 10 (+42.9%) |
| tab-cycle | 21 -> 23 (+9.5%) | 109 -> 109 (0.0%) | 129 -> 129 (0.0%) | 150 -> 150 (0.0%) | 37 -> 38 (+2.7%) | 37 -> 38 (+2.7%) |
| music-scroll | 6 -> 6 (0.0%) | 7 -> 7 (0.0%) | 8 -> 10 (+25.0%) | 53 -> 53 (0.0%) | 11 -> 11 (0.0%) | 12 -> 11 (-8.3%) |
| player-enter | 6 -> 6 (0.0%) | 53 -> 53 (0.0%) | 77 -> 85 (+10.4%) | 150 -> 150 (0.0%) | 8 -> 7 (-12.5%) | 8 -> 8 (0.0%) |
| player-playback | 6 -> 6 (0.0%) | 7 -> 7 (0.0%) | 12 -> 13 (+8.3%) | 73 -> 57 (-21.9%) | 7 -> 8 (+14.3%) | 9 -> 8 (-11.1%) |

## Result

The changes preserved central frame times across the music scenarios and produced useful targeted gains: settings P90/P95/P99 improved, playback P99 improved by 16 ms, and deadline misses decreased by one in home, music scroll, and playback. The outcome is mixed rather than a uniform performance win. Home tails, music/player P95, and several slow-frame counts regressed within the observed three-round variance.

Main-tab switching remains the dominant repeatable hotspot at P90/P95/P99 `109/129/150 ms` with 38 slow/deadline-missed frames. Player entry also retains a 150 ms P99 tail. These should be the next trace targets; the existing deterministic script does not separately automate album/artist player entry.

## Functional verification

The exact final command completed with `BUILD SUCCESSFUL`:

```powershell
./gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

`assembleDebug`, Android Lint, and all 258 unit tests passed in the same invocation. The same APK was installed before warm-up and measurement. Warm-up covered all five main tabs, home/music scrolling, Songs, player entry, and play/pause. The stale prototype source assertion was updated to the current `HomeGreeting` contract. Two async artwork loaders were expressed as `remember(key) + LaunchedEffect(key)` to avoid a Compose Lint false positive while preserving key reset, IO decoding, and artwork downsampling behavior.

## Reproduction

```powershell
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario home-scroll -Rounds 3 -OutputDirectory build/perf/final
```

Repeat with `settings-scroll`, `tab-cycle`, `music-scroll`, `player-enter`, and `player-playback`. Raw final output is under ignored `build/perf/final`; baseline data and device coordinates are documented in `docs/testing/ui-performance-baseline-2026-07-15.md`.

The planned system Perfetto command remains unavailable because `/data/misc/perfetto-configs/trace_config.pbtxt` does not exist on this device. Android Studio System Trace is the documented fallback; no trace hotspot is fabricated.
