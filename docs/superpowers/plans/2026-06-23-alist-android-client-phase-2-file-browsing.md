# Phase 2 File Browsing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement authenticated remote directory browsing with DTO mapping, breadcrumbs, refresh, sorting, search debounce, thumbnails, file-name validation, and basic CRUD operations.

**Architecture:** Alist DTOs remain in `network/dto`; app models live in `file/model`; `FileRepository` maps DTOs to `FileItem`; `FileViewModel` owns path/search/sort/selection state. UI uses Compose state only and never sees DTOs.

**Tech Stack:** Kotlin, Compose, Hilt, Retrofit, OkHttp, kotlinx-serialization, Coil 3, Turbine, MockWebServer.

## Global Constraints

- `/api/fs/list` is `POST` JSON body `{path, page, per_page}`.
- `/api/fs/search` is `POST` JSON body `{path, keywords, page, per_page}`.
- List/search requests must be cancelable with `flatMapLatest` or equivalent.
- Search debounce = 300ms.
- Thumbnail loading must use the app OkHttp client with Authorization header.
- `FileItem.size == 0L && !isDir` displays `未知大小`.
- File name validation rejects blank, `.`, `..`, `/`, `\`, ISO control chars, and length > 255.

---

## File Structure

Create:

```text
app/src/main/java/com/textvision/alistclient/file/model/FileItem.kt
app/src/main/java/com/textvision/alistclient/file/model/FileSort.kt
app/src/main/java/com/textvision/alistclient/file/model/FileType.kt
app/src/main/java/com/textvision/alistclient/file/model/FileUiState.kt
app/src/main/java/com/textvision/alistclient/file/FileNameValidator.kt
app/src/main/java/com/textvision/alistclient/file/FileRepository.kt
app/src/main/java/com/textvision/alistclient/file/FileViewModel.kt
app/src/main/java/com/textvision/alistclient/network/dto/FileDtos.kt
app/src/main/java/com/textvision/alistclient/ui/components/FileTypeIcon.kt
app/src/main/java/com/textvision/alistclient/ui/components/BreadcrumbBar.kt
app/src/test/java/com/textvision/alistclient/file/FileNameValidatorTest.kt
app/src/test/java/com/textvision/alistclient/file/FileDtoMappingTest.kt
app/src/test/java/com/textvision/alistclient/file/FileViewModelTest.kt
```

Modify:

```text
app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt
app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt
```

---

### Task 2.1: File models, DTOs, and mapping

**Files:**
- Create: `file/model/FileItem.kt`
- Create: `file/model/FileType.kt`
- Create: `network/dto/FileDtos.kt`
- Test: `file/FileDtoMappingTest.kt`

**Interfaces:**
- Produces: `AlistFileDto.toFileItem(parentPath: String, baseUrl: String): FileItem`.
- Produces: `FileType` enum and `inferFileType(extension: String?, isDir: Boolean): FileType`.
- Consumes: spec section 4.11 DTO mapping.

- [ ] **Step 1: Write failing DTO mapping tests**

```kotlin
package com.textvision.alistclient.file

import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.network.dto.AlistFileDto
import com.textvision.alistclient.network.dto.toFileItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileDtoMappingTest {
    @Test fun mapsFolderPathFromParentAndName() {
        val item = AlistFileDto(name = "docs", isDir = true, fileType = 0).toFileItem("/", "http://s/")
        assertEquals("/docs", item.path)
        assertTrue(item.isDir)
        assertEquals(FileType.Folder, item.type)
    }

    @Test fun mapsFileExtensionAndThumbnailUrl() {
        val item = AlistFileDto(name = "cat.JPG", size = 10, thumb = "abc", sign = "def").toFileItem("/photos", "http://s/")
        assertEquals("/photos/cat.JPG", item.path)
        assertFalse(item.isDir)
        assertEquals("jpg", item.extension)
        assertEquals(FileType.Image, item.type)
        assertEquals("http://s/p//photos/cat.JPG?sign=abc", item.thumbnailUrl)
        assertEquals("http://s/d//photos/cat.JPG?sign=def", item.downloadUrl)
    }

    @Test fun toleratesMissingDateAndSize() {
        val item = AlistFileDto(name = "unknown.bin").toFileItem("/", "http://s/")
        assertEquals(0L, item.size)
        assertNull(item.modifiedAt)
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileDtoMappingTest"
```

Expected: FAIL because models and DTOs do not exist.

- [ ] **Step 3: Implement models and DTO mapping**

`FileType.kt`:

```kotlin
package com.textvision.alistclient.file.model

enum class FileType { Folder, Image, Text, Audio, Video, Pdf, Archive, Other }

fun inferFileType(extension: String?, isDir: Boolean): FileType {
    if (isDir) return FileType.Folder
    return when (extension?.lowercase()) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> FileType.Image
        "txt", "md", "json", "xml", "csv", "log", "kt", "java", "js", "ts", "html", "css" -> FileType.Text
        "mp3", "wav", "flac", "aac", "ogg" -> FileType.Audio
        "mp4", "mkv", "webm", "mov", "avi" -> FileType.Video
        "pdf" -> FileType.Pdf
        "zip", "rar", "7z", "tar", "gz" -> FileType.Archive
        else -> FileType.Other
    }
}
```

`FileItem.kt`:

```kotlin
package com.textvision.alistclient.file.model

import kotlinx.datetime.Instant

data class FileItem(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val modifiedAt: Instant?,
    val extension: String?,
    val type: FileType,
    val thumbnailUrl: String?,
    val downloadUrl: String?,
)
```

`FileDtos.kt`:

```kotlin
package com.textvision.alistclient.network.dto

import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.inferFileType
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlistFsList(
    @SerialName("content") val content: List<AlistFileDto> = emptyList(),
    val total: Int = 0,
    @SerialName("readme") val readme: String? = null,
    val header: String? = null,
)

@Serializable
data class AlistFileDto(
    val name: String,
    val size: Long = 0,
    @SerialName("is_dir") val isDir: Boolean = false,
    val modified: String? = null,
    val created: String? = null,
    val sign: String? = null,
    val thumb: String? = null,
    @SerialName("type") val fileType: Int? = null,
)

fun AlistFileDto.toFileItem(parentPath: String, baseUrl: String): FileItem {
    val normalizedParent = parentPath.trimEnd('/')
    val fullPath = if (normalizedParent.isEmpty()) "/$name" else "$normalizedParent/$name"
    val directory = isDir || fileType == 0
    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .takeIf { it.isNotBlank() && it != name.lowercase() }
    val cleanBase = baseUrl.trimEnd('/')
    return FileItem(
        name = name,
        path = fullPath,
        isDir = directory,
        size = size,
        modifiedAt = modified?.let { runCatching { Instant.parse(it) }.getOrNull() },
        extension = extension,
        type = inferFileType(extension, directory),
        thumbnailUrl = thumb?.let { "$cleanBase/p/$fullPath?sign=$it" },
        downloadUrl = sign?.let { "$cleanBase/d/$fullPath?sign=$it" },
    )
}
```

- [ ] **Step 4: Run tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileDtoMappingTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/file/model app/src/main/java/com/textvision/alistclient/network/dto/FileDtos.kt app/src/test/java/com/textvision/alistclient/file/FileDtoMappingTest.kt
git commit -m "feat: add file dto mapping"
```

---

### Task 2.2: File name validation

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/file/FileNameValidator.kt`
- Test: `app/src/test/java/com/textvision/alistclient/file/FileNameValidatorTest.kt`

**Interfaces:**
- Produces: `object FileNameValidator { fun isValid(name: String): Boolean; fun errorMessage(name: String): String? }`.
- Consumes: none.

- [ ] **Step 1: Write failing tests**

```kotlin
package com.textvision.alistclient.file

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameValidatorTest {
    @Test fun validatesLegalName() {
        assertTrue(FileNameValidator.isValid("photo.jpg"))
        assertNull(FileNameValidator.errorMessage("photo.jpg"))
    }

    @Test fun rejectsSpecificInvalidNames() {
        assertEquals("名称不能为空", FileNameValidator.errorMessage("   "))
        assertEquals("名称不合法", FileNameValidator.errorMessage(".."))
        assertEquals("名称不能包含 / 或 \\", FileNameValidator.errorMessage("a/b"))
        assertEquals("名称包含非法字符", FileNameValidator.errorMessage("a b"))
        assertEquals("名称过长（最多 255 字符）", FileNameValidator.errorMessage("a".repeat(256)))
        assertFalse(FileNameValidator.isValid("a\\b"))
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileNameValidatorTest"
```

Expected: FAIL because validator does not exist.

- [ ] **Step 3: Implement validator**

```kotlin
package com.textvision.alistclient.file

object FileNameValidator {
    fun isValid(name: String): Boolean = errorMessage(name) == null

    fun errorMessage(name: String): String? {
        val trimmed = name.trim()
        if (name.isBlank()) return "名称不能为空"
        if (trimmed == "." || trimmed == "..") return "名称不合法"
        if (name.contains('/') || name.contains('\\')) return "名称不能包含 / 或 \\"
        if (name.any { it.isISOControl() }) return "名称包含非法字符"
        if (trimmed.length > 255) return "名称过长（最多 255 字符）"
        return null
    }
}
```

- [ ] **Step 4: Run tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileNameValidatorTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/file/FileNameValidator.kt app/src/test/java/com/textvision/alistclient/file/FileNameValidatorTest.kt
git commit -m "feat: add file name validation"
```

---

### Task 2.3: Alist file API and repository

**Files:**
- Modify: `network/api/AlistApi.kt`
- Create: `file/FileRepository.kt`

**Interfaces:**
- Produces: `FileRepository.list(path): ApiResult<List<FileItem>>`.
- Produces: `FileRepository.search(path, keyword): ApiResult<List<FileItem>>`.
- Produces: `mkdir(path)`, `rename(path, name)`, `delete(paths)` returning `ApiResult<Unit>`.
- Consumes: `AlistApi`, DTO mapping, `ApiResult`.

- [ ] **Step 1: Extend API interface**

Add DTO request types to `FileDtos.kt`:

```kotlin
@Serializable data class FsListRequest(val path: String, val page: Int = 1, @SerialName("per_page") val perPage: Int = 0)
@Serializable data class FsSearchRequest(val path: String, val keywords: String, val page: Int = 1, @SerialName("per_page") val perPage: Int = 50)
@Serializable data class MkdirRequest(val path: String)
@Serializable data class RenameRequest(val path: String, val name: String)
@Serializable data class RemoveRequest(val dir: String, val names: List<String>)
```

Add to `AlistApi.kt`:

```kotlin
@POST("api/fs/list")
suspend fun list(@Body request: FsListRequest): AlistResponse<AlistFsList>

@POST("api/fs/search")
suspend fun search(@Body request: FsSearchRequest): AlistResponse<AlistFsList>

@POST("api/fs/mkdir")
suspend fun mkdir(@Body request: MkdirRequest): AlistResponse<Unit>

@POST("api/fs/rename")
suspend fun rename(@Body request: RenameRequest): AlistResponse<Unit>

@POST("api/fs/remove")
suspend fun remove(@Body request: RemoveRequest): AlistResponse<Unit>
```

Ensure imports include all request DTOs.

- [ ] **Step 2: Implement repository**

`FileRepository.kt`:

```kotlin
package com.textvision.alistclient.file

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.FsListRequest
import com.textvision.alistclient.network.dto.FsSearchRequest
import com.textvision.alistclient.network.dto.MkdirRequest
import com.textvision.alistclient.network.dto.RemoveRequest
import com.textvision.alistclient.network.dto.RenameRequest
import com.textvision.alistclient.network.dto.toFileItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    private val api: AlistApi,
) {
    private val baseUrl: String = "" // Phase 1 dynamic base URL replacement can update this provider later.

    suspend fun list(path: String): ApiResult<List<FileItem>> = runAlist {
        val response = api.list(FsListRequest(path = path))
        if (response.code == 200) response.data?.content.orEmpty()
            .map { it.toFileItem(path, baseUrl) }
            .sortedWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name.lowercase() })
            .let { ApiResult.Success(it) }
        else ApiResult.Failure(response.code, response.message)
    }

    suspend fun search(path: String, keyword: String): ApiResult<List<FileItem>> = runAlist {
        val response = api.search(FsSearchRequest(path = path, keywords = keyword))
        if (response.code == 200) ApiResult.Success(response.data?.content.orEmpty().map { it.toFileItem(path, baseUrl) })
        else ApiResult.Failure(response.code, response.message)
    }

    suspend fun mkdir(path: String): ApiResult<Unit> = runUnit { api.mkdir(MkdirRequest(path)) }
    suspend fun rename(path: String, name: String): ApiResult<Unit> = runUnit { api.rename(RenameRequest(path, name)) }

    suspend fun delete(paths: List<String>): ApiResult<Unit> {
        if (paths.isEmpty()) return ApiResult.Success(Unit)
        val dir = paths.first().substringBeforeLast('/', missingDelimiterValue = "/").ifBlank { "/" }
        val names = paths.map { it.substringAfterLast('/') }
        return runUnit { api.remove(RemoveRequest(dir, names)) }
    }

    private suspend fun runUnit(block: suspend () -> com.textvision.alistclient.network.dto.AlistResponse<Unit>): ApiResult<Unit> = runAlist {
        val response = block()
        if (response.code == 200) ApiResult.Success(Unit) else ApiResult.Failure(response.code, response.message)
    }

    private suspend fun <T> runAlist(block: suspend () -> ApiResult<T>): ApiResult<T> = try { block() } catch (t: Throwable) { ApiResult.NetworkError(t) }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/network app/src/main/java/com/textvision/alistclient/file/FileRepository.kt
git commit -m "feat: add file repository"
```

---

### Task 2.4: FileViewModel state, path cancellation, search debounce, and sorting

**Files:**
- Create: `file/model/FileSort.kt`
- Create: `file/model/FileUiState.kt`
- Create: `file/FileViewModel.kt`
- Test: `file/FileViewModelTest.kt`

**Interfaces:**
- Produces: `FileViewModel.load(path)`, `updateSearchQuery`, `setSort`, `refresh`.
- Produces: `FileUiState.Loading`, `Success`, `Error`.
- Consumes: `FileRepository`.

- [ ] **Step 1: Write ViewModel tests**

Use a fake repository contract for deterministic tests:

```kotlin
package com.textvision.alistclient.file

import app.cash.turbine.test
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import com.textvision.alistclient.file.model.FileUiState
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FileViewModelTest {
    private fun item(name: String, dir: Boolean = false, size: Long = 1) = FileItem(name, "/$name", dir, size, null, null, if (dir) FileType.Folder else FileType.Other, null, null)

    private class FakeRepo : FileRepositoryContract {
        var listResult: ApiResult<List<FileItem>> = ApiResult.Success(listOf(item("b.txt"), item("docs", true), item("a.txt")))
        override suspend fun list(path: String) = listResult
        override suspend fun search(path: String, keyword: String) = ApiResult.Success(listOf(item("match.txt")))
    }

    @Test fun foldersSortFirstByDefault() = runTest {
        val vm = FileViewModel(FakeRepo(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        testScheduler.advanceUntilIdle()
        val state = vm.uiState.value as FileUiState.Success
        assertEquals(listOf("docs", "a.txt", "b.txt"), state.items.map { it.name })
    }

    @Test fun searchDebouncesAndShowsResult() = runTest {
        val vm = FileViewModel(FakeRepo(), StandardTestDispatcher(testScheduler))
        vm.load("/")
        vm.updateSearchQuery("match")
        testScheduler.advanceTimeBy(299)
        assertEquals("match", vm.searchQuery.value)
        testScheduler.advanceTimeBy(1)
        testScheduler.advanceUntilIdle()
        val state = vm.uiState.value as FileUiState.Success
        assertEquals(listOf("match.txt"), state.items.map { it.name })
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileViewModelTest"
```

Expected: FAIL because ViewModel does not exist.

- [ ] **Step 3: Implement state and ViewModel**

`FileSort.kt`:

```kotlin
package com.textvision.alistclient.file.model

enum class FileSort { NameAsc, SizeDesc, ModifiedDesc }
```

`FileUiState.kt`:

```kotlin
package com.textvision.alistclient.file.model

import com.textvision.alistclient.common.error.AppError

sealed interface FileUiState {
    data class Loading(val path: String) : FileUiState
    data class Success(
        val path: String,
        val items: List<FileItem>,
        val selectedItems: Set<String> = emptySet(),
        val isMultiSelectMode: Boolean = false,
        val isCurrentDirectoryFilter: Boolean = false,
    ) : FileUiState
    data class Error(val path: String, val error: AppError) : FileUiState
}
```

`FileViewModel.kt`:

```kotlin
package com.textvision.alistclient.file

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.error.ErrorMapper
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileSort
import com.textvision.alistclient.file.model.FileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

interface FileRepositoryContract {
    suspend fun list(path: String): ApiResult<List<FileItem>>
    suspend fun search(path: String, keyword: String): ApiResult<List<FileItem>>
}

@OptIn(FlowPreview::class)
@HiltViewModel
class FileViewModel @Inject constructor(
    private val repository: FileRepositoryContract,
) : ViewModel() {
    constructor(repository: FileRepositoryContract, dispatcher: CoroutineDispatcher) : this(repository) { this.dispatcher = dispatcher; observeSearch() }

    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private var loadJob: Job? = null
    private var currentPath: String = "/"
    private var sort: FileSort = FileSort.NameAsc
    private val _uiState = MutableStateFlow<FileUiState>(FileUiState.Loading("/"))
    val uiState: StateFlow<FileUiState> = _uiState.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init { observeSearch() }

    fun load(path: String) {
        currentPath = path
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            _uiState.value = FileUiState.Loading(path)
            when (val result = repository.list(path)) {
                is ApiResult.Success -> _uiState.value = FileUiState.Success(path, applySort(result.data))
                is ApiResult.Failure -> _uiState.value = FileUiState.Error(path, ErrorMapper.mapAlistFailure(result.code, result.message))
                is ApiResult.NetworkError -> _uiState.value = FileUiState.Error(path, ErrorMapper.mapThrowable(result.cause))
            }
        }
    }

    fun refresh() = load(currentPath)

    fun updateSearchQuery(value: String) { _searchQuery.value = value }

    fun setSort(value: FileSort) {
        sort = value
        val current = _uiState.value
        if (current is FileUiState.Success) _uiState.value = current.copy(items = applySort(current.items))
    }

    private fun observeSearch() {
        _searchQuery.debounce(300).distinctUntilChanged().onEach { query ->
            if (query.isBlank()) load(currentPath) else search(query.trim())
        }.launchIn(viewModelScope)
    }

    private fun search(query: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            when (val result = repository.search(currentPath, query)) {
                is ApiResult.Success -> _uiState.value = FileUiState.Success(currentPath, applySort(result.data))
                is ApiResult.Failure -> {
                    val current = (_uiState.value as? FileUiState.Success)?.items.orEmpty()
                    _uiState.value = FileUiState.Success(currentPath, current.filter { it.name.contains(query, ignoreCase = true) }, isCurrentDirectoryFilter = true)
                }
                is ApiResult.NetworkError -> {
                    val current = (_uiState.value as? FileUiState.Success)?.items.orEmpty()
                    _uiState.value = FileUiState.Success(currentPath, current.filter { it.name.contains(query, ignoreCase = true) }, isCurrentDirectoryFilter = true)
                }
            }
        }
    }

    private fun applySort(items: List<FileItem>): List<FileItem> = when (sort) {
        FileSort.NameAsc -> items.sortedWith(compareByDescending<FileItem> { it.isDir }.thenBy { it.name.lowercase() })
        FileSort.SizeDesc -> items.sortedWith(compareByDescending<FileItem> { it.isDir }.thenByDescending { it.size })
        FileSort.ModifiedDesc -> items.sortedWith(compareByDescending<FileItem> { it.isDir }.thenByDescending { it.modifiedAt })
    }
}
```

Modify `FileRepository` to implement `FileRepositoryContract`.

- [ ] **Step 4: Run tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.file.FileViewModelTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/file app/src/test/java/com/textvision/alistclient/file/FileViewModelTest.kt
git commit -m "feat: add file view model"
```

---

### Task 2.5: Build FileScreen UI with breadcrumbs, search, sort, and empty/error states

**Files:**
- Create: `ui/components/FileTypeIcon.kt`
- Create: `ui/components/BreadcrumbBar.kt`
- Modify: `ui/screens/FileScreen.kt`

**Interfaces:**
- Produces: File screen showing `未知大小`, current directory filter label, retry affordance, and root load.
- Consumes: `FileViewModel`, `FileUiState`.

- [ ] **Step 1: Add UI components**

`FileTypeIcon.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.textvision.alistclient.file.model.FileType

@Composable
fun FileTypeIcon(type: FileType) {
    val icon = when (type) {
        FileType.Folder -> Icons.Default.Folder
        FileType.Image -> Icons.Default.Image
        FileType.Text -> Icons.Default.Article
        else -> Icons.Default.InsertDriveFile
    }
    Icon(icon, contentDescription = null)
}
```

`BreadcrumbBar.kt`:

```kotlin
package com.textvision.alistclient.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BreadcrumbBar(path: String, onNavigate: (String) -> Unit) {
    val parts = path.trim('/').split('/').filter { it.isNotBlank() }
    Row(Modifier.horizontalScroll(rememberScrollState())) {
        TextButton(onClick = { onNavigate("/") }) { Text("/") }
        var current = ""
        parts.forEach { part ->
            current += "/$part"
            TextButton(onClick = { onNavigate(current) }) { Text(part) }
        }
    }
}
```

- [ ] **Step 2: Replace FileScreen**

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.common.error.ErrorMessageMapper
import com.textvision.alistclient.file.FileViewModel
import com.textvision.alistclient.file.model.FileUiState
import com.textvision.alistclient.ui.components.BreadcrumbBar
import com.textvision.alistclient.ui.components.FileTypeIcon

@Composable
fun FileScreen(viewModel: FileViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load("/") }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        val currentPath = when (val s = state) {
            is FileUiState.Loading -> s.path
            is FileUiState.Success -> s.path
            is FileUiState.Error -> s.path
        }
        BreadcrumbBar(currentPath, viewModel::load)
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::updateSearchQuery,
            label = { Text("搜索") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        when (val s = state) {
            is FileUiState.Loading -> CircularProgressIndicator()
            is FileUiState.Error -> Column {
                Text(ErrorMessageMapper.toUserMessage(s.error))
                Button(onClick = viewModel::refresh) { Text("重试") }
            }
            is FileUiState.Success -> {
                if (s.isCurrentDirectoryFilter) Text("当前目录搜索结果")
                if (s.items.isEmpty()) Text("这里没有文件")
                LazyColumn {
                    items(s.items, key = { it.path }) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text(if (!item.isDir && item.size == 0L) "未知大小" else if (item.isDir) "文件夹" else "${item.size} B") },
                            leadingContent = { FileTypeIcon(item.type) },
                            modifier = Modifier.clickable { if (item.isDir) viewModel.load(item.path) }
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui app/src/main/java/com/textvision/alistclient/file
git commit -m "feat: add file browsing screen"
```

---

## Phase 2 Completion Gate

Run:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

Manual check with a logged-in session or mocked server:

1. Open File tab.
2. Verify root path loads or error state has retry.
3. Enter search text; verify UI waits roughly 300ms before changing result.
4. Verify `size=0` non-directory file displays `未知大小`.
