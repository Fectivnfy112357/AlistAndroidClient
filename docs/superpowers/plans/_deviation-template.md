# Plan ↔ Implementer Deviation Log

> When a task review surfaces a divergence between the plan's stated name/signature/parameter and what the implementer actually wrote (e.g., plan named the constant `HyperOsMotion` but the codebase used `CloudMotion`), append one row to this table.
>
> Goal: prevent future plan authors from guessing the wrong symbol name. Real values accumulate here so each subsequent plan can grep them.

## Format

| task_id | plan original | implementer actual | reason |

## Known Deviations (Backfilled 2026-07-09)

| task_id | plan original | implementer actual | reason |
|---------|---------------|--------------------|--------|
| Task 19 (P2-15, followups plan §Task 19) | `PreviewDestArgs.mime: String` carries MIME type | `PreviewDestArgs.mime: String` carries `FileType.name` (later corrected by P2-15 task to real MIME) | Plan guessed `mime` parameter was a MIME string; codebase used enum-name bridge until proper MIME resolution was added in Task 25 |
| Task 8 (UI Expressive plan §Task 8) | Bottom nav item includes `onMoreClick` parameter | Bottom nav uses M3 `ListItemRow` `trailing` slot for the More menu | Plan guessed a callback hook; codebase already had a slot-based trailing widget pattern |
| Task 17 (UI Expressive plan §Task 17) | `HyperOsMotion.SpringFast` constant | `CloudMotion.SpringFast` constant (`HyperOsMotion` does not exist in codebase) | Plan author confused motion namespace; codebase uses `CloudMotion` (later renamed to `AppMotion` in followups Task 5) |
| Task 13 (UI Expressive plan §Task 13) | `fileCategoryFromMime(null, name)` helper | `toFileCategory()` extension on `FileItem` matching `FileScreen` usage | Plan guessed a nullable-MIME overload; codebase normalized to non-null `FileItem.toFileCategory()` for consistency |