# UI Performance Optimization Worklog

Last updated: 2026-07-15 Asia/Shanghai

This is the durable handoff log for executing `docs/superpowers/plans/2026-07-15-ui-performance-optimization.md`. Update this file immediately after every impact analysis, test run, device measurement, implementation step, or discovered deviation.

## Execution contract

- User explicitly requested `superpowers:executing-plans` and strict Task 1 through Task 7 execution.
- User explicitly approved working directly on `main`; do not create a worktree.
- User explicitly approved continuing past known/pre-existing failures without asking again.
- Still mandatory: before modifying any symbol, run GitNexus upstream impact and report direct callers/processes/risk. If risk is HIGH or CRITICAL, notify the user before editing.
- Before final report/commit, run GitNexus `detect_changes` against `main` and full Gradle validation.
- Preserve unrelated user changes and untracked files.
- Conversation language: Chinese.

## Repository state at start

- Repository: `D:\programming\projects\my project\alist`
- Branch: `main`
- Starting HEAD: `f7b71ddfbc1e3600cd0d967535889f402fc7af36`
- Starting commit: `f7b71dd docs(perf): 添加 UI 性能优化实施计划`
- Pre-existing user changes: modified `AGENTS.md`, modified `CLAUDE.md`.
- Pre-existing untracked files observed initially: `alist-perf-v2.apk`, `alist-perf-v3.apk`, `alist-perf-v4.apk`, `scr.png`.
- Do not modify, stage, delete, or revert those unrelated files.

## Skills and instructions loaded

- Read full `CLAUDE.md`.
- Read full implementation plan.
- Read `superpowers:using-superpowers`, `superpowers:executing-plans`, `superpowers:using-git-worktrees`, and GitNexus impact-analysis skill.
- User declined worktree and authorized direct `main` work.
- No subagents are being used because the user explicitly selected executing-plans.

## Initial validation

Task 1 Step 1 RED contract check was run before creating the script and failed as expected:

```text
missing tools/perf/Measure-UiPerformance.ps1
```

Initial unit baseline command:

```powershell
./gradlew.bat :app:testDebugUnitTest
```

Result: failed with 252 tests completed, 1 failure:

```text
PrototypeScreenSourceTest > homeScreenKeepsPrototypeHeaderGradient FAILED
java.lang.AssertionError at PrototypeScreenSourceTest.kt:21
```

This failure existed before task production changes. User explicitly approved recording it and continuing.

## Acceptance device

- Required serial: `192.168.0.109:43453`
- Connection: online through Wi-Fi ADB.
- Model: `25102RK69C`
- Android: 16
- Physical resolution: 1200 x 2608
- Supported refresh rates: 60, 90, 120 Hz
- `settings get system peak_refresh_rate`: 120
- Initial idle display evidence: active mode 3 / active render frame rate about 60 Hz.
- Initial battery: level 71%, temperature 295 (29.5 C), status 3.
- Important: do not claim 120 Hz acceptance until active refresh evidence is captured during gestures.
- A second mDNS ADB identity for the same device was visible; always use the exact required serial above.

## Task progress

- Task 1: IN PROGRESS.
- Tasks 2-7: NOT STARTED.
- No GitNexus impact is required for Task 1 because it only creates a script/document and modifies no code symbol.
- No commits have been created yet.

## Task 1 files and artifacts

Created and currently untracked:

- `tools/perf/Measure-UiPerformance.ps1`

The script currently:

- accepts the six planned scenarios;
- pins every ADB command to the supplied serial;
- force-stops and starts the app before each measured round, then resets `gfxinfo` before the scenario;
- supports `home-scroll`, `settings-scroll`, `tab-cycle`, `music-scroll`, `player-enter`, and `player-playback`;
- writes raw `gfxinfo` output to the requested output directory.

Ignored diagnostic artifacts currently under `build/perf/` include UI XML dumps/screenshots and the first home measurement. They are not source changes.

## Deterministic device coordinates

Coordinates were obtained from the live UIAutomator semantics tree on the required device:

| Action | Coordinate |
|---|---:|
| Home bottom tab | `(132, 2460)` |
| Files bottom tab | `(365, 2460)` |
| Music bottom tab | `(600, 2460)` |
| Transfers bottom tab | `(833, 2460)` |
| Settings bottom tab | `(1065, 2460)` |
| Music Songs tab | `(450, 550)` |
| First song row (`Tik Tok`) | `(600, 875)` |
| Player back | `(90, 365)` |
| Player play/pause control | `(600, 2080)` |

Observed live state:

- User is logged in and home data is populated.
- Music library shows 330 artists, 423 albums, and 439 songs.
- First deterministic song is `Tik Tok` by `2PM`.
- Player page successfully opened and showed artwork, progress, controls, and lyrics section.

## First measurement evidence

Command contract test:

```powershell
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario home-scroll -Rounds 1
```

Result from `build/perf/home-scroll-1.txt`:

| Metric | Value |
|---|---:|
| Total frames rendered | 888 |
| Janky frames | 2 (0.23%) |
| Janky frames legacy | 66 (7.43%) |
| P50 | 7 ms |
| P90 | 9 ms |
| P95 | 13 ms |
| P99 | 21 ms |
| Slow UI thread | 2 |
| Frame deadline missed | 2 |

This was only a one-round contract measurement, not the final three-round baseline table.

## Immediate next actions

1. Re-run one round each of `player-enter` and `player-playback` to validate the newly added deterministic paths after the latest script edit.
2. Capture active refresh-rate evidence during gestures; record observed mode rather than assuming 120 Hz from the configured peak.
3. Run all six scenarios for three rounds into `build/perf/baseline`.
4. Parse P50/P90/P95/P99, deadline misses, slow UI frames, jank, temperature, and refresh conditions.
5. Attempt the planned Perfetto system trace for worst player-enter and scroll cases; if rejected, record the rejection exactly and do not fabricate results.
6. Create `docs/testing/ui-performance-baseline-2026-07-15.md`.
7. Verify Task 1 files, stage only the script and baseline document, and commit with the planned message.
8. Before Task 2 symbol edits, run GitNexus impact for `AppBottomBar`, `AppBottomNavBar`, and `hyperOsEnterTransition`; append results here before editing.

## Update 2026-07-15 16:05 - player scenario contract checks

Both newly added deterministic player paths completed successfully on `192.168.0.109:43453`.

```powershell
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario player-enter -Rounds 1 -OutputDirectory build/perf/contract-player-enter
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario player-playback -Rounds 1 -OutputDirectory build/perf/contract-player-playback
```

| Scenario | Frames | P50 | P90 | P95 | P99 | Slow UI | Deadline missed |
|---|---:|---:|---:|---:|---:|---:|---:|
| player-enter | 216 | 6 ms | 44 ms | 81 ms | 150 ms | 8 | 8 |
| player-playback | 1452 | 6 ms | 7 ms | 14 ms | 73 ms | 8 | 9 |

Conclusion: deterministic paths are valid. `player-enter` is already confirmed as a major baseline hotspot. These are contract rounds only, not the final three-round baseline.

## Update 2026-07-15 16:08 - gesture refresh-rate evidence

A background `home-scroll` contract round was sampled with `dumpsys display` approximately every 150 ms. The completed run produced 25 samples:

- 20 samples: `mActiveModeId=1 mActiveRenderFrameRate=120.00001`
- 5 samples: `mActiveModeId=3 mActiveRenderFrameRate=60.000004`

The 60 Hz samples occurred around startup/idle boundaries; the repeated gesture interval was observed at 120 Hz. Evidence is stored in ignored file `build/perf/refresh-evidence.txt`.

An earlier `Start-Process` attempt failed before running the scenario because the workspace path contains spaces (`exit=-196608`) and captured only one idle 60 Hz sample. It is not acceptance evidence and was replaced by the successful PowerShell job sampling above.

## Resume command set

```powershell
git status --short --branch
Get-Content -Raw docs/testing/ui-performance-worklog-2026-07-15.md
Get-Content -Raw tools/perf/Measure-UiPerformance.ps1
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario player-enter -Rounds 1 -OutputDirectory build/perf/contract-player-enter
powershell -ExecutionPolicy Bypass -File tools/perf/Measure-UiPerformance.ps1 -Serial 192.168.0.109:43453 -Scenario player-playback -Rounds 1 -OutputDirectory build/perf/contract-player-playback
```

## Logging rule for continuation

Append every new result here before moving to the next substantive step. Include exact command, outcome, affected files/symbols, GitNexus risk, device conditions, and next action. Keep this worklog unstaged until the plan's documentation staging decision is made; do not accidentally include unrelated working-tree files.
