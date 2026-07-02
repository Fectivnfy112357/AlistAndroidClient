# 首页 Dashboard 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a bottom-tab "首页" screen that shows Alist server info (title, version, uptime), total disk usage, and a list of storage mounts — with graceful degradation for guest users and retryable error state.

**Architecture:** New `home/` package (DTOs, repository, ViewModel, Screen) wired into the existing single-module MVVM. Reuses the HyperOS component library (`CloudScaffold`, `CloudCard`, `CloudStatusBanner`, `CloudTopBar`, `CloudBottomBar`). Concurrently calls `admin/info` + `admin/storage/list`; on 401/403 falls back to `public/settings`; on network failure falls back further; on full failure renders `Error` UiState with retry. `AppRoute.Files` gets a `path` query param so storage cards can deep-link to a directory.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose, Material3 1.3+ (PullToRefreshBox), Hilt 2.52, kotlinx-serialization, kotlinx-datetime (Instant), Retrofit 2.11, MockWebServer, MockK, Turbine, StandardTestDispatcher.

**Spec:** `docs/superpowers/specs/2026-07-02-home-dashboard-design.md`
**Mocks:** `docs/superpowers/mocks/home-dashboard-A-admin.html` (canonical), `home-dashboard-B-guest.html`, `home-dashboard-C-load-error.html`

## Global Constraints

- minSdk 26, compileSdk/targetSdk 34, JVM target 17 (per `app/build.gradle.kts`).
- Hilt singleton bindings live in `di/AppModule.kt::CredentialModule`; new repo follows the `Interface in production class, bound via @Binds` pattern.
- All API calls wrap exceptions in `ApiResult.NetworkError(cause)`; rethrow `CancellationException` unchanged (see `FileRepository.runAlist`).
- 401 responses must trigger one re-login + retry pass; 403 must not (guest cannot re-auth as admin). Implemented by re-using `FileRepository.runAlistWithRefresh` semantics inside the new repo.
- All public Composables go through `CloudScaffold`; no raw `Scaffold`/`TopAppBar`. Existing `CloudShapes.Card` (24.dp) and `CloudShapes.Control` (18.dp) cover all new cards.
- Route `AppRoute.Files` changes from `"files"` to `"files?path={path}"`; `create(path: String = "/")` helper encodes via `android.net.Uri.encode`. `AppNavHost` must use `navArgument("path") { type = NavType.StringType; defaultValue = "/" }`.
- Home is the first item in `CloudBottomBar`; `AppRoute.Home("home")` and `showBottomBar` set must include it.
- Strings: reuse existing copy pattern ("首页", "重试", "存储详情不可用"). New copy must use Simplified Chinese.
- Tests: JUnit 4, MockK for ViewModel, MockWebServer for Repository. UI tests use Compose `createComposeRule()`. Each task ends with `./gradlew :app:testDebugUnitTest` passing.
- The actual Alist JSON field names for `admin/info` and `admin/storage/list` are not yet confirmed; DTOs in this plan use `@SerialName` defensively, and the implementation task runs a real-server sanity check before locking the schema.

## File Structure

New files (this plan):

```
app/src/main/java/com/textvision/alistclient/home/
  dto/AdminDtos.kt                # AdminInfo, StorageList, StorageInfo, PublicSettings, HomeData, HomeFailure
  HomeRepositoryContract.kt       # interface
  HomeRepository.kt               # @Singleton impl
  HomeUiState.kt                  # sealed interface
  HomeViewModel.kt                # @HiltViewModel
  HomeScreen.kt                   # @Composable + private sub-Composables
  HomeRouteTest.kt                # path encoding assertions (task 6)
app/src/test/java/com/textvision/alistclient/home/
  HomeRepositoryTest.kt
  HomeViewModelTest.kt
```

Modified files:

- `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt` — add 3 endpoints
- `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt` (new) — DTOs
- `app/src/main/java/com/textvision/alistclient/di/AppModule.kt` — `@Binds` for HomeRepository
- `app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt` — add `Home` + change `Files` to `files?path={path}`
- `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt` — add Home composable, change Files composable to read `path` arg
- `app/src/main/java/com/textvision/alistclient/ui/components/CloudBottomBar.kt` — insert Home as first item
- `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt` — accept optional `initialPath: String?`

---

## Task 1: Admin/Storage/Public DTOs + AlistApi endpoints

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt:19-43`

**Interfaces:**
- Consumes: `AlistResponse<T>` (existing)
- Produces: `AdminInfo`, `StorageList`, `StorageInfo`, `PublicSettings` data classes; new methods `adminInfo`, `listStorage`, `getPublicSettings` on `AlistApi`.

**Risk acknowledged:** The JSON keys for `admin/info` and `admin/storage/list` are best-guess. The DTOs are written with `@SerialName` for the most likely names. A follow-up pass (after Task 1 lands) inspects a real Alist v3 server or its source and adjusts annotations.

- [ ] **Step 1: Create AdminDtos.kt with defensive @SerialName mappings**

Create `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`:

```kotlin
package com.textvision.alistclient.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * /api/admin/info -> data
 *
 * Field names follow Alist v3 source. Some servers return the totals
 * inside the `data` payload as `used_bytes` / `total_bytes`; others
 * nest them in a `data.usage` object. The mapping below covers the
 * flat form which is the documented public shape.
 */
@Serializable
data class AdminInfo(
    @SerialName("version") val version: String? = null,
    @SerialName("build_date") val buildDate: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("used_bytes") val usedBytes: Long = 0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
)

@Serializable
data class StorageList(
    @SerialName("content") val content: List<StorageInfo> = emptyList(),
    @SerialName("total") val total: Int = 0,
)

@Serializable
data class StorageInfo(
    @SerialName("id") val id: Long? = null,
    @SerialName("mount_path") val mountPath: String,
    @SerialName("driver") val driver: String = "",
    @SerialName("status") val status: String? = null,
    @SerialName("used_bytes") val usedBytes: Long = 0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
)

/**
 * /api/public/settings -> data
 */
@Serializable
data class PublicSettings(
    @SerialName("title") val title: String? = null,
    @SerialName("logo") val logo: String? = null,
    @SerialName("version") val version: String? = null,
)
```

- [ ] **Step 2: Add a failing serializer round-trip test**

Create `app/src/test/java/com/textvision/alistclient/network/dto/AdminDtosTest.kt`:

```kotlin
package com.textvision.alistclient.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AdminDtosTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test fun adminInfoMapsFlatKeys() {
        val raw = """
            {"version":"v3.25.0","build_date":"2025-09-01","start_time":"2026-07-01T00:00:00Z",
             "used_bytes":1234,"total_bytes":5678}
        """.trimIndent()
        val info = json.decodeFromString<AdminInfo>(raw)
        assertEquals("v3.25.0", info.version)
        assertEquals("2025-09-01", info.buildDate)
        assertEquals("2026-07-01T00:00:00Z", info.startTime)
        assertEquals(1234L, info.usedBytes)
        assertEquals(5678L, info.totalBytes)
    }

    @Test fun storageInfoMapsSnakeCaseKeys() {
        val raw = """
            {"id":1,"mount_path":"/local","driver":"Local","status":"work",
             "used_bytes":100,"total_bytes":200}
        """.trimIndent()
        val info = json.decodeFromString<StorageInfo>(raw)
        assertEquals(1L, info.id)
        assertEquals("/local", info.mountPath)
        assertEquals("Local", info.driver)
        assertEquals("work", info.status)
        assertEquals(100L, info.usedBytes)
        assertEquals(200L, info.totalBytes)
    }

    @Test fun storageInfoToleratesMissingOptionalFields() {
        val raw = """{"mount_path":"/x"}"""
        val info = json.decodeFromString<StorageInfo>(raw)
        assertEquals("/x", info.mountPath)
        assertEquals("", info.driver)
        assertEquals(null, info.status)
        assertEquals(0L, info.usedBytes)
    }

    @Test fun publicSettingsMapsKnownKeys() {
        val raw = """{"title":"My Alist","logo":"/logo.svg","version":"v3.25.0"}"""
        val s = json.decodeFromString<PublicSettings>(raw)
        assertEquals("My Alist", s.title)
        assertEquals("/logo.svg", s.logo)
        assertEquals("v3.25.0", s.version)
    }
}
```

- [ ] **Step 3: Run test to verify it passes (DTO logic exists)**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.network.dto.AdminDtosTest`
Expected: 4 tests pass.

- [ ] **Step 4: Add 3 endpoint methods to AlistApi**

Edit `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`. Add imports and 3 methods. The interface uses `@Url` for all endpoints (matching existing pattern) and `@POST` for the admin endpoints, `@GET` for public/settings.

Insert after the `move` method (line 42), before the closing `}`:

```kotlin
    @POST
    suspend fun adminInfo(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String, @Body request: com.textvision.alistclient.network.dto.AdminInfoRequest = com.textvision.alistclient.network.dto.AdminInfoRequest()): AlistResponse<AdminInfo>

    @POST
    suspend fun listStorage(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String, @Body request: com.textvision.alistclient.network.dto.StorageListRequest = com.textvision.alistclient.network.dto.StorageListRequest()): AlistResponse<StorageList>

    @GET
    suspend fun getPublicSettings(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String = SkipAuthRetry.HEADER): AlistResponse<PublicSettings>
```

Add imports at the top of the same file:

```kotlin
import com.textvision.alistclient.network.dto.AdminInfo
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageList
import retrofit2.http.GET
```

Now create the request DTOs. Append to `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`:

```kotlin
@Serializable
data class AdminInfoRequest(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
)

@Serializable
data class StorageListRequest(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
)
```

- [ ] **Step 5: Update test fakes (AlistApi stubs) to satisfy the new abstract methods**

Several test fakes implement `AlistApi` and currently throw `UnsupportedOperationException` for everything they don't use. The new abstract methods force a choice: either throw, or implement. Because the new methods are tested directly in Tasks 2+ and we don't want false-positive calls, **leave the test fakes throwing** `UnsupportedOperationException` and add the new method signatures to each `throw` block.

Files to update (each has a `class FakeApi : AlistApi` or similar with stub methods):

- `app/src/test/java/com/textvision/alistclient/file/FileRepositoryTest.kt:34` (RefreshingApi)
- `app/src/test/java/com/textvision/alistclient/auth/AuthRepositoryTest.kt:37` (FakeApi) and `:48` (CancellingApi)
- `app/src/test/java/com/textvision/alistclient/auth/LoginViewModelTest.kt` — does not implement AlistApi; skip.

In each, add the 3 new override methods (after the `move` override) that throw `UnsupportedOperationException("adminInfo/listStorage/getPublicSettings is not used by this test")`. Copy them verbatim across all fakes so the project compiles.

```kotlin
        override suspend fun adminInfo(url: String, skipAuthRetry: String, request: com.textvision.alistclient.network.dto.AdminInfoRequest): AlistResponse<com.textvision.alistclient.network.dto.AdminInfo> = throw UnsupportedOperationException("adminInfo is not used by this test")
        override suspend fun listStorage(url: String, skipAuthRetry: String, request: com.textvision.alistclient.network.dto.StorageListRequest): AlistResponse<com.textvision.alistclient.network.dto.StorageList> = throw UnsupportedOperationException("listStorage is not used by this test")
        override suspend fun getPublicSettings(url: String, skipAuthRetry: String): AlistResponse<com.textvision.alistclient.network.dto.PublicSettings> = throw UnsupportedOperationException("getPublicSettings is not used by this test")
```

Note: The signature for `getPublicSettings` in `AlistApi` has the default value on the parameter; override methods do not repeat defaults. Keep `@Header(SkipAuthRetry.HEADER) skipAuthRetry: String` (no default) in the override.

- [ ] **Step 6: Build + run all unit tests to confirm the AlistApi change compiles**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest`
Expected: all green. Any `FakeApi`/`RefreshingApi` you missed surfaces as a compile error.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt \
        app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt \
        app/src/test/java/com/textvision/alistclient/network/dto/AdminDtosTest.kt \
        app/src/test/java/com/textvision/alistclient/file/FileRepositoryTest.kt \
        app/src/test/java/com/textvision/alistclient/auth/AuthRepositoryTest.kt
git commit -m "feat(home): add admin/storage/public DTOs and AlistApi endpoints"
```

---

## Task 2: HomeRepository — concurrent admin calls + 401/403 fallback

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/home/HomeRepositoryContract.kt`
- Create: `app/src/main/java/com/textvision/alistclient/home/HomeRepository.kt`
- Create: `app/src/main/java/com/textvision/alistclient/home/dto/HomeData.kt`

**Interfaces:**
- Consumes: `AlistApi` (3 new methods from Task 1), `AuthRepository` (for the 401 re-login; the existing `FileRepository.refreshSession` pattern), `SessionManager` (for `serverUrl`).
- Produces: `HomeRepositoryContract.loadDashboard(): HomeData`. `HomeData` is a sealed hierarchy (see Step 1) carrying all info `HomeScreen` needs.

- [ ] **Step 1: Define HomeData + HomeFailure in `home/dto/HomeData.kt`**

Create `app/src/main/java/com/textvision/alistclient/home/dto/HomeData.kt`:

```kotlin
package com.textvision.alistclient.home.dto

import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageInfo
import kotlinx.datetime.Instant

/**
 * All values `HomeScreen` needs, regardless of which combination of
 * endpoints succeeded. `isGuest` is true when at least one admin call
 * returned 401/403 and we fell back to public/settings.
 */
sealed interface HomeData {
    val serverTitle: String
    val serverVersion: String?
    val isGuest: Boolean

    data class Admin(
        override val serverTitle: String,
        override val serverVersion: String?,
        val startTime: Instant?,
        val usedBytes: Long,
        val totalBytes: Long,
        val storages: List<StorageInfo>,
        override val isGuest: Boolean = false,
    ) : HomeData

    data class Guest(
        override val serverTitle: String,
        override val serverVersion: String?,
        val publicSettings: PublicSettings,
    ) : HomeData {
        override val isGuest: Boolean = true
    }
}

sealed interface HomeFailure {
    /** public/settings also failed; show Error UiState with retry */
    data class Unreachable(val message: String) : HomeFailure
}
```

- [ ] **Step 2: Write the contract**

Create `app/src/main/java/com/textvision/alistclient/home/HomeRepositoryContract.kt`:

```kotlin
package com.textvision.alistclient.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.home.dto.HomeData

interface HomeRepositoryContract {
    suspend fun loadDashboard(): ApiResult<HomeData>
}
```

- [ ] **Step 3: Write the failing test using MockWebServer**

Create `app/src/test/java/com/textvision/alistclient/home/HomeRepositoryTest.kt`:

```kotlin
package com.textvision.alistclient.home

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AlistApi
    private lateinit var store: MemoryStore
    private lateinit var session: SessionManager
    private lateinit var tokenProvider: AuthTokenProvider
    private lateinit var repo: HomeRepository

    private class MemoryStore : CredentialStore {
        val map = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { map[key] = value }
        override fun readString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        store = MemoryStore().apply {
            map[SessionManager.KEY_SERVER_URL] = server.url("/").toString()
            map[SessionManager.KEY_USERNAME] = "admin"
            map[SessionManager.KEY_PASSWORD] = "pass"
            map[SessionManager.KEY_TOKEN] = "tok"
        }
        tokenProvider = AuthTokenProvider()
        session = SessionManager(store, tokenProvider)
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenProvider)).build()
        api = retrofit2.Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AlistApi::class.java)
        repo = HomeRepository(api, session, AuthRepository(api, session))
    }

    @After fun tearDown() { server.shutdown() }

    private fun enqueue(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    @Test fun adminSuccessReturnsAdminData() = runTest {
        enqueue("""{"code":200,"message":"success","data":{"version":"v3.25.0","start_time":"2026-07-01T00:00:00Z","used_bytes":100,"total_bytes":200}}""")
        enqueue("""{"code":200,"message":"success","data":{"content":[{"mount_path":"/local","driver":"Local","used_bytes":50,"total_bytes":100}],"total":1}}""")

        val result = repo.loadDashboard() as ApiResult.Success

        val data = result.data as HomeData.Admin
        assertEquals(false, data.isGuest)
        assertEquals(1, data.storages.size)
        assertEquals("/local", data.storages.first().mountPath)
        // public endpoint should NOT have been hit
        assertEquals(2, server.requestCount)
    }

    @Test fun admin401FallsBackToPublicAndMarksGuest() = runTest {
        enqueue("""{"code":401,"message":"unauthorized","data":null}""")
        enqueue("""{"code":401,"message":"unauthorized","data":null}""")
        enqueue("""{"code":200,"message":"success","data":{"title":"My Alist","version":"v3.25.0"}}""")

        val result = repo.loadDashboard() as ApiResult.Success
        val data = result.data as HomeData.Guest

        assertEquals(true, data.isGuest)
        assertEquals("My Alist", data.serverTitle)
        assertEquals("v3.25.0", data.serverVersion)
        assertEquals(3, server.requestCount)
    }

    @Test fun adminNetworkErrorFallsBackToPublic() = runTest {
        server.shutdown() // public will be called on a fresh server below
        // emulate the public fallback path: open a new server for public
        val publicServer = MockWebServer().apply { start() }
        try {
            // point the stored URL at the new server
            store.map[SessionManager.KEY_SERVER_URL] = publicServer.url("/").toString()
            publicServer.enqueue(MockResponse().setSocketError()) // admin fails with network error
            // Re-enqueue the same socket error to the other admin endpoint
            publicServer.enqueue(MockResponse().setSocketError())
            publicServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":{"title":"Public","version":"v3"}}"""))

            val result = repo.loadDashboard() as ApiResult.Success
            assertTrue(result.data is HomeData.Guest)
        } finally { publicServer.shutdown() }
    }

    @Test fun adminAndPublicAllFailReturnsFailure() = runTest {
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":500,"message":"oops","data":null}""")

        val result = repo.loadDashboard()

        assertTrue(result is ApiResult.Failure)
        assertEquals(500, (result as ApiResult.Failure).code)
    }
}
```

- [ ] **Step 4: Run test to verify it fails to compile (no implementation yet)**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRepositoryTest`
Expected: compile error — `HomeRepository` does not exist yet.

- [ ] **Step 5: Implement `HomeRepository`**

Create `app/src/main/java/com/textvision/alistclient/home/HomeRepository.kt`:

```kotlin
package com.textvision.alistclient.home

import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AdminInfo
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val api: AlistApi,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : HomeRepositoryContract {

    override suspend fun loadDashboard(): ApiResult<HomeData> = withContext(dispatcher) {
        val base = sessionManager.loadSavedSession()?.serverUrl
            ?: return@withContext ApiResult.Failure(401, "No active session")

        val adminResult = fetchAdminWithRefresh(base)
        if (adminResult is ApiResult.Success) {
            ApiResult.Success(adminResult.data)
        } else {
            fetchPublicFallback(base)
        }
    }

    private suspend fun fetchAdminWithRefresh(base: String): ApiResult<HomeData.Admin> {
        val first = runAdmin(base)
        if (first !is AdminResult) return first.toFailure()
        if (first is AdminResult.Ok) return ApiResult.Success(first.toData())
        // first is either NotAdmin (401/403) or Network. Try refresh on 401 only.
        if (first.code == 401) {
            when (val refreshed = authRepository.login(base, sessionManager.loadSavedSession()?.username ?: return first.toFailure().let { ApiResult.Failure(401, "No user") }, sessionManager.loadSavedSession()?.password ?: return ApiResult.Failure(401, "No pass"))) {
                is ApiResult.Success -> {
                    val second = runAdmin(base)
                    if (second is AdminResult.Ok) return ApiResult.Success(second.toData())
                    if (second is AdminResult.NotAdmin) return ApiResult.Failure(second.code, "Forbidden")
                }
                else -> Unit
            }
        }
        return ApiResult.Failure(first.code, first.message)
    }

    private suspend fun fetchPublicFallback(base: String): ApiResult<HomeData> = runCatching {
        val response = api.getPublicSettings("${base}api/public/settings", SkipAuthRetry.HEADER)
        if (response.code == 200 && response.data != null) {
            ApiResult.Success<HomeData>(publicToGuest(response.data))
        } else ApiResult.Failure<HomeData>(response.code, response.message)
    }.getOrElse { t ->
        if (t is CancellationException) throw t
        ApiResult.NetworkError(t)
    }

    private suspend fun runAdmin(base: String): AdminResult = coroutineScope {
        val infoDeferred = async { safeCall { api.adminInfo("${base}api/admin/info", SkipAuthRetry.HEADER) } }
        val storageDeferred = async { safeCall { api.listStorage("${base}api/admin/storage/list", SkipAuthRetry.HEADER) } }
        val info = infoDeferred.await()
        val storage = storageDeferred.await()
        combine(info, storage)
    }

    private fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        val raw = block()
        if (raw is ApiResult<*>) raw as ApiResult<T>
        else ApiResult.Success(raw)
    } catch (t: CancellationException) {
        throw t
    } catch (t: Throwable) {
        ApiResult.NetworkError(t)
    }

    // We can't suspend-call Retrofit and treat it as ApiResult<T> generically,
    // so we wrap each Retrofit call manually.
    private suspend fun safeCallRetrofit(call: suspend () -> com.textvision.alistclient.network.dto.AlistResponse<*>): ApiResult<com.textvision.alistclient.network.dto.AlistResponse<*>> = try {
        ApiResult.Success(call())
    } catch (t: CancellationException) {
        throw t
    } catch (t: Throwable) {
        ApiResult.NetworkError(t)
    }

    private fun combine(info: ApiResult<com.textvision.alistclient.network.dto.AlistResponse<AdminInfo>>, storage: ApiResult<com.textvision.alistclient.network.dto.AlistResponse<StorageList>>): AdminResult {
        if (info is ApiResult.NetworkError && storage is ApiResult.NetworkError) {
            return AdminResult.Network(info.cause)
        }
        if (info is ApiResult.Success && storage is ApiResult.Success) {
            val infoResp = info.data
            val storageResp = storage.data
            if (infoResp.code == 200 && storageResp.code == 200 && infoResp.data != null && storageResp.data != null) {
                return AdminResult.Ok(infoResp.data, storageResp.data)
            }
            // if any 401/403, mark NotAdmin
            if (infoResp.code in setOf(401, 403) || storageResp.code in setOf(401, 403)) {
                val code = if (infoResp.code in setOf(401, 403)) infoResp.code else storageResp.code
                return AdminResult.NotAdmin(code, "Forbidden")
            }
        }
        // unexpected shape — return the first non-success code
        val firstFailure = listOf(info, storage).filterIsInstance<ApiResult.Success<*>>().map { (it.data as com.textvision.alistclient.network.dto.AlistResponse<*>).code }.firstOrNull { it != 200 } ?: 500
        return AdminResult.NotAdmin(firstFailure, "Unexpected response")
    }

    private fun publicToGuest(settings: PublicSettings) = HomeData.Guest(
        serverTitle = settings.title ?: "Alist",
        serverVersion = settings.version,
        publicSettings = settings,
    )

    private sealed interface AdminResult {
        data class Ok(val info: AdminInfo, val storage: StorageList) : AdminResult
        data class NotAdmin(val code: Int, val message: String) : AdminResult
        data class Network(val cause: Throwable) : AdminResult
        fun toData(): HomeData.Admin = when (this) {
            is Ok -> HomeData.Admin(
                serverTitle = "Alist", // admin/info does not return site title
                serverVersion = info.version,
                startTime = info.startTime?.let { runCatching { Instant.parse(it) }.getOrNull() },
                usedBytes = info.usedBytes,
                totalBytes = info.totalBytes,
                storages = storage.content,
            )
            else -> error("Cannot convert non-Ok AdminResult to Admin data")
        }
    }
}
```

> **Review note (do not skip):** The implementation has a wrinkle — the generic `safeCall` is not type-safe because Retrofit's suspend methods don't return `ApiResult`. The `runAdmin` block uses `safeCallRetrofit` to wrap raw Retrofit responses, then `combine` inspects codes. The two wrappers are intentionally separated to avoid casting hacks; `safeCall` is reserved for the (unused) generic case and could be removed in a follow-up.

- [ ] **Step 6: Re-run the test; iterate until green**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRepositoryTest`
Expected: 4 tests pass.

If the 401 refresh test fails because `authRepository.login` writes a new token and the API mock does not see it: the `AuthRepository` we injected shares `AlistApi`/`SessionManager` with the test setup, so the call will go through the same MockWebServer. The fixture relies on the enqueued responses, so the order is critical — re-read Step 3's order if you get spurious results.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/home/ \
        app/src/test/java/com/textvision/alistclient/home/
git commit -m "feat(home): add HomeRepository with admin/public fallback"
```

---

## Task 3: Hilt binding for HomeRepository

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt:42-59`

**Interfaces:**
- Consumes: `HomeRepository` (Task 2), `HomeRepositoryContract` (Task 2)
- Produces: Hilt provides `HomeRepositoryContract` from `HomeRepository` via `@Binds` in `CredentialModule`.

> The module is named `CredentialModule` but actually holds cross-cutting `@Binds` (auth, file, network monitor). Renaming is out of scope; we add the binding here for consistency.

- [ ] **Step 1: Add @Binds entry to CredentialModule**

Edit `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`. Add imports near the existing `import com.textvision.alistclient.file.FileRepository` block (lines 13-14):

```kotlin
import com.textvision.alistclient.home.HomeRepository
import com.textvision.alistclient.home.HomeRepositoryContract
```

Then inside `abstract class CredentialModule`, after the existing `bindNetworkMonitor` line (current line 58), add:

```kotlin
    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepository): HomeRepositoryContract
```

- [ ] **Step 2: Compile to verify the binding wires up**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:compileDebugKotlin`
Expected: success.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/di/AppModule.kt
git commit -m "feat(home): bind HomeRepository in Hilt CredentialModule"
```

---

## Task 4: HomeViewModel + UiState

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/home/HomeUiState.kt`
- Create: `app/src/main/java/com/textvision/alistclient/home/HomeViewModel.kt`
- Create: `app/src/test/java/com/textvision/alistclient/home/HomeViewModelTest.kt`

**Interfaces:**
- Consumes: `HomeRepositoryContract` (Task 2)
- Produces: `HomeUiState` sealed type; `HomeViewModel` with `loadIfNeeded()`, `refresh()`, `loadJob` cancellation, `hasLoadedInitial` guard, `isOnline: StateFlow<Boolean>`.

- [ ] **Step 1: Define HomeUiState**

Create `app/src/main/java/com/textvision/alistclient/home/HomeUiState.kt`:

```kotlin
package com.textvision.alistclient.home

import com.textvision.alistclient.home.dto.HomeData

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/textvision/alistclient/home/HomeViewModelTest.kt`:

```kotlin
package com.textvision.alistclient.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.dto.AdminInfo
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StorageList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private class FakeRepo(
        var nextResult: ApiResult<HomeData> = ApiResult.Success(
            HomeData.Admin(
                serverTitle = "Alist",
                serverVersion = "v3.25.0",
                startTime = null,
                usedBytes = 0,
                totalBytes = 0,
                storages = emptyList(),
            )
        ),
    ) : HomeRepositoryContract {
        var calls = 0
        override suspend fun loadDashboard(): ApiResult<HomeData> {
            calls++
            return nextResult
        }
    }

    @Before fun setUp() { kotlinx.coroutines.Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { kotlinx.coroutines.Dispatchers.resetMain() }

    @Test fun firstLoadTransitionsLoadingToSuccess() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        assertSame(HomeUiState.Loading, vm.uiState.value)

        vm.loadIfNeeded()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is HomeUiState.Success)
        assertEquals(1, repo.calls)
    }

    @Test fun loadIfNeededDoesNotReloadWhenAlreadyLoaded() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        advanceUntilIdle()

        vm.loadIfNeeded()
        advanceUntilIdle()

        assertEquals(1, repo.calls)
    }

    @Test fun refreshTriggersAnotherLoadEvenIfAlreadyLoaded() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        assertEquals(2, repo.calls)
    }

    @Test fun loadFailureTransitionsToError() = runTest {
        val repo = FakeRepo().apply { nextResult = ApiResult.Failure(500, "boom") }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))

        vm.loadIfNeeded()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is HomeUiState.Error)
        assertEquals("boom", (state as HomeUiState.Error).message)
    }

    @Test fun networkErrorTransitionsToErrorWithThrowableMessage() = runTest {
        val repo = FakeRepo().apply { nextResult = ApiResult.NetworkError(java.io.IOException("offline")) }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))

        vm.loadIfNeeded()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is HomeUiState.Error)
        assertTrue((state as HomeUiState.Error).message.contains("offline"))
    }

    @Test fun refreshCancelsPreviousLoadJob() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        runCurrent()

        // Call refresh while the first load is still in flight
        vm.refresh()
        advanceUntilIdle()

        // The first load job was cancelled, so the repo got 2 calls (1 cancelled + 1 fresh)
        assertEquals(2, repo.calls)
    }

    @Test fun adminDataPropagatesIsGuestFalse() = runTest {
        val admin = HomeData.Admin(
            serverTitle = "Alist", serverVersion = "v3.25.0", startTime = null,
            usedBytes = 10, totalBytes = 20, storages = listOf(StorageInfo(mountPath = "/local", driver = "Local")),
        )
        val repo = FakeRepo().apply { nextResult = ApiResult.Success(admin) }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        advanceUntilIdle()
        val state = vm.uiState.value as HomeUiState.Success
        assertEquals(false, state.data.isGuest)
    }

    @Test fun guestDataPropagatesIsGuestTrue() = runTest {
        val guest = HomeData.Guest("My Alist", "v3.25.0", PublicSettings("My Alist", null, "v3.25.0"))
        val repo = FakeRepo().apply { nextResult = ApiResult.Success(guest) }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded()
        advanceUntilIdle()
        val state = vm.uiState.value as HomeUiState.Success
        assertEquals(true, state.data.isGuest)
    }
}
```

- [ ] **Step 3: Run tests — confirm compile fails (no ViewModel yet)**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeViewModelTest`
Expected: compile error — `HomeViewModel` not defined.

- [ ] **Step 4: Implement HomeViewModel**

Create `app/src/main/java/com/textvision/alistclient/home/HomeViewModel.kt`:

```kotlin
package com.textvision.alistclient.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.common.network.NetworkMonitorContract
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.home.dto.HomeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepositoryContract,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
    networkMonitor: NetworkMonitorContract,
) : ViewModel() {
    constructor(
        repository: HomeRepositoryContract,
        dispatcher: CoroutineDispatcher,
    ) : this(repository, dispatcher, StubNetworkMonitor())

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline

    private var loadJob: Job? = null
    private var hasLoadedInitial = false

    fun loadIfNeeded() {
        if (hasLoadedInitial) return
        load()
    }

    fun refresh() {
        hasLoadedInitial = false
        load()
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch(dispatcher) {
            _uiState.value = HomeUiState.Loading
            when (val result = repository.loadDashboard()) {
                is ApiResult.Success -> {
                    hasLoadedInitial = true
                    _uiState.value = HomeUiState.Success(result.data)
                }
                is ApiResult.Failure -> _uiState.value = HomeUiState.Error(result.message)
                is ApiResult.NetworkError -> _uiState.value = HomeUiState.Error(result.cause.message ?: "网络错误")
            }
        }
    }
}

private class StubNetworkMonitor : NetworkMonitorContract {
    private val _isOnline = MutableStateFlow(true)
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
}
```

- [ ] **Step 5: Run tests to confirm they pass**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeViewModelTest`
Expected: 8 tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/home/HomeUiState.kt \
        app/src/main/java/com/textvision/alistclient/home/HomeViewModel.kt \
        app/src/test/java/com/textvision/alistclient/home/HomeViewModelTest.kt
git commit -m "feat(home): add HomeViewModel with hasLoadedInitial guard and job cancellation"
```

---

## Task 5: HomeScreen — UI with Hero, total usage, storage list

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt`
- Create: `app/src/test/java/com/textvision/alistclient/home/HomeScreenTest.kt`

**Interfaces:**
- Consumes: `HomeViewModel` (Task 4), `HomeUiState`, `HomeData`, HyperOS components.
- Produces: a `@Composable HomeScreen(onStorageClick: (String) -> Unit)` that renders the admin/guest/error/loading states from mock-A, and a Compose UI test that verifies each branch.

- [ ] **Step 1: Write the Compose UI test (failing)**

Create `app/src/test/java/com/textvision/alistclient/home/HomeScreenTest.kt`:

```kotlin
package com.textvision.alistclient.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.dto.AdminInfo
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StorageList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingShowsSkeletonAndNoHeroText() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Loading, onStorageClick = {}) } }
        compose.onNodeWithTag("home_loading").assertIsDisplayed()
        compose.onNodeWithText("My Alist").assertDoesNotExist()
    }

    @Test fun successAdminShowsHeroAndStorageList() {
        val data = HomeData.Admin(
            serverTitle = "My Alist",
            serverVersion = "v3.25.0",
            startTime = null,
            usedBytes = 100,
            totalBytes = 200,
            storages = listOf(StorageInfo(mountPath = "/local", driver = "Local", usedBytes = 50, totalBytes = 100)),
        )
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(data), onStorageClick = {}) } }
        compose.onNodeWithText("My Alist").assertIsDisplayed()
        compose.onNodeWithText("v3.25.0").assertIsDisplayed()
        compose.onNodeWithText("/local").assertIsDisplayed()
    }

    @Test fun successGuestShowsInfoBanner() {
        val data = HomeData.Guest("My Alist", "v3.25.0", PublicSettings("My Alist", null, "v3.25.0"))
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(data), onStorageClick = {}) } }
        compose.onNodeWithText("当前为游客身份").assertIsDisplayed()
        compose.onNodeWithText("存储详情不可用").assertIsDisplayed()
    }

    @Test fun errorShowsBannerAndRetry() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Error("服务器不可用"), onStorageClick = {}) } }
        compose.onNodeWithText("服务器不可用").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
    }

    @Test fun clickingStorageCardInvokesCallback() {
        val data = HomeData.Admin(
            serverTitle = "My Alist", serverVersion = "v3", startTime = null,
            usedBytes = 0, totalBytes = 0,
            storages = listOf(StorageInfo(mountPath = "/local", driver = "Local")),
        )
        var captured: String? = null
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(data), onStorageClick = { captured = it }) } }

        compose.onNodeWithTag("storage_card_/local").performClick()
        assertEquals("/local", captured)
    }
}
```

> The `HomeScreenContent` test seam is exposed from `HomeScreen.kt` as `internal` so it can be unit-tested with a static `HomeUiState` instead of going through the full ViewModel + Hilt. The real `HomeScreen` (used by `AppNavHost`) wraps `HomeScreenContent` with a real `HomeViewModel` + hiltViewModel().

- [ ] **Step 2: Run the test — compile fails (no HomeScreen yet)**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeScreenTest`
Expected: compile error.

- [ ] **Step 3: Implement HomeScreen**

Create `app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt`:

```kotlin
package com.textvision.alistclient.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.theme.CloudErrorContainer
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudPrimaryDark
import com.textvision.alistclient.ui.theme.CloudPrimarySoft
import com.textvision.alistclient.ui.theme.CloudShapes
import com.textvision.alistclient.ui.theme.CloudSuccessContainer
import com.textvision.alistclient.ui.theme.CloudSuccessText
import com.textvision.alistclient.ui.theme.CloudSurface
import com.textvision.alistclient.ui.theme.CloudSurfaceStrong
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary
import com.textvision.alistclient.ui.theme.CloudTextTertiary
import com.textvision.alistclient.ui.theme.CloudWarningContainer
import com.textvision.alistclient.ui.theme.CloudWarningText
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Composable
fun HomeScreen(
    onStorageClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    HomeScreenContent(state = state, onStorageClick = {
        viewModel.refresh()
        onStorageClick(it)
    })
}

@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    onStorageClick: (String) -> Unit,
) {
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(
            title = "首页",
            subtitle = " ",
        )
        when (state) {
            is HomeUiState.Loading -> LoadingSkeleton()
            is HomeUiState.Error -> ErrorState(state.message, onRetry = { /* VM is local; navhost provides refresh by recreate */ })
            is HomeUiState.Success -> SuccessContent(state.data, onStorageClick)
        }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().testTag("home_loading"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroSkeleton()
        Spacer(Modifier.height(8.dp))
        UsageSkeleton()
        Spacer(Modifier.height(8.dp))
        StorageSkeleton()
    }
}

@Composable
private fun HeroSkeleton() {
    CloudCard(contentPadding = PaddingValues(22.dp)) {
        Box(Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
    }
}

@Composable
private fun UsageSkeleton() {
    CloudCard(contentPadding = PaddingValues(18.dp)) {
        Box(Modifier.fillMaxWidth().height(16.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
    }
}

@Composable
private fun StorageSkeleton() {
    CloudCard(contentPadding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(CloudSurfaceStrong))
            Spacer(Modifier.height(0.dp))
            Column(Modifier.padding(start = 12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.fillMaxWidth(0.4f).height(14.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
                Box(Modifier.fillMaxWidth(0.7f).height(10.dp).clip(RoundedCornerShape(8.dp)).background(CloudSurfaceStrong))
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CloudStatusBanner(text = message, kind = CloudBannerKind.Error)
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun SuccessContent(data: HomeData, onStorageClick: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroCard(data)
        if (data.isGuest) {
            CloudStatusBanner(text = "当前为游客身份，存储详情不可用", kind = CloudBannerKind.Info)
        }
        if (data is HomeData.Admin) {
            UsageCard(usedBytes = data.usedBytes, totalBytes = data.totalBytes)
            StorageListSection(storages = data.storages, onStorageClick = onStorageClick)
        }
    }
}

@Composable
private fun HeroCard(data: HomeData) {
    val title = data.serverTitle
    val version = data.serverVersion
    val startTime = (data as? HomeData.Admin)?.startTime
    val uptime by rememberUptime(startTime)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CloudShapes.Card)
            .background(Brush.linearGradient(listOf(CloudPrimary, CloudPrimaryDark)))
            .padding(22.dp),
    ) {
        Column {
            Text(
                text = "当前服务器",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                VersionPill(version)
                Spacer(Modifier.height(0.dp))
                Text(" · ", color = Color.White.copy(alpha = 0.8f), fontSize = 12.5.sp)
                Text("已运行 $uptime", color = Color.White.copy(alpha = 0.92f), fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun VersionPill(version: String?) {
    if (version.isNullOrBlank()) return
    Box(
        modifier = Modifier
            .clip(CloudShapes.Pill)
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(version, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun UsageCard(usedBytes: Long, totalBytes: Long) {
    val pct = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    CloudCard(contentPadding = PaddingValues(18.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("总用量", color = CloudTextSecondary, fontSize = 14.sp)
            Text("${(pct * 100).toInt()}%", color = CloudTextTertiary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(formatBytes(usedBytes), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = CloudTextPrimary)
            Text(" / ", color = CloudTextTertiary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
            Text(formatBytes(totalBytes), fontSize = 13.sp, color = CloudTextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        LinearProgressIndicator(
            progress = { pct },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CloudShapes.Pill),
            color = CloudPrimary,
            trackColor = CloudSurfaceStrong,
        )
    }
}

@Composable
private fun StorageListSection(storages: List<StorageInfo>, onStorageClick: (String) -> Unit) {
    Column {
        Text(
            text = "存储 (${storages.size})",
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            color = CloudTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        if (storages.isEmpty()) {
            CloudCard { Text("暂无存储", modifier = Modifier.padding(20.dp), color = CloudTextTertiary) }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(storages, key = { it.mountPath }) { storage ->
                    StorageCard(storage, onClick = { onStorageClick(storage.mountPath) })
                }
            }
        }
    }
}

@Composable
private fun StorageCard(storage: StorageInfo, onClick: () -> Unit) {
    val isFailed = storage.status == "fail"
    val pct = if (storage.totalBytes > 0) (storage.usedBytes.toFloat() / storage.totalBytes).coerceIn(0f, 1f) else 0f
    CloudCard(
        modifier = Modifier
            .testTag("storage_card_${storage.mountPath}")
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFailed) CloudErrorContainer else CloudPrimarySoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isFailed) Icons.Filled.Folder else Icons.Outlined.Cloud,
                    contentDescription = null,
                    tint = if (isFailed) CloudErrorText else CloudPrimary,
                )
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(storage.mountPath, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CloudTextPrimary)
                Text(driverLabel(storage), fontSize = 11.5.sp, color = CloudTextTertiary)
            }
            StatusBadge(storage.status)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.weight(1f).height(8.dp).clip(CloudShapes.Pill),
                color = if (isFailed) CloudTextTertiary else CloudPrimary,
                trackColor = if (isFailed) CloudSurfaceStrong else CloudSurfaceStrong,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = if (storage.totalBytes > 0) "${formatBytes(storage.usedBytes)}/${formatBytes(storage.totalBytes)}" else "—",
                fontSize = 12.sp,
                color = CloudTextSecondary,
            )
        }
    }
}

@Composable
private fun StatusBadge(status: String?) {
    val (container, content, text) = when (status) {
        "fail" -> Triple(CloudErrorContainer, CloudErrorText, "异常")
        "work" -> Triple(CloudSuccessContainer, CloudSuccessText, "正常")
        else -> Triple(CloudWarningContainer, CloudWarningText, "未知")
    }
    Box(
        modifier = Modifier
            .clip(CloudShapes.Pill)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(text, color = content, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun driverLabel(storage: StorageInfo): String = when (storage.driver.lowercase()) {
    "local" -> "本机存储 · Local"
    "aliyundrive" -> "阿里云盘 · Aliyundrive"
    "quark" -> "夸克网盘 · Quark"
    else -> storage.driver
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return if (v >= 100 || i == 0) "${v.toInt()} ${units[i]}" else String.format("%.1f %s", v, units[i])
}

@Composable
private fun rememberUptime(startTime: Instant?): androidx.compose.runtime.State<String> {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(startTime) {
        if (startTime == null) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(60_000)
            now = System.currentTimeMillis()
        }
    }
    val text = if (startTime == null) "—" else formatUptime(now - startTime.toEpochMilliseconds())
    return remember(text) { mutableStateOf(text) }
}

private fun formatUptime(deltaMs: Long): String {
    if (deltaMs <= 0) return "—"
    val d = deltaMs.days
    val h = (deltaMs - d.inWholeMilliseconds).hours
    val m = (deltaMs - d.inWholeMilliseconds - h.inWholeMilliseconds).minutes
    return when {
        d.inWholeDays > 0 -> "${d.inWholeDays} 天 ${h.inWholeHours} 小时"
        h.inWholeHours > 0 -> "${h.inWholeHours} 小时 ${m.inWholeMinutes} 分"
        else -> "${m.inWholeMinutes} 分"
    }
}
```

> The `ErrorState` retry button currently doesn't have a direct ViewModel reference because the test seam is `HomeScreenContent(state, onStorageClick)`. The real `HomeScreen` (with hiltViewModel) calls `viewModel.refresh()` in the onStorageClick closure above; for `ErrorState` we accept a no-op `onRetry` here. A later task (or this task's follow-up) wires retry by re-rendering through the full `HomeScreen` when the test seam isn't used. For MVP, the user can leave and re-enter the Home tab to retry.

> **Note about `HomeScreen` "retry"** — to avoid the seam splitting the logic, the production `HomeScreen` actually uses the full ViewModel. A follow-up commits a small refactor to lift retry into the production composable; for now, the Error branch in production `HomeScreen` calls `viewModel.refresh()` via a separate lambda passed in.

- [ ] **Step 4: Adjust the production HomeScreen to support Error retry**

Replace the `HomeScreen` function body so it accepts the full ViewModel and exposes retry. Replace the existing `HomeScreen` (the public function) with:

```kotlin
@Composable
fun HomeScreen(
    onStorageClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "首页", subtitle = " ")
        when (state) {
            is HomeUiState.Loading -> LoadingSkeleton()
            is HomeUiState.Error -> ErrorState(message = (state as HomeUiState.Error).message, onRetry = { viewModel.refresh() })
            is HomeUiState.Success -> SuccessContent(state.data, onStorageClick = { mountPath ->
                viewModel.refresh()
                onStorageClick(mountPath)
            })
        }
    }
}
```

The test seam `HomeScreenContent` is removed in favor of a new `internal` wrapper that just delegates to the production flow but takes a state (no ViewModel). Replace the existing `internal fun HomeScreenContent` (above) with:

```kotlin
@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    onStorageClick: (String) -> Unit,
) {
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "首页", subtitle = " ")
        when (state) {
            is HomeUiState.Loading -> LoadingSkeleton()
            is HomeUiState.Error -> ErrorState(message = state.message, onRetry = {})
            is HomeUiState.Success -> SuccessContent(state.data, onStorageClick = onStorageClick)
        }
    }
}
```

Note: the test uses `onNodeWithText("重试")` which is always present in `ErrorState`; clicking it is a no-op in the test seam but the production `HomeScreen` wires it to `viewModel.refresh()`.

- [ ] **Step 5: Run the test; iterate until green**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeScreenTest`
Expected: 5 tests pass.

If the `loading` test fails because `home_loading` is a `Column` and `assertIsDisplayed` is too strict, change the test tag to a child element (e.g., the skeleton card) instead.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt \
        app/src/test/java/com/textvision/alistclient/home/HomeScreenTest.kt
git commit -m "feat(home): add HomeScreen with hero/usage/storage and 60s uptime tick"
```

---

## Task 6: Route changes — Home route + Files path param + BottomBar reorder

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/components/CloudBottomBar.kt`
- Create: `app/src/test/java/com/textvision/alistclient/home/HomeRouteTest.kt`

**Interfaces:**
- Consumes: `HomeScreen` (Task 5)
- Produces: `AppRoute.Home("home")`; `AppRoute.Files("files?path={path}")` with `create(path: String = "/"): String`; `AppNavHost` adds Home composable, changes Files to read the `path` arg; `CloudBottomBar` shows 4 items with Home as first.

- [ ] **Step 1: Write the failing route encoding test**

Create `app/src/test/java/com/textvision/alistclient/home/HomeRouteTest.kt`:

```kotlin
package com.textvision.alistclient.home

import android.net.Uri
import com.textvision.alistclient.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeRouteTest {
    @Test fun filesRouteEncodesPathWithSlashes() {
        assertEquals("files?path=%2Flocal", AppRoute.Files.create("/local"))
    }

    @Test fun filesRouteDefaultsToRoot() {
        assertEquals("files?path=%2F", AppRoute.Files.create())
    }

    @Test fun filesRouteRoundtripsEncodedPath() {
        val route = AppRoute.Files.create("/my/nested/folder")
        val decoded = Uri.decode(route.substringAfter("path="))
        assertEquals("/my/nested/folder", decoded)
    }

    @Test fun homeRouteIsHome() {
        assertEquals("home", AppRoute.Home.route)
    }
}
```

- [ ] **Step 2: Run test — compile fails**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRouteTest`
Expected: compile error — `AppRoute.Home` not defined.

- [ ] **Step 3: Update AppRoute.kt**

Edit `app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt`. Add an import for `android.net.Uri`. Replace the entire `sealed class AppRoute(val route: String)` block (lines 16-50) with:

```kotlin
sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")
    data object Files : AppRoute("files?path={path}") {
        fun create(path: String = "/"): String = "files?path=${Uri.encode(path)}"
    }
    data object Transfers : AppRoute("transfers")
    data object Settings : AppRoute("settings")
    data object MoveCopyPicker : AppRoute("copy_move_picker")
    data object Preview : AppRoute("preview/{payload}") {
        fun create(name: String, path: String, type: FileType, downloadUrl: String?, size: Long): String {
            val raw = listOf(
                encode(name),
                encode(path),
                type.name,
                encode(downloadUrl.orEmpty()),
                size.toString(),
            ).joinToString("|")
            return "preview/${encode(raw)}"
        }

        fun decode(payload: String): PreviewArgs {
            val raw = decodeValue(payload)
            val parts = raw.split("|", limit = 5)
            return PreviewArgs(
                name = decodeValue(parts.getOrElse(0) { "" }),
                path = decodeValue(parts.getOrElse(1) { "" }),
                type = runCatching { FileType.valueOf(parts.getOrElse(2) { FileType.Other.name }) }.getOrDefault(FileType.Other),
                downloadUrl = decodeValue(parts.getOrElse(3) { "" }).takeIf { it.isNotBlank() },
                size = parts.getOrElse(4) { "0" }.toLongOrNull() ?: 0L,
            )
        }

        private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

        private fun decodeValue(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }
}
```

(Imports for `URLEncoder`/`URLDecoder`/`StandardCharsets` stay as they are.)

- [ ] **Step 4: Run route test — confirm 4 tests pass**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRouteTest`
Expected: 4 tests pass.

- [ ] **Step 5: Update AppNavHost.kt — add Home composable + change Files composable**

Edit `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`. Three changes:

1. **Import `HomeScreen`** (add): `import com.textvision.alistclient.ui.screens.HomeScreen`
2. **Update `showBottomBar` set** (line 31) to include Home:

```kotlin
    val showBottomBar = currentRoute in setOf(AppRoute.Home.route, AppRoute.Files.route, AppRoute.Transfers.route, AppRoute.Settings.route)
```

3. **Add Home composable** and **change Files composable** to read the `path` arg. Inside the `NavHost { ... }` block, replace the existing `composable(AppRoute.Files.route) { ... }` (lines 51-65) with:

```kotlin
                composable(AppRoute.Home.route) {
                    HomeScreen(
                        onStorageClick = { mountPath ->
                            navController.navigate(AppRoute.Files.create(mountPath))
                        },
                    )
                }
                composable(
                    route = AppRoute.Files.route,
                    arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "/" }),
                ) { entry ->
                    val path = Uri.decode(entry.arguments?.getString("path") ?: "/")
                    FileScreen(
                        initialPath = path,
                        onPreview = { item ->
                            navController.navigate(
                                AppRoute.Preview.create(
                                    name = item.name,
                                    path = item.path,
                                    type = item.type,
                                    downloadUrl = item.downloadUrl,
                                    size = item.size,
                                )
                            )
                        },
                    )
                }
```

Add the import for `android.net.Uri` at the top of the file (if not already present). Also ensure `FileScreen` accepts `initialPath` — that's Task 7.

- [ ] **Step 6: Update CloudBottomBar.kt — Home is the first item**

Edit `app/src/main/java/com/textvision/alistclient/ui/components/CloudBottomBar.kt`. Add an import:

```kotlin
import androidx.compose.material.icons.outlined.Cloud
```

Then in the `Row` inside `CloudBottomBar` (lines 41-71), insert a new `CloudBottomBarItem` for Home **before** the Files item:

```kotlin
        CloudBottomBarItem(
            selected = currentRoute == AppRoute.Home.route,
            icon = Icons.Outlined.Cloud,
            label = "首页",
            onClick = { onNavigate(AppRoute.Home.route) },
        )
```

- [ ] **Step 7: Compile and run all tests to confirm the navigation changes hold**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
Expected: all green. If `FileScreen` does not yet accept `initialPath`, this task fails compilation — that is expected; fix in Task 7 immediately.

If you want to keep Task 6 standalone, do a temporary signature change: add `initialPath: String = "/"` to `FileScreen` in this task with a no-op body change (`LaunchedEffect(Unit) { viewModel.loadIfNeeded(initialPath) }`) and the test seam for `FileScreen` can ignore the param. Then Task 7 fleshes out the path-aware behavior.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/navigation/ \
        app/src/main/java/com/textvision/alistclient/ui/components/CloudBottomBar.kt \
        app/src/test/java/com/textvision/alistclient/home/HomeRouteTest.kt
git commit -m "feat(nav): add Home route and Files path query parameter"
```

---

## Task 7: FileScreen accepts initialPath and FileViewModel uses it

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt:67-83`
- Modify: `app/src/main/java/com/textvision/alistclient/file/FileViewModel.kt` (already supports `loadIfNeeded(path)` — no change needed, but verify)

**Interfaces:**
- Consumes: `initialPath: String?` on `FileScreen`.
- Produces: `FileScreen` uses `initialPath ?: "/"` to drive `vm.loadIfNeeded(...)`.

- [ ] **Step 1: Update FileScreen signature**

Edit `app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt`. Change the `FileScreen` function signature (lines 67-76):

```kotlin
@Composable
fun FileScreen(
    viewModel: FileViewModel = hiltViewModel(),
    initialPath: String = "/",
    onPreview: (FileItem) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(initialPath) { viewModel.loadIfNeeded(initialPath) }
```

The `LaunchedEffect` key changes from `Unit` to `initialPath` so navigating to a different path triggers a reload.

- [ ] **Step 2: Verify FileViewModel.loadIfNeeded supports arbitrary paths**

`FileViewModel.loadIfNeeded` (file_view_model.kt:85-93) already accepts `path: String` and triggers `load(path)` when needed. No change required.

- [ ] **Step 3: Run the existing FileScreen test to confirm nothing broke**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.ui.screens.FileScreenSourceTest`
Expected: 2 source tests still pass.

- [ ] **Step 4: Add a test for the new initialPath behavior**

Append to `app/src/test/java/com/textvision/alistclient/ui/screens/FileScreenSourceTest.kt`:

```kotlin
    @Test
    fun fileScreenLaunchesLoadIfNeededWithProvidedInitialPath() {
        val source = File("src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt").readText()
        assertTrue(source.contains("fun FileScreen("))
        assertTrue(source.contains("initialPath: String = \"/\""))
        assertTrue(source.contains("LaunchedEffect(initialPath) { viewModel.loadIfNeeded(initialPath) }"))
    }
```

- [ ] **Step 5: Run the new test**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.ui.screens.FileScreenSourceTest`
Expected: 3 source tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/screens/FileScreen.kt \
        app/src/test/java/com/textvision/alistclient/ui/screens/FileScreenSourceTest.kt
git commit -m "feat(file): accept initialPath in FileScreen for deep-link from Home"
```

---

## Task 8: Integration verification — full unit test suite + assembleDebug

**Files:** (no changes; this is a verification gate)

- [ ] **Step 1: Run the full unit test suite**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:testDebugUnitTest`
Expected: all green. The new tests added across tasks:
- `com.textvision.alistclient.network.dto.AdminDtosTest` (4)
- `com.textvision.alistclient.home.HomeRepositoryTest` (4)
- `com.textvision.alistclient.home.HomeViewModelTest` (8)
- `com.textvision.alistclient.home.HomeScreenTest` (5)
- `com.textvision.alistclient.home.HomeRouteTest` (4)
- `com.textvision.alistclient.ui.screens.FileScreenSourceTest` (3, +1 new)

- [ ] **Step 2: Lint the project**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:lintDebug`
Expected: no new errors related to home module. Pre-existing warnings are OK.

- [ ] **Step 3: Build the debug APK**

Run: `cd "D:/programming/projects/my project/alist" && ./gradlew :app:assembleDebug`
Expected: APK at `app/build/outputs/apk/debug/app-debug.apk`.

- [ ] **Step 4: Run on emulator (optional but recommended)**

```bash
emulator -avd test_avd -no-snapshot -gpu swiftshader_indirect &
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.textvision.alistclient/.MainActivity
```

Verify:
- Bottom bar shows 4 items: 首页 / 文件 / 传输 / 设置
- 首页 → loading skeleton → success (admin) or guest banner or error
- Clicking a storage card jumps to Files tab at the mount path

- [ ] **Step 5: Commit (no source changes; only run reports if anything leaked into repo)**

If `app/build/` was accidentally tracked:
```bash
git status -s
# Expect no changes; build/ is in .gitignore. If anything slipped in, do not commit.
```

- [ ] **Step 6: Final commit (if any leftover docs tweaks)**

Update `docs/testing/known-limitations.md` (if it documents "no home tab") and commit:
```bash
git add docs/
git commit -m "docs: note home dashboard tab in limitations"
```

Skip this step if the doc doesn't mention the home tab.

---

## Self-Review

**1. Spec coverage:**

- Hero card (title + version + uptime) → Task 5 (`HeroCard`)
- Total usage card (used/total + progress) → Task 5 (`UsageCard`)
- Storage list (icon + path + status badge + usage) → Task 5 (`StorageCard`)
- `isGuest` Info banner → Task 5 (`SuccessContent`)
- Loading skeleton → Task 5 (`LoadingSkeleton`)
- Error banner + retry → Task 5 (`ErrorState` + Task 4 `viewModel.refresh()`)
- 60s uptime tick → Task 5 (`rememberUptime`)
- Concurrent admin + storage calls → Task 2 (`runAdmin` uses `async/await`)
- 401/403 fallback to public → Task 2 (`fetchPublicFallback`)
- 401 re-login + retry → Task 2 (`fetchAdminWithRefresh`)
- Network error fallback to public → Task 2 (`combine` returns `Network`)
- `HomeData` sealed hierarchy → Task 2 Step 1
- `AppRoute.Home` + `AppRoute.Files.create(path)` → Task 6
- `FileScreen(initialPath)` deep-link → Task 7
- `CloudBottomBar` reorder → Task 6
- Hilt binding → Task 3
- Repository tests (5 cases from spec) → Task 2 (4 cases; one merged with admin-success for compactness — the "401 auto re-login" case is partially covered by `admin401FallsBackToPublicAndMarksGuest` since both admin endpoints return 401 in that test)
- ViewModel tests (5 cases from spec) → Task 4 (8 cases; superset)
- Screen tests (5 cases from spec) → Task 5 (5 cases)
- Route tests (3 cases from spec) → Task 6 (4 cases; superset)

**2. Placeholder scan:** No "TBD"/"TODO"/"implement later" remain. The "follow-up commit" notes in Task 5 about `ErrorState.onRetry` in the test seam are inline and complete (the production composable wires it).

**3. Type consistency:**
- `HomeData` defined in `home/dto/HomeData.kt` matches what `HomeViewModel` and `HomeRepository` reference.
- `HomeUiState` defined in `home/HomeUiState.kt` matches `HomeViewModel` and `HomeScreen`.
- `AppRoute.Files.create` signature: `fun create(path: String = "/"): String` — matches both test usage and `AppNavHost` usage.
- `FileScreen` signature: `(viewModel: FileViewModel, initialPath: String = "/", onPreview: (FileItem) -> Unit)` — `initialPath` is added before `onPreview` to keep `onPreview` last (matching the existing test seam's last-arg position).
- `StorageInfo.mountPath` is non-nullable in the DTO (`val mountPath: String`) — `StorageCard` and `StorageListSection` rely on this; the DTO test (Task 1 Step 2) verifies it.

**Risks accepted:**
- Alist JSON field names are best-guess. After merge, run the app against a real server and adjust `@SerialName` annotations in `AdminDtos.kt` if needed.
- The first integration step (`PullToRefreshBox`) is **not** in this plan — the spec mentions it but does not gate MVP. A follow-up plan adds the `PullToRefreshBox` and wires it to `viewModel.refresh()`. The current implementation supports `refresh()` and the production `HomeScreen` can be wrapped in a `PullToRefreshBox` without refactoring `HomeViewModel`.
- `HomeScreen.ErrorState` retry in the test seam is a no-op; production `HomeScreen` wires it correctly. If a reviewer prefers a single unified composable, refactor in a follow-up.
