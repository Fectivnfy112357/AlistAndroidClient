# Alist Android Client Implementation Plan Index

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Alist v3 Android client as a sequence of independently reviewable implementation phases.

**Architecture:** This plan suite is split by delivery phase. Each phase plan produces working, testable software and must be completed in order unless explicitly marked optional.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose, Material 3, Hilt, Retrofit, OkHttp, kotlinx-serialization, Room, Coil 3, AndroidX Security Crypto, MockK, MockWebServer, Turbine.

## Global Constraints

- minSdk = 26; targetSdk = 34; compileSdk = 34.
- Kotlin = 2.0.21; use KSP, not KAPT.
- Kotlin 2.0+ Compose compiler is configured with `org.jetbrains.kotlin.plugin.compose`; do not set `composeOptions.kotlinCompilerExtensionVersion`.
- Single Gradle module for MVP; use packages for boundaries.
- Use `kotlinx-serialization`, not Moshi.
- Use `ActivityResultContracts.GetContent()` for uploads.
- Upload main path is `PUT /api/fs/put`; `/api/fs/form` is optional compatibility only.
- Downloads go to `filesDir/downloads/{sha1(path)}.ext`; preview cache goes to `cacheDir/preview/`.
- Transfer concurrency: upload = 2; download = 3.
- No reliable background transfer, no resumable transfer, no multi-account, no admin panel, no Alist v2 support.
- Do not log passwords, tokens, Authorization headers, full download links, full share links, or sensitive full paths.
- HTTPS certificate errors are not bypassable.

---

## Source Spec

Read before executing any phase:

- `docs/superpowers/specs/2026-06-23-alist-android-client-design.md`

## Plan Files

Execute in this order:

1. `docs/superpowers/plans/2026-06-23-alist-android-client-phase-0-foundation.md`
   - Creates the Android project skeleton, Gradle setup, dependencies, base navigation, backup rules, FileProvider, Hilt, Room smoke test, encrypted prefs smoke test.
2. `docs/superpowers/plans/2026-06-23-alist-android-client-phase-1-auth-session.md`
   - Implements URL normalization, API result envelope, error mapping, login, encrypted credentials, token interceptor, Authenticator, session restore, logout.
3. `docs/superpowers/plans/2026-06-23-alist-android-client-phase-2-file-browsing.md`
   - Implements file list, DTO mapping, root browsing, breadcrumbs, refresh, sorting, search debounce, thumbnails, CRUD basics.
4. `docs/superpowers/plans/2026-06-23-alist-android-client-phase-3a-transfer-core.md`
   - Implements Room transfer tasks, TransferManager, upload/download queues, progress, cancellation, retry, private downloads, FileProvider open/share.
5. `docs/superpowers/plans/2026-06-23-alist-android-client-phase-3b-copy-move-notifications.md`
   - Implements copy/move picker, serial multi-file operations, transfer notification channels, merged progress notification, interrupted task restore.
6. `docs/superpowers/plans/2026-06-23-alist-android-client-phase-4-preview-sharing-settings.md`
   - Implements preview routing, image/text previews, external open, system share, link share, settings, crash logs, NetworkMonitor.
7. `docs/superpowers/plans/2026-06-23-alist-android-client-phase-5-integration-release.md`
   - Runs integration, compatibility, weak network, kill-process, backup, large-file, monkey, and packaging checks.

## Execution Rules

- Work phase-by-phase.
- Within a phase, work task-by-task.
- Use TDD where possible: write the failing test, run it, implement, rerun.
- Commit after each task if this directory is a git repository. If not, record the intended commit message in the task notes.
- Never skip verification. If a verification command cannot run because SDK/tooling is unavailable, record the exact failure output and stop for user decision.

## Phase Completion Gate

A phase is complete only when:

- Every checkbox in that phase file is complete.
- The phase verification commands pass or the failure is explicitly documented.
- The phase deliverable can be demonstrated without relying on later phases, except where the phase file states a mock/stub is expected.
