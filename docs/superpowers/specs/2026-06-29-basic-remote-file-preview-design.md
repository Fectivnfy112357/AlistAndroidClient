# Basic Remote File Preview Design

Date: 2026-06-29

## Goal

Add basic in-app preview for common file types in the Android app: images, audio, text, and Markdown. PDF is routed through external open/download in the first version because the chosen source strategy is direct server streaming without local preview caching.

## User Decisions

- File row click behavior:
  - Folder rows navigate into the folder.
  - File rows open preview directly.
  - Existing download/share actions remain available from row actions.
- In-app preview types:
  - Images: in-app preview.
  - Audio: in-app playback.
  - Text and Markdown: in-app text preview.
  - PDF: external open/download for v1.
  - Video, archives, and unknown types: external open/download.
- Preview source:
  - Use server-provided signed `downloadUrl` directly.
  - Do not download to local cache just for preview in v1.

## Current State

`PreviewRouter` currently routes local `File` objects. `PreviewScreen(filePath: String)` assumes a local path and reads the file as text. `FileItem` already contains enough remote metadata for preview routing: `name`, `path`, `type`, `size`, `thumbnailUrl`, and `downloadUrl`.

## Proposed Architecture

Change preview routing from local-file-based to remote-file-based.

Introduce or revise a preview mode model:

```kotlin
sealed interface PreviewMode {
    data class Image(val url: String) : PreviewMode
    data class Text(val url: String, val size: Long) : PreviewMode
    data class Audio(val url: String) : PreviewMode
    data class PdfExternal(val url: String) : PreviewMode
    data class External(val url: String) : PreviewMode
    data object Unavailable : PreviewMode
}
```

`PreviewRouter.route(item: FileItem)` should decide the mode:

- no `downloadUrl` -> `Unavailable`
- `FileType.Image` -> `Image(downloadUrl)`
- `FileType.Text` -> `Text(downloadUrl, size)` when under text limit, otherwise too-large state
- `FileType.Audio` -> `Audio(downloadUrl)`
- `FileType.Pdf` -> `PdfExternal(downloadUrl)`
- video/archive/other -> `External(downloadUrl)`

The existing local `PreviewAction` can either be replaced or kept separately only for external file-sharing helpers. The implementation should avoid mixing local and remote preview concepts in one sealed type.

## Navigation

Update `AppRoute.Preview` and `AppNavHost` to carry encoded remote metadata instead of a local file path. At minimum pass:

- `name`
- `type`
- `downloadUrl`
- `size`

If route length becomes awkward, use a compact encoded payload string. Keep the implementation simple and testable.

## Preview UI

`PreviewScreen` should render by preview mode:

### Image

Use Coil Compose (`AsyncImage`) against the signed URL. Show loading and failure states.

### Text / Markdown

Fetch text from the signed URL using OkHttp or an injected preview repository. Limit preview to `PreviewRouter.TEXT_PREVIEW_LIMIT_BYTES`. Markdown is rendered as plain text in v1.

### Audio

Use Android media playback support for a remote URL. V1 controls should include at least:

- file name
- play/pause
- current state text (`准备播放`, `播放中`, `已暂停`, `播放失败`)

No background playback or notification controls in v1.

### PDF / Unsupported

Show a clear unsupported/hand-off card with:

- `外部打开`
- `下载`

## Error Handling

- Missing `downloadUrl`: show `当前文件没有可用预览链接，请下载后查看`.
- Text too large: show `文件过大，可以下载或用其他应用打开`.
- Image load failure: show retry/download/external open options.
- Audio playback failure: show download/external open options.
- PDF unsupported in v1: show external open/download options.

## Testing

Add or update tests for:

- `PreviewRouter.route(item)` returns the correct mode for image/text/markdown/audio/pdf/unsupported/missing-url.
- `FileScreen` file row click opens preview while directory row still loads directory.
- `PreviewScreen` source-level or unit-level checks ensure it branches for image/text/audio/PDF modes.
- Existing `MimeTypeResolver` and `inferFileType` behavior remains valid.

## Scope Exclusions

- No video in-app player in v1.
- No PDF page rendering in v1.
- No local preview cache in v1.
- No background audio playback service.
- No complex image zoom gestures in v1.
