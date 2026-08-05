# UI Performance Root-Cause Follow-up Design

## Status

Proposed follow-up to `2026-07-15-ui-performance-optimization-design.md` and its implementation plan. This document records code-level bottlenecks found after the final real-device acceptance did not show a broad, stable improvement. It is not an authorization to modify production code.

## Problem Statement

The initial optimization focused on Compose invalidation boundaries, Lazy item identity, and animation scope. Final three-round measurements showed isolated gains but no substantial improvement to the two dominant interactions:

| Scenario | Final median | Observation |
|---|---|---|
| tab-cycle | P90/P95/P99 `109/129/150 ms`; 38 deadline misses | Main-tab switching remains the largest repeated hotspot. |
| player-enter | P90/P95/P99 `53/85/150 ms`; 8 deadline misses | Player entry retains a long tail. |

The fixed `tab-cycle` script force-stops the app before every round. It therefore measures cold creation of root destinations, their ViewModels, subscriptions, data loading, and UI composition as well as bottom-navigation work. It must not be used as evidence for a bottom-bar-only micro-optimization.

## Evidence-Based Targets

### Music data lifecycle clarification

The app's persistent music index and its playable-URL queue are separate concerns:

- On music-library entry, `MusicIndexRepository.ensureIndexed()` first checks the local Room song count. When the index already contains songs, it publishes `Ready` and does not rescan the server's complete music tree.
- A full server scan is reserved for an empty index, an explicit rescan, or a deliberately invalidated music root.
- Entering the library still reads the local Room artist/album/song tables and projects their contents into UI models. With 439 songs this can create meaningful CPU/allocation work even though it makes no full-library network request.
- Selecting a song is different again: the playback service obtains signed, playable URLs for queue members. The current implementation progressively signs the entire selected queue after playback begins; this is the queue cost addressed by P0 batching below, not an App-start rescan.

### P0: Batch player queue construction and tail insertion

Current path:

1. `MusicLibraryViewModel.onPlayQueueClick` maps every visible `UiSong` to a new domain `Song` on the UI click path.
2. `PlaybackController.playQueue` maps the full queue again to paths and serializes all paths into an `Intent` extra.
3. `MusicPlaybackService.enqueueTail` starts one coroutine per remaining path and calls `ExoPlayer.addMediaItem` once per path on the main looper.

For the observed 439-song library, the tail path can schedule approximately 437 main-looper `addMediaItem` operations after the user selects one song. This is the strongest code-level explanation for unstable player-entry tails.

Required design direction:

- Pass a compact queue descriptor, such as a list identity plus start path/index, instead of copying complete song models through the UI and `Intent` path.
- Resolve the queue in the service on IO.
- Build signed items off-main and append them to ExoPlayer in bounded batches on the main looper, rather than one main-thread mutation per song.
- Keep initial playback behavior: first selected item starts as soon as it is ready, repeat/shuffle behavior remains valid, and full queue semantics are retained.
- Make `pathToSong` updates serialized and avoid repeated immutable-map copying for every tail item.

### P0: Stop unconditional file-directory reloads on every resume

`FileScreen` dispatches `FileIntent.Load(initialPath)` for every `LifecycleResumeEffect`. `FileViewModel.load` starts a new request without cancelling or sequencing an existing request.

Required design direction:

- Preserve immediate load for an unseen path, explicit refresh, successful delete, and an invalidated storage configuration.
- On bottom-tab return, render cached directory data immediately and refresh only when stale or invalidated.
- Give each load a request generation or cancel the previous load, so a late response cannot overwrite a newer navigation result.
- Keep the existing user-visible guarantee that a manual refresh retrieves current server data.

### P0: Move settings cache-size filesystem traversal off the main dispatcher

`SettingsViewModel` reads `MusicCache.sizeBytes` from `viewModelScope.launch`, whose default dispatcher is Main. `sizeBytes` walks the entire music cache directory and sums every file.

Required design direction:

- Compute cache size and cache clearing work on the injected IO dispatcher.
- Publish only the final size to UI state.
- Preserve the displayed byte count and the current clear-cache behavior.

### P1: Bound music-library startup projections

`MusicLibraryViewModel.state` combines index state, artists, all albums, recent albums, and all songs. Each source emission rebuilds an album-artwork map and remaps all domain lists to UI lists; the downstream collection runs in the ViewModel's Main context.

Required design direction:

- Move list-to-UI projection to the injected/default IO dispatcher, then publish immutable snapshots to Main.
- Avoid rebuilding artists/albums/songs that did not change when a sibling source emits.
- Derive recent albums from the already projected album snapshot when it is semantically equivalent, or otherwise retain a separate query with independent memoization.
- Preserve sort order, artwork selection, counts, tab content, and current scroll-state behavior.

### P1: Remove repeated transfer-list scans from the render path

`TransferListUiState` computes `visible`, three badge counts, failed count, and summary through independent getters. `TransferScreen` reads several of them per state emission, causing repeated full scans of the same transfer list. It also cross-fades the whole `TransferListContent` tree on transfer-subtab changes.

Required design direction:

- Compute visible rows, counts, and summary once when source data or selected tab changes.
- Keep exactly the same tabs, badges, rows, and summary text.
- Replace whole-list cross-fade with no animation or a bounded header-only indication if visual review accepts it; do not alter transfer status behavior.

### P2: Reduce file-row per-composition work

`FileListContent` applies bouncy `animateItem` to every row and creates a gradient plus formatted subtitle inside row composition. These costs are proportional to visible rows and directory refreshes.

Required design direction:

- Restrict item animation to structural list changes, or remove it for remote-directory replacement.
- Precompute display subtitle/type presentation when file data changes, not on each row recomposition.
- Reuse stable brushes by file type.
- Preserve file ordering, actions, selection behavior, and all row content.

### P2: Reduce homepage initial request fan-out only after tracing

`HomeRepository.loadDashboard` makes one public request, several admin requests, a sequential user/role pair, and seven task-type requests. The design already moves this work to IO and parallelizes many calls, so this is a first-load/network budget issue rather than a proven scroll bottleneck.

Required design direction:

- Do not remove dashboard information merely to reduce calls.
- First capture a trace and endpoint timings.
- Consider server aggregation, TTL caching, or deferred non-critical sections only if product requirements accept their freshness semantics.

## Business-Behavior Contract

| Change | Business behavior change? | Required guarantee |
|---|---|---|
| Player queue batching | No | Same selected song, queue order, repeat/shuffle behavior, controls, and eventual full queue. |
| File cached return + stale refresh | No, if invalidation is correct | Explicit refresh remains immediate; directory mutations/storage changes invalidate cache; return navigation can show cached data briefly before background refresh. |
| Settings cache-size IO dispatch | No | Same byte value and clear-cache result. |
| Music projection off-main/memoized | No | Same displayed artists, albums, songs, order, counts, and artwork. |
| Transfer derived-state caching | No | Same visible tasks, badge counts, summary, and actions. |
| File-row draw/animation reduction | No | Same rows, order, metadata, selection, menus, and actions. |
| Homepage request consolidation/defer | Potentially | Requires an explicit freshness and information-availability decision before implementation. |

## Measurement Plan

Separate the following cases; do not treat them as interchangeable:

1. Cold app start followed by first entry to each root tab.
2. Warm root-tab switching after every tab has been opened once.
3. Player entry with a small queue and the real 439-song queue.
4. File-tab return with unchanged directory, manual refresh, folder navigation, deletion, and storage-setting invalidation.

Before any new production edit, capture Android Studio System Trace or an equivalent app-owned Perfetto configuration for `tab-cycle` and `player-enter`. Record main-thread slices, RenderThread/GPU activity, allocation/GC evidence, and network/service startup timing. Use `gfxinfo` only as acceptance data, not as causal proof.

## Acceptance Criteria

- All behavior-contract cases have focused tests.
- Full Gradle build, lint, and unit tests pass.
- The same APK is installed on `192.168.0.109:43453`.
- Cold and warm metrics are reported separately, three rounds each.
- Player-entry report includes queue size and whether tail insertion overlapped the measured interval.
- Any P90/P95/P99 claim is supported by trace evidence and median data, not a single best run.
- No change is committed without GitNexus upstream impact before each production symbol edit and `detect_changes` before commit.

## Non-Goals

- Changing visual information architecture to hide existing content.
- Reducing user-visible data freshness without an explicit product decision.
- Treating a Debug `gfxinfo` number as proof of a Compose-specific root cause.
