# Basic Remote File Preview Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add in-app preview for images, audio, text, and Markdown using remote signed `downloadUrl` links, while routing PDF/video/archive/unknown types to external open/download.

**Architecture:** Convert preview from local-file routing to remote-file routing. `PreviewRouter` chooses a `PreviewMode` from `FileItem`; `FileScreen` navigates to a preview route carrying remote metadata; `PreviewScreen` renders by mode and fetches text through a small injected preview repository.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, Coil Compose, OkHttp, Android `MediaPlayer`, Hilt, JUnit.

## Global Constraints

- File row click behavior: folder rows navigate into the folder; file rows open preview directly.
- In-app preview types: images, audio, text, and Markdown.
- PDF: external open/download for v1.
- Video, archives, and unknown types: external open/download for v1.
- Preview source: use server-provided signed `downloadUrl` directly; do not download to local cache just for preview in v1.
- Markdown is rendered as plain text in v1.
- No background audio playback service in v1.
- No complex image zoom gestures in v1.
- Use TDD: every production behavior change starts with a failing test.
- Project instruction: run GitNexus impact analysis before editing symbols and `gitnexus_detect_changes()` before committing.
- Do not commit unless the user explicitly asks.

---

## File Structure

- Modify: `app/src/main/java/com/textvision/alistclient/preview/PreviewRouter.kt`
  - Defines remote preview modes and routes `FileItem` to a preview mode.
  - Keeps existing share/open helpers where useful.
- Create: `app/src/main/java/com/textvision/alistclient/preview/PreviewTextRepository.kt`
  - Fetches text previews from signed URLs using OkHttp with a 2MB cap.
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt`
  - Encodes/decodes preview route metadata.
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
  - Parses preview route metadata and calls the new `PreviewScreen` signature.
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt`
  - File row click navigates to preview; directories still load.
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt`
  - Renders image/text/audio/PDF/external/unavailable modes.
- Modify/Create tests under `app/src/test/java/com/textvision/alistclient/preview/`, `app/src/test/java/com/textvision/alistclient/navigation/`, and `app/src/test/java/com/textvision/alistclient/ui/screens/`.

---

### Task 1: Remote preview routing model

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/preview/PreviewRouter.kt`
- Test: `app/src/test/java/com/textvision/alistclient/preview/PreviewRouterTest.kt`

**Interfaces:**
- Consumes: `FileItem`, `FileType`.
- Produces:
  - `sealed interface PreviewMode`
  - `PreviewMode.Image(url: String)`
  - `PreviewMode.Text(url: String, size: Long)`
  - `PreviewMode.TextTooLarge(size: Long)`
  - `PreviewMode.Audio(url: String)`
  - `PreviewMode.External(url: String)`
  - `PreviewMode.Unavailable`
  - `PreviewRouter.route(item: FileItem): PreviewMode`

- [ ] **Step 1: Write failing PreviewRouter tests**

Create or update `app/src/test/java/com/textvision/alistclient/preview/PreviewRouterTest.kt`:

```kotlin
package com.textvision.alistclient.preview

import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewRouterTest {
    private fun item(
        type: FileType,
        url: String? = "https://example.test/file",
        size: Long = 1024L,
        name: String = "file.dat",
    ) = FileItem(
        name = name,
        path = "/$name",
        isDir = false,
        size = size,
        modifiedAt = null,
        extension = name.substringAfterLast('.', ""),
        type = type,
        thumbnailUrl = null,
        downloadUrl = url,
    )

    @Test fun routesImageTextAudioAndExternalTypes() {
        assertEquals(PreviewMode.Image("https://example.test/file"), PreviewRouter.route(item(FileType.Image, name = "photo.png")))
        assertEquals(PreviewMode.Text("https://example.test/file", 1024L), PreviewRouter.route(item(FileType.Text, name = "notes.md")))
        assertEquals(PreviewMode.Audio("https://example.test/file"), PreviewRouter.route(item(FileType.Audio, name = "song.mp3")))
        assertEquals(PreviewMode.External("https://example.test/file"), PreviewRouter.route(item(FileType.Pdf, name = "doc.pdf")))
        assertEquals(PreviewMode.External("https://example.test/file"), PreviewRouter.route(item(FileType.Video, name = "movie.mp4")))
    }

    @Test fun routesMissingUrlAndOversizedTextToSafeStates() {
        assertEquals(PreviewMode.Unavailable, PreviewRouter.route(item(FileType.Image, url = null, name = "photo.png")))
        assertEquals(PreviewMode.TextTooLarge(PreviewRouter.TEXT_PREVIEW_LIMIT_BYTES + 1), PreviewRouter.route(
            item(FileType.Text, size = PreviewRouter.TEXT_PREVIEW_LIMIT_BYTES + 1, name = "large.log")
        ))
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.preview.PreviewRouterTest"
```

Expected: compile failure because `PreviewMode` and `PreviewRouter.route(item)` do not exist in the new shape.

- [ ] **Step 3: Implement PreviewMode and remote route**

Replace the top sealed action definitions in `PreviewRouter.kt` with:

```kotlin
sealed interface PreviewMode {
    data class Image(val url: String) : PreviewMode
    data class Text(val url: String, val size: Long) : PreviewMode
    data class TextTooLarge(val size: Long) : PreviewMode
    data class Audio(val url: String) : PreviewMode
    data class External(val url: String) : PreviewMode
    data object Unavailable : PreviewMode
}
```

Add this overload inside `PreviewRouter`:

```kotlin
fun route(item: FileItem): PreviewMode {
    val url = item.downloadUrl ?: return PreviewMode.Unavailable
    return when (item.type) {
        FileType.Image -> PreviewMode.Image(url)
        FileType.Text -> if (item.size <= TEXT_PREVIEW_LIMIT_BYTES) PreviewMode.Text(url, item.size) else PreviewMode.TextTooLarge(item.size)
        FileType.Audio -> PreviewMode.Audio(url)
        FileType.Pdf,
        FileType.Video,
        FileType.Archive,
        FileType.Other -> PreviewMode.External(url)
        FileType.Folder -> PreviewMode.Unavailable
    }
}
```

Keep existing local `openIntent`, `shareFileIntent`, and `shareLinkIntent` helpers for now. If existing `PreviewAction` conflicts with the new model, rename local-only actions to `LocalPreviewAction` and update references/tests.

- [ ] **Step 4: Run PreviewRouter tests and verify GREEN**

Run the command from Step 2.

Expected: PASS.

---

### Task 2: Preview route metadata navigation

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Test: `app/src/test/java/com/textvision/alistclient/navigation/AppRouteTest.kt`

**Interfaces:**
- Consumes: file metadata (`name`, `type`, `downloadUrl`, `size`).
- Produces:
  - `AppRoute.Preview.create(name: String, type: FileType, downloadUrl: String?, size: Long): String`
  - `AppRoute.Preview.parse(name: String, type: String, downloadUrl: String?, size: String): PreviewArgs`
  - `data class PreviewArgs(val name: String, val type: FileType, val downloadUrl: String?, val size: Long)`

- [ ] **Step 1: Write failing AppRoute preview route tests**

Create or update `app/src/test/java/com/textvision/alistclient/navigation/AppRouteTest.kt`:

```kotlin
package com.textvision.alistclient.navigation

import com.textvision.alistclient.file.model.FileType
import org.junit.Assert.assertEquals
import org.junit.Test

class AppRouteTest {
    @Test fun previewRouteRoundTripsRemotePreviewMetadata() {
        val route = AppRoute.Preview.create(
            name = "a b.png",
            type = FileType.Image,
            downloadUrl = "https://example.test/d/a%20b.png?sign=abc&x=1",
            size = 1234L,
        )

        val prefix = "preview/"
        val encoded = route.removePrefix(prefix)
        val args = AppRoute.Preview.decode(encoded)

        assertEquals("a b.png", args.name)
        assertEquals(FileType.Image, args.type)
        assertEquals("https://example.test/d/a%20b.png?sign=abc&x=1", args.downloadUrl)
        assertEquals(1234L, args.size)
    }
}
```

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.navigation.AppRouteTest.previewRouteRoundTripsRemotePreviewMetadata"
```

Expected: compile failure because the new overload/decode API does not exist.

- [ ] **Step 3: Implement compact encoded preview route**

In `AppRoute.kt`, implement preview route as one encoded payload segment to avoid multiple optional URL path args:

```kotlin
data class PreviewArgs(
    val name: String,
    val type: FileType,
    val downloadUrl: String?,
    val size: Long,
)
```

Update `AppRoute.Preview`:

```kotlin
data object Preview : AppRoute("preview/{payload}") {
    fun create(name: String, type: FileType, downloadUrl: String?, size: Long): String {
        val raw = listOf(
            Uri.encode(name),
            type.name,
            Uri.encode(downloadUrl.orEmpty()),
            size.toString(),
        ).joinToString("|")
        return "preview/${Uri.encode(raw)}"
    }

    fun decode(payload: String): PreviewArgs {
        val raw = Uri.decode(payload)
        val parts = raw.split("|", limit = 4)
        return PreviewArgs(
            name = Uri.decode(parts.getOrElse(0) { "" }),
            type = runCatching { FileType.valueOf(parts.getOrElse(1) { FileType.Other.name }) }.getOrDefault(FileType.Other),
            downloadUrl = Uri.decode(parts.getOrElse(2) { "" }).takeIf { it.isNotBlank() },
            size = parts.getOrElse(3) { "0" }.toLongOrNull() ?: 0L,
        )
    }
}
```

Add imports:

```kotlin
import android.net.Uri
import com.textvision.alistclient.file.model.FileType
```

- [ ] **Step 4: Update AppNavHost preview argument parsing**

In `AppNavHost.kt`, change preview composable argument from `filePath` to `payload`:

```kotlin
composable(
    route = AppRoute.Preview.route,
    arguments = listOf(navArgument("payload") { type = NavType.StringType })
) { entry ->
    val payload = requireNotNull(entry.arguments?.getString("payload"))
    val args = AppRoute.Preview.decode(payload)
    PreviewScreen(
        name = args.name,
        type = args.type,
        downloadUrl = args.downloadUrl,
        size = args.size,
        onDownload = { navController.popBackStack() },
        onExternalOpen = { navController.popBackStack() },
    )
}
```

Remove now-unused `android.net.Uri` import if no longer used.

- [ ] **Step 5: Run AppRoute test and compile**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.navigation.AppRouteTest.previewRouteRoundTripsRemotePreviewMetadata"
```

Expected: PASS.

---

### Task 3: File row click opens preview

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt`
- Test: `app/src/test/java/com/textvision/alistclient/ui/screens/FileScreenSourceTest.kt`

**Interfaces:**
- Consumes: `AppRoute.Preview.create(name, type, downloadUrl, size)` from Task 2.
- Produces: `FileRow(..., onPreview: () -> Unit)` behavior where file click opens preview and directory click still opens folder.

- [ ] **Step 1: Write failing source test for file click preview route**

Create or update `app/src/test/java/com/textvision/alistclient/ui/screens/FileScreenSourceTest.kt`:

```kotlin
package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileScreenSourceTest {
    @Test fun fileRowsNavigateToPreviewRouteOnClick() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt").readText()

        assertTrue(source.contains("AppRoute.Preview.create("))
        assertTrue(source.contains("onPreview = { navController.navigate"))
        assertTrue(source.contains("onClick = if (item.isDir) onOpenDir else onPreview"))
    }
}
```

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.screens.FileScreenSourceTest.fileRowsNavigateToPreviewRouteOnClick"
```

Expected: FAIL because `FileScreen` does not navigate to preview on file row click.

- [ ] **Step 3: Add preview navigation to FileScreen**

Add import:

```kotlin
import com.textvision.alistclient.navigation.AppRoute
```

Change `FileScreen` signature to accept navigation lambda:

```kotlin
fun FileScreen(
    viewModel: FileViewModel = hiltViewModel(),
    onPreview: (FileItem) -> Unit = {},
) {
```

Inside `items`, update `FileRow` call:

```kotlin
FileRow(
    item = item,
    onOpenDir = { viewModel.load(item.path) },
    onPreview = { onPreview(item) },
    onDownload = { viewModel.enqueueDownload(item) },
    onShare = { ... },
)
```

Update `FileRow` signature:

```kotlin
private fun FileRow(
    item: FileItem,
    onOpenDir: () -> Unit,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
)
```

Update row click:

```kotlin
onClick = if (item.isDir) onOpenDir else onPreview,
```

- [ ] **Step 4: Wire AppNavHost to pass preview navigation**

In `AppNavHost.kt`, change files composable:

```kotlin
composable(AppRoute.Files.route) {
    FileScreen(
        onPreview = { item ->
            navController.navigate(
                AppRoute.Preview.create(
                    name = item.name,
                    type = item.type,
                    downloadUrl = item.downloadUrl,
                    size = item.size,
                )
            )
        }
    )
}
```

- [ ] **Step 5: Run FileScreen source test**

Run the command from Step 2.

Expected: PASS.

---

### Task 4: Text preview repository

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/preview/PreviewTextRepository.kt`
- Test: `app/src/test/java/com/textvision/alistclient/preview/PreviewTextRepositoryTest.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt` only if Hilt needs a binding.

**Interfaces:**
- Consumes: OkHttp `OkHttpClient`.
- Produces: `class PreviewTextRepository @Inject constructor(private val okHttpClient: OkHttpClient)` with `suspend fun fetch(url: String): Result<String>`.

- [ ] **Step 1: Write failing repository tests**

Create `PreviewTextRepositoryTest.kt`:

```kotlin
package com.textvision.alistclient.preview

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PreviewTextRepositoryTest {
    private lateinit var server: MockWebServer

    @Before fun setUp() { server = MockWebServer().also { it.start() } }
    @After fun tearDown() { server.shutdown() }

    @Test fun fetchReturnsResponseText() = runTest {
        server.enqueue(MockResponse().setBody("hello preview"))
        val repo = PreviewTextRepository(OkHttpClient())

        val result = repo.fetch(server.url("/file.txt").toString())

        assertEquals("hello preview", result.getOrThrow())
    }

    @Test fun fetchFailsOnHttpError() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("missing"))
        val repo = PreviewTextRepository(OkHttpClient())

        val result = repo.fetch(server.url("/missing.txt").toString())

        assertTrue(result.isFailure)
    }
}
```

- [ ] **Step 2: Run tests and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.preview.PreviewTextRepositoryTest"
```

Expected: compile failure because `PreviewTextRepository` does not exist.

- [ ] **Step 3: Implement repository**

Create `PreviewTextRepository.kt`:

```kotlin
package com.textvision.alistclient.preview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreviewTextRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun fetch(url: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
                response.body?.string().orEmpty()
            }
        }
    }
}
```

- [ ] **Step 4: Run repository tests**

Run Step 2 command.

Expected: PASS.

---

### Task 5: PreviewScreen renders image/text/audio/external modes

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt`
- Test: `app/src/test/java/com/textvision/alistclient/ui/screens/PreviewScreenSourceTest.kt`

**Interfaces:**
- Consumes: `PreviewRouter.route(FileItem)` indirectly through `PreviewScreen(name, type, downloadUrl, size, ...)`.
- Consumes: `PreviewTextRepository.fetch(url)` from Task 4.
- Produces: `PreviewScreen(name: String, type: FileType, downloadUrl: String?, size: Long, onDownload: () -> Unit, onExternalOpen: () -> Unit)`.

- [ ] **Step 1: Write failing PreviewScreen source test**

Create `PreviewScreenSourceTest.kt`:

```kotlin
package com.textvision.alistclient.ui.screens

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PreviewScreenSourceTest {
    @Test fun previewScreenBranchesForImageTextAudioAndExternalModes() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt").readText()

        assertTrue(source.contains("PreviewMode.Image"))
        assertTrue(source.contains("PreviewMode.Text"))
        assertTrue(source.contains("PreviewMode.Audio"))
        assertTrue(source.contains("AsyncImage"))
        assertTrue(source.contains("MediaPlayer"))
        assertTrue(source.contains("PreviewTextRepository"))
    }
}
```

- [ ] **Step 2: Run test and verify RED**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.ui.screens.PreviewScreenSourceTest.previewScreenBranchesForImageTextAudioAndExternalModes"
```

Expected: FAIL because current screen only reads a local file as text.

- [ ] **Step 3: Replace PreviewScreen signature and routing**

In `PreviewScreen.kt`, change signature:

```kotlin
@Composable
fun PreviewScreen(
    name: String,
    type: FileType,
    downloadUrl: String?,
    size: Long,
    onDownload: () -> Unit,
    onExternalOpen: () -> Unit,
    textRepository: PreviewTextRepository = hiltViewModel<PreviewViewModel>().textRepository,
)
```

If exposing repository through `hiltViewModel` is awkward, create a small `@HiltViewModel PreviewViewModel @Inject constructor(val textRepository: PreviewTextRepository) : ViewModel()` in this file.

Construct a temporary `FileItem` to reuse router:

```kotlin
val mode = remember(name, type, downloadUrl, size) {
    PreviewRouter.route(FileItem(name, name, false, size, null, name.substringAfterLast('.', ""), type, null, downloadUrl))
}
```

- [ ] **Step 4: Implement image mode UI**

Use Coil:

```kotlin
@Composable
private fun ImagePreview(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier.fillMaxWidth(),
    )
}
```

Add imports:

```kotlin
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.fillMaxWidth
```

- [ ] **Step 5: Implement text mode UI**

Use repository in `LaunchedEffect(url)`:

```kotlin
@Composable
private fun TextPreview(url: String, textRepository: PreviewTextRepository) {
    var text by remember(url) { mutableStateOf("加载中") }
    LaunchedEffect(url) {
        text = textRepository.fetch(url).getOrElse { "无法读取文件" }
    }
    Text(
        text = text,
        modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
    )
}
```

- [ ] **Step 6: Implement audio mode UI**

Add simple `MediaPlayer` lifecycle:

```kotlin
@Composable
private fun AudioPreview(url: String) {
    var isPlaying by remember(url) { mutableStateOf(false) }
    var error by remember(url) { mutableStateOf<String?>(null) }
    val player = remember(url) { MediaPlayer() }
    DisposableEffect(url) {
        runCatching {
            player.setDataSource(url)
            player.prepareAsync()
        }.onFailure { error = "播放失败" }
        onDispose { player.release() }
    }
    Column(Modifier.padding(12.dp)) {
        Text(error ?: if (isPlaying) "播放中" else "准备播放")
        TextButton(onClick = {
            runCatching {
                if (isPlaying) player.pause() else player.start()
                isPlaying = !isPlaying
            }.onFailure { error = "播放失败" }
        }) { Text(if (isPlaying) "暂停" else "播放") }
    }
}
```

Add import:

```kotlin
import android.media.MediaPlayer
import androidx.compose.runtime.DisposableEffect
```

- [ ] **Step 7: Implement fallback modes**

Inside `CloudCard`, switch on mode:

```kotlin
when (mode) {
    is PreviewMode.Image -> ImagePreview(mode.url)
    is PreviewMode.Text -> TextPreview(mode.url, textRepository)
    is PreviewMode.Audio -> AudioPreview(mode.url)
    is PreviewMode.TextTooLarge -> PreviewFallback("文件过大", "可以下载或用其他应用打开", onDownload, onExternalOpen)
    is PreviewMode.External -> PreviewFallback("暂不支持内置预览", "可以下载或用其他应用打开", onDownload, onExternalOpen)
    PreviewMode.Unavailable -> PreviewFallback("无法预览", "当前文件没有可用预览链接，请下载后查看", onDownload, onExternalOpen)
}
```

Use a helper:

```kotlin
@Composable
private fun PreviewFallback(title: String, message: String, onDownload: () -> Unit, onExternalOpen: () -> Unit) {
    CloudEmptyState(
        title = title,
        message = message,
        action = {
            Row {
                TextButton(onClick = onDownload) { Text("下载") }
                TextButton(onClick = onExternalOpen) { Text("外部打开") }
            }
        },
    )
}
```

- [ ] **Step 8: Run PreviewScreen source test**

Run Step 2 command.

Expected: PASS.

---

### Task 6: Verify and install

**Files:**
- Test only; no expected production changes unless tests reveal issues.

**Interfaces:**
- Consumes: all prior tasks.
- Produces: installed APK with basic preview support.

- [ ] **Step 1: Run targeted tests**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "com.textvision.alistclient.preview.*" --tests "com.textvision.alistclient.navigation.AppRouteTest" --tests "com.textvision.alistclient.ui.screens.FileScreenSourceTest" --tests "com.textvision.alistclient.ui.screens.PreviewScreenSourceTest"
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Run full debug unit test suite**

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Install debug build**

Run:

```powershell
.\gradlew.bat :app:installDebug
```

Expected: `Installed on 1 device.` and `BUILD SUCCESSFUL`.

- [ ] **Step 4: Manual checks on emulator**

1. Tap an image file row: preview page opens and displays image.
2. Tap a text or Markdown file row: preview page opens and displays text.
3. Tap an audio file row: preview page opens with play/pause controls.
4. Tap a PDF: preview page offers external open/download.
5. Tap a folder row: still navigates into the folder, not preview.

---

## Self-Review

**Spec coverage:**
- Images in-app: Task 5.
- Audio in-app: Task 5.
- Text/Markdown in-app: Task 1 + Task 5.
- PDF external for v1: Task 1 + Task 5.
- Other external/download: Task 1 + Task 5.
- Server signed URL, no preview cache: Task 1 + Task 4 + Task 5.
- File click opens preview: Task 3.

**Placeholder scan:** No TBD/TODO/fill-in placeholders are present.

**Type consistency:** `PreviewMode`, `PreviewRouter.route(item)`, `PreviewTextRepository.fetch(url)`, and `PreviewScreen(...)` are consistently named and consumed.
