# Phase 1 Auth and Session Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement URL normalization, Alist login, encrypted credential persistence, error mapping, token injection, 401 relogin, session restore, and logout.

**Architecture:** Network details live in `network`; session state lives in `auth`; credentials live behind `CredentialStore`. UI calls ViewModels only. Retrofit returns DTO envelopes; repositories map DTO/network failures into `ApiResult` and `AppError`.

**Tech Stack:** Kotlin, Compose, Hilt, Retrofit, OkHttp, kotlinx-serialization, AndroidX Security Crypto, MockK, MockWebServer, Turbine.

## Global Constraints

- Alist list/search endpoints use POST JSON body.
- Login endpoint is `POST /api/auth/login`.
- Token header format: `Authorization: Bearer <token>`.
- Login must distinguish connection failure, certificate failure, non-Alist service, and authentication failure.
- HTTP warning appears under the server URL field in real time.
- Password and token must only be persisted through `CredentialStore`.
- Upload requests later will be able to opt out of Authenticator retry; Phase 1 must design token infrastructure to support this.

---

## File Structure

Create:

```text
app/src/main/java/com/textvision/alistclient/common/result/ApiResult.kt
app/src/main/java/com/textvision/alistclient/common/error/AppError.kt
app/src/main/java/com/textvision/alistclient/common/error/ErrorMapper.kt
app/src/main/java/com/textvision/alistclient/common/error/ErrorMessageMapper.kt
app/src/main/java/com/textvision/alistclient/auth/model/SavedSession.kt
app/src/main/java/com/textvision/alistclient/auth/SessionManager.kt
app/src/main/java/com/textvision/alistclient/auth/AuthRepository.kt
app/src/main/java/com/textvision/alistclient/auth/LoginViewModel.kt
app/src/main/java/com/textvision/alistclient/network/dto/AlistResponse.kt
app/src/main/java/com/textvision/alistclient/network/dto/AuthDtos.kt
app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt
app/src/main/java/com/textvision/alistclient/network/AuthTokenProvider.kt
app/src/main/java/com/textvision/alistclient/network/AuthInterceptor.kt
app/src/main/java/com/textvision/alistclient/network/TokenAuthenticator.kt
app/src/main/java/com/textvision/alistclient/network/SkipAuthRetry.kt
app/src/main/java/com/textvision/alistclient/util/ServerUrlNormalizer.kt
app/src/test/java/com/textvision/alistclient/util/ServerUrlNormalizerTest.kt
app/src/test/java/com/textvision/alistclient/common/error/ErrorMapperTest.kt
app/src/test/java/com/textvision/alistclient/common/error/ErrorMessageMapperTest.kt
app/src/test/java/com/textvision/alistclient/auth/AuthRepositoryTest.kt
app/src/test/java/com/textvision/alistclient/auth/LoginViewModelTest.kt
```

Modify:

```text
app/src/main/java/com/textvision/alistclient/di/AppModule.kt
app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt
app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt
app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt
```

---

### Task 1.1: URL normalization and HTTP warning model

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/util/ServerUrlNormalizer.kt`
- Test: `app/src/test/java/com/textvision/alistclient/util/ServerUrlNormalizerTest.kt`

**Interfaces:**
- Produces: `object ServerUrlNormalizer { fun normalize(input: String): Result<String>; fun isHttp(input: String): Boolean }`
- Consumes: none.

- [ ] **Step 1: Write failing tests**

```kotlin
package com.textvision.alistclient.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ServerUrlNormalizerTest {
    @Test fun addsHttpWhenSchemeMissing() {
        assertEquals("http://textvision.top:5244/", ServerUrlNormalizer.normalize("textvision.top:5244").getOrThrow())
    }

    @Test fun preservesHttpsAndAddsTrailingSlash() {
        assertEquals("https://example.com/alist/", ServerUrlNormalizer.normalize(" https://example.com/alist ").getOrThrow())
    }

    @Test fun rejectsBlankAndNonHttpSchemes() {
        assertTrue(ServerUrlNormalizer.normalize("   ").isFailure)
        assertTrue(ServerUrlNormalizer.normalize("ftp://example.com").isFailure)
    }

    @Test fun detectsHttpForWarning() {
        assertTrue(ServerUrlNormalizer.isHttp("http://example.com"))
        assertFalse(ServerUrlNormalizer.isHttp("https://example.com"))
        assertFalse(ServerUrlNormalizer.isHttp("example.com"))
    }
}
```

- [ ] **Step 2: Run test and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.util.ServerUrlNormalizerTest"
```

Expected: FAIL because `ServerUrlNormalizer` does not exist.

- [ ] **Step 3: Implement normalizer**

```kotlin
package com.textvision.alistclient.util

import java.net.URI

object ServerUrlNormalizer {
    fun normalize(input: String): Result<String> = runCatching {
        val trimmed = input.trim()
        require(trimmed.isNotEmpty()) { "服务器地址不能为空" }
        val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        val uri = URI(withScheme)
        require(uri.scheme == "http" || uri.scheme == "https") { "仅支持 HTTP 或 HTTPS" }
        require(!uri.host.isNullOrBlank()) { "服务器地址不合法" }
        if (withScheme.endsWith('/')) withScheme else "$withScheme/"
    }

    fun isHttp(input: String): Boolean = input.trim().startsWith("http://", ignoreCase = true)
}
```

- [ ] **Step 4: Run test and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.util.ServerUrlNormalizerTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/util/ServerUrlNormalizer.kt app/src/test/java/com/textvision/alistclient/util/ServerUrlNormalizerTest.kt
git commit -m "feat: add server url normalization"
```

---

### Task 1.2: API result, app error, and user message mapping

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/common/result/ApiResult.kt`
- Create: `app/src/main/java/com/textvision/alistclient/common/error/AppError.kt`
- Create: `app/src/main/java/com/textvision/alistclient/common/error/ErrorMapper.kt`
- Create: `app/src/main/java/com/textvision/alistclient/common/error/ErrorMessageMapper.kt`
- Test: `app/src/test/java/com/textvision/alistclient/common/error/ErrorMapperTest.kt`
- Test: `app/src/test/java/com/textvision/alistclient/common/error/ErrorMessageMapperTest.kt`

**Interfaces:**
- Produces: `ApiResult<T>`, `AppError`, `ErrorMapper.mapThrowable(Throwable)`, `ErrorMapper.mapAlistFailure(Int, String?)`, `ErrorMessageMapper.toUserMessage(AppError)`.
- Consumes: none.

- [ ] **Step 1: Write failing tests**

`ErrorMapperTest.kt`:

```kotlin
package com.textvision.alistclient.common.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.CancellationException
import org.junit.Test
import java.net.ConnectException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException

class ErrorMapperTest {
    @Test fun mapsCommonThrowables() {
        assertEquals(AppError.ServerUnreachable, ErrorMapper.mapThrowable(ConnectException()))
        assertEquals(AppError.Timeout, ErrorMapper.mapThrowable(SocketTimeoutException()))
        assertEquals(AppError.CertificateUntrusted, ErrorMapper.mapThrowable(SSLHandshakeException("bad cert")))
        assertEquals(AppError.Cancelled, ErrorMapper.mapThrowable(CancellationException("cancelled")))
    }

    @Test fun mapsAlistFailures() {
        assertEquals(AppError.Unauthorized, ErrorMapper.mapAlistFailure(401, "unauthorized"))
        assertEquals(AppError.PermissionDenied, ErrorMapper.mapAlistFailure(403, "forbidden"))
        assertEquals(AppError.NotFound, ErrorMapper.mapAlistFailure(404, "not found"))
        assertEquals(AppError.Conflict, ErrorMapper.mapAlistFailure(409, "conflict"))
        assertTrue(ErrorMapper.mapAlistFailure(500, "boom") is AppError.OperationFailed)
    }
}
```

`ErrorMessageMapperTest.kt`:

```kotlin
package com.textvision.alistclient.common.error

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ErrorMessageMapperTest {
    @Test fun mapsUserMessages() {
        assertEquals("网络不可用，请检查 WiFi 或移动数据", ErrorMessageMapper.toUserMessage(AppError.NetworkUnavailable))
        assertEquals("服务器证书不受信任（过期/自签/主机名不匹配）", ErrorMessageMapper.toUserMessage(AppError.CertificateUntrusted))
        assertEquals("文件不存在", ErrorMessageMapper.toUserMessage(AppError.NotFound))
        assertEquals("server said no", ErrorMessageMapper.toUserMessage(AppError.OperationFailed("server said no")))
    }

    @Test fun mapsActionLabels() {
        assertEquals("重试", ErrorMessageMapper.toActionLabel(AppError.Timeout))
        assertEquals("重试", ErrorMessageMapper.toActionLabel(AppError.Conflict))
        assertEquals("重新登录", ErrorMessageMapper.toActionLabel(AppError.Unauthorized))
        assertNull(ErrorMessageMapper.toActionLabel(AppError.Cancelled))
        assertNull(ErrorMessageMapper.toActionLabel(AppError.CertificateUntrusted))
    }

    @Test fun retryLabelsMatchRetryability() {
        val errors = listOf(
            AppError.NetworkUnavailable,
            AppError.ServerUnreachable,
            AppError.NotFound,
            AppError.Conflict,
            AppError.Timeout,
            AppError.SSLError,
            AppError.CertificateUntrusted,
            AppError.Cancelled,
        )
        errors.forEach { error ->
            val retryLabel = ErrorMessageMapper.toActionLabel(error) == "重试"
            assertEquals(error.toString(), ErrorMessageMapper.isRetryable(error), retryLabel)
        }
    }
}
```

- [ ] **Step 2: Run tests and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.common.error.*"
```

Expected: FAIL because error classes do not exist.

- [ ] **Step 3: Implement result and errors**

`ApiResult.kt`:

```kotlin
package com.textvision.alistclient.common.result

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val code: Int, val message: String) : ApiResult<Nothing>
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>
}
```

`AppError.kt`:

```kotlin
package com.textvision.alistclient.common.error

sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object ServerUnreachable : AppError
    data object NotAlistServer : AppError
    data object Unauthorized : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object Conflict : AppError
    data object Timeout : AppError
    data object SSLError : AppError
    data object CertificateUntrusted : AppError
    data object Cancelled : AppError
    data class OperationFailed(val message: String) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}
```

`ErrorMapper.kt`:

```kotlin
package com.textvision.alistclient.common.error

import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLProtocolException

object ErrorMapper {
    fun mapThrowable(t: Throwable): AppError = when (t) {
        is CancellationException -> AppError.Cancelled
        is SSLHandshakeException -> AppError.CertificateUntrusted
        is SSLProtocolException -> AppError.SSLError
        is SSLException -> AppError.SSLError
        is UnknownHostException -> AppError.ServerUnreachable
        is ConnectException -> AppError.ServerUnreachable
        is SocketTimeoutException -> AppError.Timeout
        is IOException -> AppError.NetworkUnavailable
        else -> AppError.Unknown(t)
    }

    fun mapAlistFailure(code: Int, serverMessage: String?): AppError = when (code) {
        401 -> AppError.Unauthorized
        403 -> AppError.PermissionDenied
        404 -> AppError.NotFound
        409 -> AppError.Conflict
        in 500..599 -> AppError.OperationFailed(
            serverMessage?.takeIf { it.isNotBlank() } ?: "服务器错误 ($code)，请稍后重试"
        )
        else -> AppError.OperationFailed(
            serverMessage?.takeIf { it.isNotBlank() } ?: "操作失败 ($code)"
        )
    }
}
```

`ErrorMessageMapper.kt`:

```kotlin
package com.textvision.alistclient.common.error

object ErrorMessageMapper {
    fun toUserMessage(error: AppError): String = when (error) {
        AppError.NetworkUnavailable -> "网络不可用，请检查 WiFi 或移动数据"
        AppError.ServerUnreachable -> "无法连接服务器，请检查地址和网络"
        AppError.NotAlistServer -> "该地址不是 Alist 服务"
        AppError.Unauthorized -> "登录已失效，请重新登录"
        AppError.PermissionDenied -> "没有访问权限"
        AppError.NotFound -> "文件不存在"
        AppError.Conflict -> "操作冲突，请重试"
        AppError.Timeout -> "请求超时，请重试"
        AppError.SSLError -> "TLS 握手失败，服务器证书可能不受信任"
        AppError.CertificateUntrusted -> "服务器证书不受信任（过期/自签/主机名不匹配）"
        AppError.Cancelled -> ""
        is AppError.OperationFailed -> error.message
        is AppError.Unknown -> "出错了，请重试"
    }

    fun toActionLabel(error: AppError): String? = when (error) {
        AppError.NetworkUnavailable,
        AppError.ServerUnreachable,
        AppError.Timeout,
        AppError.NotFound,
        AppError.Conflict -> "重试"
        AppError.Unauthorized -> "重新登录"
        else -> null
    }

    fun isRetryable(error: AppError): Boolean = when (error) {
        AppError.NetworkUnavailable,
        AppError.ServerUnreachable,
        AppError.Timeout,
        AppError.NotFound,
        AppError.Conflict -> true
        else -> false
    }
}
```

- [ ] **Step 4: Run tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.common.error.*"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/common app/src/test/java/com/textvision/alistclient/common
git commit -m "feat: add api and error mapping foundations"
```

---

### Task 1.3: Add auth DTOs, API interface, token provider, and Retrofit bindings

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/network/dto/AlistResponse.kt`
- Create: `app/src/main/java/com/textvision/alistclient/network/dto/AuthDtos.kt`
- Create: `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`
- Create: `app/src/main/java/com/textvision/alistclient/network/AuthTokenProvider.kt`
- Create: `app/src/main/java/com/textvision/alistclient/network/AuthInterceptor.kt`
- Create: `app/src/main/java/com/textvision/alistclient/network/SkipAuthRetry.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`

**Interfaces:**
- Produces: `AlistApi.login(LoginRequest): AlistResponse<AlistLoginData>`.
- Produces: `AuthTokenProvider` with `getToken()`, `setToken(token)`, `clearToken()`.
- Consumes: `CredentialStore` from Phase 0.

- [ ] **Step 1: Add DTOs and API interface**

`AlistResponse.kt`:

```kotlin
package com.textvision.alistclient.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AlistResponse<T>(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: T? = null,
)
```

`AuthDtos.kt`:

```kotlin
package com.textvision.alistclient.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class AlistLoginData(
    val token: String,
)
```

`AlistApi.kt`:

```kotlin
package com.textvision.alistclient.network.api

import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.LoginRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AlistApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): AlistResponse<AlistLoginData>
}
```

- [ ] **Step 2: Add token provider and interceptor**

`AuthTokenProvider.kt`:

```kotlin
package com.textvision.alistclient.network

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenProvider @Inject constructor() {
    @Volatile private var token: String? = null

    fun getToken(): String? = token
    fun setToken(value: String) { token = value }
    fun clearToken() { token = null }
}
```

`SkipAuthRetry.kt`:

```kotlin
package com.textvision.alistclient.network

object SkipAuthRetry {
    const val HEADER = "X-Skip-Auth-Retry"
}
```

`AuthInterceptor.kt`:

```kotlin
package com.textvision.alistclient.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenProvider: AuthTokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider.getToken()
        val request = chain.request()
        val builder = request.newBuilder().removeHeader(SkipAuthRetry.HEADER)
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}
```

- [ ] **Step 3: Add Retrofit and OkHttp Hilt providers**

Append to `AppModule.kt`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): kotlinx.serialization.json.Json = kotlinx.serialization.json.Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: com.textvision.alistclient.network.AuthInterceptor): okhttp3.OkHttpClient =
        okhttp3.OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: okhttp3.OkHttpClient,
        json: kotlinx.serialization.json.Json,
    ): retrofit2.Retrofit = retrofit2.Retrofit.Builder()
        .baseUrl("http://127.0.0.1:5244/")
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAlistApi(retrofit: retrofit2.Retrofit): com.textvision.alistclient.network.api.AlistApi =
        retrofit.create(com.textvision.alistclient.network.api.AlistApi::class.java)
}
```

Then add imports at top of `AppModule.kt` if not using fully qualified names:

```kotlin
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
```

- [ ] **Step 4: Build generated code**

```bash
./gradlew :app:assembleDebug
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/network app/src/main/java/com/textvision/alistclient/di/AppModule.kt
git commit -m "feat: add auth api network bindings"
```

---

### Task 1.4: Implement AuthRepository and SessionManager

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/auth/model/SavedSession.kt`
- Create: `app/src/main/java/com/textvision/alistclient/auth/SessionManager.kt`
- Create: `app/src/main/java/com/textvision/alistclient/auth/AuthRepository.kt`
- Test: `app/src/test/java/com/textvision/alistclient/auth/AuthRepositoryTest.kt`

**Interfaces:**
- Produces: `AuthRepository.login(serverUrl, username, password): ApiResult<SavedSession>`.
- Produces: `SessionManager.loadSavedSession()`, `saveSession`, `clearSession`.
- Consumes: `AlistApi`, `CredentialStore`, `AuthTokenProvider`, `ApiResult`.

- [ ] **Step 1: Write repository tests with fake API**

```kotlin
package com.textvision.alistclient.auth

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistLoginData
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.LoginRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    private class MemoryStore : CredentialStore {
        val map = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { map[key] = value }
        override fun readString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    private class FakeApi(private val response: AlistResponse<AlistLoginData>) : AlistApi {
        override suspend fun login(request: LoginRequest): AlistResponse<AlistLoginData> = response
    }

    @Test fun loginSuccessPersistsSessionAndToken() = runTest {
        val store = MemoryStore()
        val tokenProvider = AuthTokenProvider()
        val manager = SessionManager(store, tokenProvider)
        val repo = AuthRepository(FakeApi(AlistResponse(200, "success", AlistLoginData("tok"))), manager)

        val result = repo.login("http://server/", "admin", "pass")

        assertTrue(result is ApiResult.Success)
        assertEquals("tok", store.map[SessionManager.KEY_TOKEN])
        assertEquals("pass", store.map[SessionManager.KEY_PASSWORD])
        assertEquals("tok", tokenProvider.getToken())
    }

    @Test fun loginBusinessFailureDoesNotPersistPassword() = runTest {
        val store = MemoryStore()
        val manager = SessionManager(store, AuthTokenProvider())
        val repo = AuthRepository(FakeApi(AlistResponse(401, "unauthorized", null)), manager)

        val result = repo.login("http://server/", "admin", "bad")

        assertTrue(result is ApiResult.Failure)
        assertTrue(store.map.isEmpty())
    }
}
```

- [ ] **Step 2: Run test and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.auth.AuthRepositoryTest"
```

Expected: FAIL because classes do not exist.

- [ ] **Step 3: Implement session and repository**

`SavedSession.kt`:

```kotlin
package com.textvision.alistclient.auth.model

data class SavedSession(
    val serverUrl: String,
    val username: String,
    val password: String,
    val token: String,
)
```

`SessionManager.kt`:

```kotlin
package com.textvision.alistclient.auth

import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthTokenProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val credentialStore: CredentialStore,
    private val tokenProvider: AuthTokenProvider,
) {
    fun loadSavedSession(): SavedSession? {
        val serverUrl = credentialStore.readString(KEY_SERVER_URL) ?: return null
        val username = credentialStore.readString(KEY_USERNAME) ?: return null
        val password = credentialStore.readString(KEY_PASSWORD) ?: return null
        val token = credentialStore.readString(KEY_TOKEN) ?: return null
        tokenProvider.setToken(token)
        return SavedSession(serverUrl, username, password, token)
    }

    fun saveSession(session: SavedSession) {
        credentialStore.saveString(KEY_SERVER_URL, session.serverUrl)
        credentialStore.saveString(KEY_USERNAME, session.username)
        credentialStore.saveString(KEY_PASSWORD, session.password)
        credentialStore.saveString(KEY_TOKEN, session.token)
        tokenProvider.setToken(session.token)
    }

    fun clearToken() {
        credentialStore.remove(KEY_TOKEN)
        tokenProvider.clearToken()
    }

    fun clearSession() {
        credentialStore.clearAll()
        tokenProvider.clearToken()
    }

    companion object {
        const val KEY_SERVER_URL = "serverUrl"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_TOKEN = "token"
    }
}
```

`AuthRepository.kt`:

```kotlin
package com.textvision.alistclient.auth

import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.LoginRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: AlistApi,
    private val sessionManager: SessionManager,
) {
    suspend fun login(serverUrl: String, username: String, password: String): ApiResult<SavedSession> = try {
        val response = api.login(LoginRequest(username, password))
        if (response.code == 200 && response.data?.token?.isNotBlank() == true) {
            val session = SavedSession(serverUrl, username, password, response.data.token)
            sessionManager.saveSession(session)
            ApiResult.Success(session)
        } else {
            ApiResult.Failure(response.code, response.message)
        }
    } catch (t: Throwable) {
        ApiResult.NetworkError(t)
    }

    fun loadSavedSession(): SavedSession? = sessionManager.loadSavedSession()

    fun logout() {
        sessionManager.clearSession()
    }
}
```

- [ ] **Step 4: Run tests and verify pass**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.auth.AuthRepositoryTest"
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/auth app/src/test/java/com/textvision/alistclient/auth/AuthRepositoryTest.kt
git commit -m "feat: add auth repository and session manager"
```

---

### Task 1.5: Implement LoginViewModel and LoginScreen behavior

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/auth/LoginViewModel.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt`
- Test: `app/src/test/java/com/textvision/alistclient/auth/LoginViewModelTest.kt`

**Interfaces:**
- Produces: `LoginUiState`, `LoginViewModel.updateServerUrl`, `updateUsername`, `updatePassword`, `login`.
- Consumes: `AuthRepository`, `ServerUrlNormalizer`, `ErrorMessageMapper`.

- [ ] **Step 1: Write ViewModel tests**

```kotlin
package com.textvision.alistclient.auth

import app.cash.turbine.test
import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.common.result.ApiResult
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelTest {
    private class FakeAuthRepository : AuthRepositoryContract {
        var result: ApiResult<SavedSession> = ApiResult.Success(SavedSession("http://s/", "u", "p", "t"))
        override suspend fun login(serverUrl: String, username: String, password: String): ApiResult<SavedSession> = result
    }

    @Test fun httpWarningUpdatesFromServerUrl() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))
        vm.updateServerUrl("http://example.com")
        assertTrue(vm.uiState.value.showHttpWarning)
    }

    @Test fun invalidUrlShowsError() = runTest {
        val vm = LoginViewModel(FakeAuthRepository(), StandardTestDispatcher(testScheduler))
        vm.updateServerUrl("ftp://bad")
        vm.updateUsername("admin")
        vm.updatePassword("pw")
        vm.login()
        testScheduler.advanceUntilIdle()
        assertEquals("仅支持 HTTP 或 HTTPS", vm.uiState.value.errorMessage)
    }
}
```

- [ ] **Step 2: Run test and verify failure**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.auth.LoginViewModelTest"
```

Expected: FAIL because ViewModel contract/classes do not exist.

- [ ] **Step 3: Implement ViewModel and contract**

Create `LoginViewModel.kt`:

```kotlin
package com.textvision.alistclient.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.auth.model.SavedSession
import com.textvision.alistclient.common.error.ErrorMapper
import com.textvision.alistclient.common.error.ErrorMessageMapper
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.util.ServerUrlNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

interface AuthRepositoryContract {
    suspend fun login(serverUrl: String, username: String, password: String): ApiResult<SavedSession>
}

data class LoginUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val showHttpWarning: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepositoryContract,
) : ViewModel() {
    constructor(authRepository: AuthRepositoryContract, dispatcher: CoroutineDispatcher) : this(authRepository) {
        this.dispatcher = dispatcher
    }

    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateServerUrl(value: String) {
        _uiState.update { it.copy(serverUrl = value, showHttpWarning = ServerUrlNormalizer.isHttp(value), errorMessage = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit = {}) {
        val current = _uiState.value
        val normalized = ServerUrlNormalizer.normalize(current.serverUrl).getOrElse { error ->
            _uiState.update { it.copy(errorMessage = error.message ?: "服务器地址不合法") }
            return
        }
        if (current.username.isBlank() || current.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "用户名和密码不能为空") }
            return
        }
        viewModelScope.launch(dispatcher) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.login(normalized, current.username.trim(), current.password)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
                is ApiResult.Failure -> {
                    val error = ErrorMapper.mapAlistFailure(result.code, result.message)
                    val message = if (result.code == 401 || result.code == 400) "用户名或密码错误" else ErrorMessageMapper.toUserMessage(error)
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                }
                is ApiResult.NetworkError -> {
                    val error = ErrorMapper.mapThrowable(result.cause)
                    _uiState.update { it.copy(isLoading = false, errorMessage = ErrorMessageMapper.toUserMessage(error)) }
                }
            }
        }
    }
}
```

Modify `AuthRepository` declaration:

```kotlin
class AuthRepository @Inject constructor(...) : AuthRepositoryContract {
```

- [ ] **Step 4: Update LoginScreen**

Replace `LoginScreen.kt` with:

```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.auth.LoginViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(48.dp))
        Text("登录 Alist", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = viewModel::updateServerUrl,
            label = { Text("服务器地址") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        if (state.showHttpWarning) {
            Text(
                "⚠ 当前使用 HTTP 明文连接，账号密码可能被窃听",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.username,
            onValueChange = viewModel::updateUsername,
            label = { Text("用户名") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::updatePassword,
            label = { Text("密码") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        state.errorMessage?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.login(onLoginSuccess) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isLoading) CircularProgressIndicator() else Text("登录")
        }
    }
}
```

- [ ] **Step 5: Bind AuthRepositoryContract**

Add to `CredentialModule` in `AppModule.kt`:

```kotlin
@Binds
@Singleton
abstract fun bindAuthRepository(impl: com.textvision.alistclient.auth.AuthRepository): com.textvision.alistclient.auth.AuthRepositoryContract
```

- [ ] **Step 6: Run tests and build**

```bash
./gradlew :app:testDebugUnitTest --tests "com.textvision.alistclient.auth.LoginViewModelTest" :app:assembleDebug
```

Expected: PASS and `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/textvision/alistclient/auth app/src/main/java/com/textvision/alistclient/ui/screens/LoginScreen.kt app/src/main/java/com/textvision/alistclient/di/AppModule.kt app/src/test/java/com/textvision/alistclient/auth/LoginViewModelTest.kt
git commit -m "feat: implement login view model and screen"
```

---

## Phase 1 Completion Gate

Run:

```bash
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

Manual check on device/emulator:

1. Open App.
2. Enter `http://example.com` in server field.
3. Verify HTTP warning appears immediately.
4. Change to `https://example.com`.
5. Verify warning disappears.
