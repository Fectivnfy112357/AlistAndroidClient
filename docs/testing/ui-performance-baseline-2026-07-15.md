# UI Performance Baseline - 2026-07-15

## Scope

This baseline measures the unoptimized build at commit `f7b71ddfbc1e3600cd0d967535889f402fc7af36` using `tools/perf/Measure-UiPerformance.ps1`. Every scenario was run three times after force-stopping and starting the app, with `gfxinfo` reset immediately before the deterministic interaction.

## Device conditions

| Property | Value |
|---|---|
| ADB serial | `192.168.0.109:43453` |
| Model | `25102RK69C` |
| Android | 16 |
| Resolution | 1200 x 2608 |
| Supported refresh | 60 / 90 / 120 Hz |
| Configured peak refresh | 120 Hz |
| Gesture-time evidence | 20/25 samples at 120.00001 Hz; 5/25 startup/idle samples at 60.000004 Hz |
| Start battery / temperature | 71% / 29.5 C |
| End battery / temperature | 69% / 32.2 C |

The gesture-time refresh evidence was sampled with `dumpsys display` about every 150 ms during a complete home-scroll run. The device switched to 120 Hz during repeated gestures and returned to 60 Hz around idle boundaries.

## Deterministic coordinates

| Interaction | Coordinate |
|---|---:|
| Home / Files / Music / Transfers / Settings | `(132/365/600/833/1065, 2460)` |
| Music Songs tab | `(450, 550)` |
| First song row (`Tik Tok`, 2PM) | `(600, 875)` |
| Player play/pause | `(600, 2080)` |

## Three-round results

Values below are raw `dumpsys gfxinfo` results. Percentiles are milliseconds.

| Scenario | Round | Frames | P50 | P90 | P95 | P99 | Slow UI | Deadline missed |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| home-scroll | 1 | 880 | 8 | 13 | 18 | 53 | 6 | 7 |
| home-scroll | 2 | 908 | 7 | 13 | 21 | 48 | 0 | 0 |
| home-scroll | 3 | 888 | 8 | 14 | 21 | 46 | 8 | 8 |
| settings-scroll | 1 | 798 | 8 | 19 | 26 | 61 | 7 | 7 |
| settings-scroll | 2 | 820 | 8 | 14 | 21 | 46 | 7 | 7 |
| settings-scroll | 3 | 788 | 8 | 17 | 22 | 53 | 9 | 9 |
| tab-cycle | 1 | 242 | 21 | 105 | 129 | 150 | 37 | 37 |
| tab-cycle | 2 | 246 | 19 | 109 | 133 | 150 | 37 | 37 |
| tab-cycle | 3 | 208 | 23 | 109 | 121 | 133 | 43 | 43 |
| music-scroll | 1 | 918 | 6 | 7 | 8 | 53 | 12 | 12 |
| music-scroll | 2 | 894 | 6 | 7 | 8 | 57 | 10 | 12 |
| music-scroll | 3 | 904 | 6 | 7 | 9 | 48 | 11 | 11 |
| player-enter | 1 | 244 | 6 | 44 | 77 | 150 | 8 | 8 |
| player-enter | 2 | 204 | 6 | 57 | 77 | 150 | 9 | 9 |
| player-enter | 3 | 246 | 6 | 53 | 85 | 150 | 7 | 8 |
| player-playback | 1 | 1482 | 6 | 7 | 9 | 73 | 9 | 9 |
| player-playback | 2 | 1478 | 6 | 7 | 12 | 57 | 7 | 9 |
| player-playback | 3 | 1400 | 6 | 7 | 13 | 77 | 6 | 8 |

## Median baseline

| Scenario | P50 | P90 | P95 | P99 | Slow UI | Deadline missed |
|---|---:|---:|---:|---:|---:|---:|
| home-scroll | 8 | 13 | 21 | 48 | 6 | 7 |
| settings-scroll | 8 | 17 | 22 | 53 | 7 | 7 |
| tab-cycle | 21 | 109 | 129 | 150 | 37 | 37 |
| music-scroll | 6 | 7 | 8 | 53 | 11 | 12 |
| player-enter | 6 | 53 | 77 | 150 | 8 | 8 |
| player-playback | 6 | 7 | 12 | 73 | 7 | 9 |

The dominant repeatable hotspots are main-tab switching and player entry. Scroll and playback scenarios have acceptable central percentiles but substantial P99 tails and repeated missed deadlines.

## Trace status

The plan's system Perfetto command was attempted, but `/data/misc/perfetto-configs/trace_config.pbtxt` does not exist on this device. Perfetto exited with errno 2. No trace hotspot is claimed. Android Studio System Trace remains the documented fallback for a manual capture.

## Reproduction

```powershell
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario home-scroll -Rounds 3 -OutputDirectory build/perf/baseline
```

Replace `home-scroll` with each of `settings-scroll`, `tab-cycle`, `music-scroll`, `player-enter`, and `player-playback`.
