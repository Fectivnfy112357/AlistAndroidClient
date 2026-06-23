# Phase 4 Preview Sharing and Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete user-facing preview, open, share, link-share, settings, crash log, and network status behavior.

**Architecture:** `PreviewRouter` decides what to do by `FileType`. Local temp preview files live in `cacheDir/preview/`. FileProvider shares only `downloads/` and `preview/`. Settings delegates destructive actions to repositories/managers.

**Tech Stack:** Compose, Coil 3, Android Intent/FileProvider, Hilt, ConnectivityManager, Room, AndroidX lifecycle.

## Global Constraints

- Text preview limit = 2MB.
- Preview temp files are cleaned only by Settings manual action or system cache eviction.
- Link share text: `此链接需要登录 Alist 账号才能访问。如果服务器在内网，对方可能无法打开。`
- FileProvider grants read-only URI permission.
- Logout clears credentials, cancels transfers, deletes Room transfer rows, preserves `filesDir/downloads/`, clears `cacheDir/preview/`.
- Crash logs live in `filesDir/crash_logs`, retain newest 10.

---

## File Structure

Create:

```text
app/src/main/java/com/textvision/alistclient/preview/PreviewRouter.kt
app/src/main/java/com/textvision/alistclient/preview/PreviewFileStore.kt
app/src/main/java/com/textvision/alistclient/preview/MimeTypeResolver.kt
app/src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt
app/src/main/java/com/textvision/alistclient/common/crash/SafeCrashHandler.kt
app/src/main/java/com/textvision/alistclient/common/network/NetworkMonitor.kt
app/src/test/java/com/textvision/alistclient/preview/MimeTypeResolverTest.kt
app/src/test/java/com/textvision/alistclient/common/crash/SafeCrashHandlerTest.kt
```

Modify:

```text
app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt
app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt
app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt
app/src/main/java/com/textvision/alistclient/AlistClientApp.kt
app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt
app/src/main/java/com/textvision/alistclient/auth/AuthRepository.kt
```

---

### Task 4.1: MIME type resolver and preview file store

**Files:**
- Create: `preview/MimeTypeResolver.kt`
- Create: `preview/PreviewFileStore.kt`
- Test: `preview/MimeTypeResolverTest.kt`

**Interfaces:**
- Produces: `MimeTypeResolver.infer(name): String`.
- Produces: `PreviewFileStore.previewDir()`, `clearPreviewFiles()`.
- Consumes: Android context.

- [ ] **Step 1: Write MIME tests**

```kotlin
package com.textvision.alistclient.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class MimeTypeResolverTest {
    @Test fun infersKnownTypesAndFallsBack() {
        assertEquals("image/jpeg", MimeTypeResolver.infer("cat.jpg"))
        assertEquals("text/plain", MimeTypeResolver.infer("note.txt"))
        assertEquals("application/octet-stream", MimeTypeResolver.infer("file.unknownext"))
        assertEquals("application/octet-stream", MimeTypeResolver.infer("file"))
    }
}
```

- [ ] **Step 2: Run test and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.preview.MimeTypeResolverTest"
```

Expected: FAIL because resolver does not exist.

- [ ] **Step 3: Implement resolver and file store**

`MimeTypeResolver.kt`:

```kotlin
package com.textvision.alistclient.preview

import android.webkit.MimeTypeMap

object MimeTypeResolver {
    fun infer(name: String): String {
        val ext = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        if (ext.isBlank()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }
}
```

`PreviewFileStore.kt`:

```kotlin
package com.textvision.alistclient.preview

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreviewFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun previewDir(): File = File(context.cacheDir, "preview").also { it.mkdirs() }

    fun clearPreviewFiles(): Int {
        val files = previewDir().listFiles().orEmpty()
        files.forEach { it.deleteRecursively() }
        return files.size
    }
}
```

- [ ] **Step 4: Run tests and build**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.preview.MimeTypeResolverTest" :app:assembleDebug
```

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/preview app/src/test/java/com/textvision/alistclient/preview/MimeTypeResolverTest.kt
git commit -m "feat: add preview file utilities"
```

---

### Task 4.2: Preview router and PreviewScreen

**Files:**
- Create: `preview/PreviewRouter.kt`
- Create: `ui/screens/PreviewScreen.kt`
- Modify: `navigation/AppRoute.kt`
- Modify: `navigation/AppNavHost.kt`

**Interfaces:**
- Produces: text preview screen for files <= 2MB.
- Produces: external open intent helper for other file types.
- Consumes: FileProvider authority `${applicationId}.fileprovider`.

- [ ] **Step 1: Implement PreviewRouter**

```kotlin
package com.textvision.alistclient.preview

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.textvision.alistclient.file.model.FileItem
import com.textvision.alistclient.file.model.FileType
import java.io.File

sealed interface PreviewAction {
    data class InAppText(val file: File) : PreviewAction
    data class InAppImage(val uriString: String) : PreviewAction
    data class ExternalOpen(val intent: Intent) : PreviewAction
    data class TooLargeText(val file: File) : PreviewAction
}

object PreviewRouter {
    const val TEXT_PREVIEW_LIMIT_BYTES = 2L * 1024L * 1024L

    fun route(context: Context, item: FileItem, localFile: File): PreviewAction {
        return when (item.type) {
            FileType.Text -> if (localFile.length() <= TEXT_PREVIEW_LIMIT_BYTES) PreviewAction.InAppText(localFile) else PreviewAction.TooLargeText(localFile)
            FileType.Image -> PreviewAction.InAppImage(localFile.toURI().toString())
            else -> PreviewAction.ExternalOpen(openIntent(context, localFile, item.name))
        }
    }

    fun openIntent(context: Context, file: File, name: String): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MimeTypeResolver.infer(name))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
```

- [ ] **Step 2: Add PreviewScreen**

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PreviewScreen(filePath: String, onDownload: () -> Unit, onExternalOpen: () -> Unit) {
    val file = File(filePath)
    val text = produceState(initialValue = "加载中", filePath) {
        value = runCatching { file.readText() }.getOrElse { "无法读取文件" }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (file.length() > 2L * 1024L * 1024L) {
            Text("文件过大，是否下载或用其他应用打开？")
            Button(onClick = onDownload) { Text("下载") }
            Button(onClick = onExternalOpen) { Text("外部打开") }
        } else {
            Text(text.value, modifier = Modifier.verticalScroll(rememberScrollState()))
        }
    }
}
```

- [ ] **Step 3: Add preview route**

In `AppRoute.kt` add:

```kotlin
data object Preview : AppRoute("preview/{filePath}") {
    fun create(filePath: String): String = "preview/${android.net.Uri.encode(filePath)}"
}
```

In `AppNavHost.kt`, add imports:

```kotlin
import androidx.navigation.NavType
import androidx.navigation.navArgument
import android.net.Uri
```

Then add this composable inside `NavHost`:

```kotlin
composable(
    route = AppRoute.Preview.route,
    arguments = listOf(navArgument("filePath") { type = NavType.StringType })
) { entry ->
    val encoded = requireNotNull(entry.arguments?.getString("filePath"))
    val filePath = Uri.decode(encoded)
    PreviewScreen(
        filePath = filePath,
        onDownload = { navController.popBackStack() },
        onExternalOpen = { navController.popBackStack() },
    )
}
```

- [ ] **Step 4: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/preview app/src/main/java/com/textvision/alistclient/ui/screens/PreviewScreen.kt app/src/main/java/com/textvision/alistclient/navigation
git commit -m "feat: add preview routing"
```

---

### Task 4.3: System share and link share helpers

**Files:**
- Modify: `preview/PreviewRouter.kt`

**Interfaces:**
- Produces: `shareFileIntent(context, file, name): Intent`.
- Produces: `shareLinkIntent(link): Intent` with exact warning copy exposed to UI.
- Consumes: `MimeTypeResolver`.

- [ ] **Step 1: Add share helpers**

Append to `PreviewRouter`:

```kotlin
const val LINK_SHARE_WARNING = "此链接需要登录 Alist 账号才能访问。如果服务器在内网，对方可能无法打开。"

fun shareFileIntent(context: Context, file: File, name: String): Intent {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    return Intent(Intent.ACTION_SEND).apply {
        type = MimeTypeResolver.infer(name)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

fun shareLinkIntent(link: String): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, "$link\n\n$LINK_SHARE_WARNING")
}
```

- [ ] **Step 2: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/preview/PreviewRouter.kt
git commit -m "feat: add share intent helpers"
```

---

### Task 4.4: Settings logout, preview cleanup, and crash log export

**Files:**
- Create: `common/crash/SafeCrashHandler.kt`
- Modify: `AlistClientApp.kt`
- Modify: `ui/screens/SettingsScreen.kt`
- Modify: `auth/AuthRepository.kt`
- Modify: `transfer/TransferManager.kt`

**Interfaces:**
- Produces: Settings screen actions: logout, clear preview files, export crash logs.
- Produces: crash logs in `filesDir/crash_logs`, max 10 files.
- Consumes: `PreviewFileStore`, `AuthRepository`, `TransferManager`.

- [ ] **Step 1: Add crash handler**

`SafeCrashHandler.kt`:

```kotlin
package com.textvision.alistclient.common.crash

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class SafeCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        runCatching {
            val crashDir = File(context.filesDir, "crash_logs").also { it.mkdirs() }
            val file = File(crashDir, "crash-${System.currentTimeMillis()}-${thread.name}.log")
            file.writeText(sanitizeStackTrace(throwable))
            crashDir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(10)?.forEach { it.delete() }
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    fun sanitizeStackTrace(e: Throwable): String = StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
        .replace(Regex("Bearer [\\w\\-\\.]+"), "Bearer ***")
        .replace(Regex("password[\"']?\\s*[:=]\\s*[\"']?[\\w]+"), "password=***")
        .replace(Regex("token[\"']?\\s*[:=]\\s*[\"']?[\\w\\-\\.]+"), "token=***")
}
```

- [ ] **Step 2: Install crash handler**

Modify `AlistClientApp.kt`:

```kotlin
override fun onCreate() {
    super.onCreate()
    Thread.setDefaultUncaughtExceptionHandler(
        SafeCrashHandler(this, Thread.getDefaultUncaughtExceptionHandler())
    )
}
```

- [ ] **Step 3: Add logout cleanup methods**

In `TransferManager` add:

```kotlin
fun clearAllTasks() {
    scope.launch { dao.deleteAll() }
}
```

In `AuthRepository.logout()`, keep clearing credentials. Settings will call both `authRepository.logout()` and `transferManager.clearAllTasks()`.

- [ ] **Step 4: Replace SettingsScreen**

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transferManager: TransferManager,
    private val previewFileStore: PreviewFileStore,
) : ViewModel() {
    fun logout() {
        authRepository.logout()
        transferManager.clearAllTasks()
        previewFileStore.clearPreviewFiles()
    }

    fun clearPreviewFiles(): Int = previewFileStore.clearPreviewFiles()
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val message = remember { mutableStateOf<String?>(null) }
    Column(Modifier.padding(16.dp)) {
        Text("设置")
        Button(onClick = { viewModel.logout(); message.value = "已退出登录" }) { Text("退出登录") }
        Button(onClick = { val count = viewModel.clearPreviewFiles(); message.value = "已清理 $count 个临时文件" }) { Text("清理临时预览文件") }
        Text("多账号、管理员、外部网盘管理不在 MVP 范围内")
        message.value?.let { Text(it) }
    }
}
```

- [ ] **Step 5: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/common/crash app/src/main/java/com/textvision/alistclient/AlistClientApp.kt app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt app/src/main/java/com/textvision/alistclient/transfer app/src/main/java/com/textvision/alistclient/auth
git commit -m "feat: add settings cleanup and crash logs"
```

---

### Task 4.5: NetworkMonitor and offline banner

**Files:**
- Create: `common/network/NetworkMonitor.kt`
- Modify: `di/AppModule.kt`
- Modify: `ui/screens/FileScreen.kt`

**Interfaces:**
- Produces: `NetworkMonitor.isOnline: StateFlow<Boolean>`.
- Consumes: Android ConnectivityManager.

- [ ] **Step 1: Implement NetworkMonitor**

```kotlin
package com.textvision.alistclient.common.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkMonitor @Inject constructor(@ApplicationContext context: Context) {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { _isOnline.value = true }
                override fun onLost(network: Network) { _isOnline.value = false }
            }
        )
    }
}
```

- [ ] **Step 2: Display offline banner in FileScreen**

Inject a small Hilt ViewModel or pass `NetworkMonitor` through existing `FileViewModel`. Add `val isOnline by networkMonitor.isOnline.collectAsStateWithLifecycle()` and render:

```kotlin
if (!isOnline) {
    Text("当前无网络", color = MaterialTheme.colorScheme.error)
}
```

- [ ] **Step 3: Build**

```bash
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/common/network app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt
git commit -m "feat: add network status banner"
```

---

## Phase 4 Completion Gate

Run:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

Manual checks:

1. Text file > 2MB shows “下载 / 外部打开”.
2. Link sharing includes exact warning text.
3. Settings clear preview deletes `cacheDir/preview/` files.
4. Logout clears credentials and transfer rows but keeps `filesDir/downloads/`.
