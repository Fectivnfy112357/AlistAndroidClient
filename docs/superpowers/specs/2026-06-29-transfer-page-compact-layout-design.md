# Transfer Page Compact Layout Design

Date: 2026-06-29

## Goal

Improve the Transfer page layout so upload/download tasks are easier to scan on a narrow Android screen. The selected direction is the compact list layout: high information density, clear progress, and visible actions without the current large vertical gaps.

## Current Problem

The current `TransferScreen` renders each task as a `CloudListItem`, then places the progress bar and actions in a separate `Column` below it. With multiple tasks this creates visually disconnected rows: titles, status text, progress bars, and retry/cancel buttons feel like separate items instead of one transfer record.

## Design

Each transfer task becomes one compact row/card with three areas:

1. **Main text area**
   - File name on the first line, bold and single-line ellipsized.
   - Status/failure text below it in secondary color, also constrained.
   - Progress text below or near the progress bar: `38.4% · 192.0 MB / 500.0 MB`.

2. **Progress area**
   - A thin horizontal progress bar sits inside the same task row/card.
   - Determinate progress is used when `totalBytes > 0`.
   - Indeterminate progress remains for unknown size.

3. **Action area**
   - One compact action button aligned to the right.
   - Active tasks show `取消` in error color.
   - Failed/interrupted tasks show `重试` in primary color.
   - Completed tasks show a small `完成` status chip instead of a button.

The top bar subtitle should summarize task counts, e.g. `2 个进行中 · 1 个失败`. If no task exists, keep the existing empty state.

## Scope

Change only the Transfer page presentation and small supporting formatting helpers. Do not change transfer execution, database schema, upload/download logic, or navigation behavior.

## Data Flow

`TransferViewModel` continues exposing `manager.observeTransfers()` unchanged. The UI derives compact summary values from `List<TransferEntity>`:

- active count: `Waiting`, `Uploading`, `Downloading`
- failed count: statuses with retry support
- completed count: `Success`

Each row reads `TransferEntity.statusText`, `progressText`, `showRetry`, and `retryButtonLabel`.

## Error Handling

No new error behavior. Failure reasons remain visible, but the compact row should ellipsize long messages so the layout stays stable.

## Testing

Add or update unit tests for:

- Transfer list summary text.
- Active task action label is `取消`.
- Failed task action label is `重试`.
- Completed task shows no retry/cancel action.
- Existing progress text formatting remains valid.

Manual verification after install:

- Open Transfer page with multiple tasks.
- Confirm each task appears as a single compact unit.
- Confirm progress text is visible and actions are aligned.
