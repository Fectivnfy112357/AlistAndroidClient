# Admin: 存储管理 + 站点设置集成 — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在设置页集成 Alist v3 管理员功能：存储可改不可增删 + 站点设置常用项行内编辑 + 完整设置走二级页。

**Architecture:** 抽 `AdminRepository` 共享 admin API 鉴权刷新模式；动态表单 `FormItem` schema 复用 driver/settings 渲染；`SettingsScreen` 垂直堆叠 5 卡片；二级页 `StorageEditScreen`/`AdminSiteSettingsScreen` 用 `AppRoute` 路由 + Hilt 注入 ViewModel。

**Tech Stack:** Kotlin 2.0.21 + Compose BOM 2024.09.03 + Hilt 2.52 + Retrofit 2.11 + kotlinx.serialization 1.7 + JUnit 4 + MockK + Turbine + MockWebServer + Compose UI Test。

## Global Constraints

- 包名 `com.textvision.alistclient`，所有新文件路径前缀 `app/src/main/java/com/textvision/alistclient/` 或 `app/src/test/java/com/textvision/alistclient/`
- Kotlin target JVM 17，minSdk 26 / targetSdk 34
- 测试框架：JUnit 4 + MockK + Turbine + MockWebServer
- ViewModel 用 Hilt 注入（`@HiltViewModel`），Screen 用 `hiltViewModel()` 拿
- DI 在 `app/src/main/java/com/textvision/alistclient/di/AppModule.kt` 的 `CredentialModule` 加 `@Binds`
- 共享 admin 模式：所有 admin API 走 `AdminRepository.runAdmin`，自动 401 refresh + 重试一次
- Alist API base URL 默认 `http://127.0.0.1:5244/`，URL 拼接走 `sessionManager.loadSavedSession()?.serverUrl`
- 不可新增/删除存储；无 `form_items` 的设置项过滤不展示
- 提交策略：`storage/update` 用全量提交（拉原对象 + 合并改动）；`setting/save` 用 patch 列表
- 常用 4 个 setting key：`site_title`、`logo`、`login_background`、`announcement`（实施时按实际后端响应调整）
- Commits 用英文 conventional（feat/fix/test/refactor/chore）
- 任何 spec 标注"待 v3 确认"的项目，实施时实测决定，**单测用 MockWebServer 真实模拟响应**不依赖实际 v3 后端

## File Structure

新增：
- `admin/AdminRepository.kt` — 共享 admin API 模式
- `admin/AdminResult.kt` — sealed result 类型
- `admin/storage/StorageRepository.kt` + `StorageRepositoryContract.kt` + 配套 DTO 更新
- `admin/storage/StorageEditViewModel.kt`
- `admin/storage/StorageEditScreen.kt`
- `admin/settings/SettingsRepository.kt` + `SettingsRepositoryContract.kt` + 配套 DTO
- `admin/settings/AdminSiteSettingsViewModel.kt`
- `admin/settings/AdminSiteSettingsScreen.kt`
- `admin/form/FormItem.kt` — sealed schema
- `admin/form/DynamicFormField.kt` — Composable 渲染器
- `admin/storage/StorageRowItem.kt` — 列表行组件

修改：
- `network/api/AlistApi.kt` — 加 4 个端点
- `network/dto/AdminDtos.kt` — 加 DTO
- `home/HomeRepository.kt` — 改用 AdminRepository
- `ui/screens/SettingsScreen.kt` — 加卡片 4/5
- `ui/screens/SettingsViewModel.kt` — 注入新 repo + 状态
- `navigation/AppRoute.kt` — 加 2 个路由
- `navigation/AppNavHost.kt` — 加 2 个 composable
- `di/AppModule.kt` — 绑新 repo

测试：
- `test/.../admin/AdminRepositoryTest.kt`
- `test/.../admin/storage/StorageRepositoryTest.kt`
- `test/.../admin/storage/StorageEditViewModelTest.kt`
- `test/.../admin/settings/SettingsRepositoryTest.kt`
- `test/.../admin/settings/AdminSiteSettingsViewModelTest.kt`
- `test/.../admin/form/DynamicFormFieldTest.kt`
- `test/.../ui/screens/SettingsViewModelTest.kt`（重写扩展）

---

### Task 1: 抽 AdminRepository 共享 admin API 模式

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/admin/AdminResult.kt`
- Create: `app/src/main/java/com/textvision/alistclient/admin/AdminRepository.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/home/HomeRepository.kt`
- Test: `app/src/test/java/com/textvision/alistclient/admin/AdminRepositoryTest.kt`

**Interfaces:**
- Consumes: `AlistApi`, `SessionManager`, `AuthRepository`, `@IoDispatcher CoroutineDispatcher`
- Produces:
  - `sealed interface AdminResult<out T> { Ok<T>(data: T); object Unauthorized; data class ServerError(code: Int); object Network }`
  - `class AdminRepository { suspend fun <T : Any> runAdmin(base: String, call: suspend () -> AlistResponse<T>): AdminResult<T> }`

- [ ] **Step 1: 写失败测试 — AdminRepository 401 自动 refresh + 重试**

`AdminRepositoryTest.kt`:
```kotlin
package com.textvision.alistclient.admin

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.http.GET
import retrofit2.http.Url

@OptIn(ExperimentalCoroutinesApi::class)
class AdminRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AlistApi
    private lateinit var store: MemoryStore
    private lateinit var session: SessionManager
    private lateinit var tokenProvider: AuthTokenProvider
    private lateinit var repo: AdminRepository

    private class MemoryStore : CredentialStore {
        val map = linkedMapOf<String, String>()
        override fun saveString(key: String, value: String) { map[key] = value }
        override fun readString(key: String): String? = map[key]
        override fun remove(key: String) { map.remove(key) }
        override fun clearAll() { map.clear() }
    }

    interface ProbeApi {
        @GET suspend fun probe(@Url url: String): AlistResponse<Unit>
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
        repo = AdminRepository(api, session, AuthRepository(api, session), UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun unauthorizedTriggersRefreshAndRetriesSuccessfully() = runTest {
        var probeCount = 0
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                if (request.path?.contains("/api/auth/login") == true) {
                    return MockResponse().setResponseCode(200).setBody(
                        """{"code":200,"message":"success","data":{"token":"new_tok"}}"""
                    )
                }
                if (request.path?.contains("/api/admin/probe") == true) {
                    probeCount++
                    return if (probeCount == 1) {
                        MockResponse().setResponseCode(401).setBody("""{"code":401,"message":"x","data":null}""")
                    } else {
                        MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":null}""")
                    }
                }
                return MockResponse().setResponseCode(404)
            }
        }
        val probeApi = retrofit2.Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenProvider)).build())
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ProbeApi::class.java)
        val r = repo.runAdmin(server.url("/").toString()) { probeApi.probe("api/admin/probe") }
        assertTrue("expected Ok after refresh, got $r", r is AdminResult.Ok)
        assertEquals(2, probeCount)
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.AdminRepositoryTest`
Expected: FAIL (AdminRepository not defined)

- [ ] **Step 2: 创建 AdminResult sealed interface**

`AdminResult.kt`:
```kotlin
package com.textvision.alistclient.admin

sealed interface AdminResult<out T> {
    data class Ok<T>(val data: T) : AdminResult<T>
    data object Unauthorized : AdminResult<Nothing>
    data class ServerError(val code: Int) : AdminResult<Nothing>
    data object Network : AdminResult<Nothing>
}
```

- [ ] **Step 3: 创建 AdminRepository**

`AdminRepository.kt`:
```kotlin
package com.textvision.alistclient.admin

import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val api: AlistApi,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend fun <T : Any> runAdmin(
        base: String,
        call: suspend () -> AlistResponse<T>,
    ): AdminResult<T> = withContext(dispatcher) {
        val first = safeCall(call)
        if (first is AdminResult.Ok) return@withContext first
        if (first is AdminResult.Unauthorized) {
            val refreshed = refreshAndRetry(base)
            if (refreshed) return@withContext safeCall(call)
        }
        first
    }

    private suspend fun refreshAndRetry(base: String): Boolean {
        val session = sessionManager.loadSavedSession() ?: return false
        val username = session.username ?: return false
        val password = session.password ?: return false
        return when (val r = authRepository.login(base, username, password)) {
            is ApiResult.Success -> true
            else -> false
        }
    }

    private suspend fun <T : Any> safeCall(call: suspend () -> AlistResponse<T>): AdminResult<T> {
        return try {
            val resp = call()
            when {
                resp.code == 200 && resp.data != null -> AdminResult.Ok(resp.data)
                resp.code == 401 || resp.code == 403 -> AdminResult.Unauthorized
                else -> AdminResult.ServerError(resp.code)
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            AdminResult.Network
        }
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.AdminRepositoryTest`
Expected: PASS

- [ ] **Step 5: 修改 HomeRepository 复用 AdminRepository**

修改 `HomeRepository.kt`：把 `runAdmin`、`refreshAndRetry`、`safeCall` 三个 private 方法删除；注入 `AdminRepository`；把所有 `runAdmin(base) { ... }` 调用改为 `adminRepository.runAdmin(base) { ... }`；删除 `AdminResult` private sealed interface（用 `com.textvision.alistclient.admin.AdminResult`）。

构造函数参数列表改为：
```kotlin
@Inject constructor(
    private val api: AlistApi,
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val adminRepository: AdminRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
)
```

`runAdmin` 在文件内的所有调用从 `runAdmin(base) { api.xxx(url) }` 改为 `adminRepository.runAdmin(base) { api.xxx(url) }`。

各 `.toSection()` 扩展方法从 `AdminResult<T>` 改为 `com.textvision.alistclient.admin.AdminResult<T>`。

- [ ] **Step 6: 运行 HomeRepositoryTest 验证未回归**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRepositoryTest`
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/admin/AdminResult.kt \
        app/src/main/java/com/textvision/alistclient/admin/AdminRepository.kt \
        app/src/main/java/com/textvision/alistclient/home/HomeRepository.kt \
        app/src/test/java/com/textvision/alistclient/admin/AdminRepositoryTest.kt
git commit -m "refactor(admin): extract AdminRepository for shared admin API pattern"
```

---

### Task 2: 扩展 AlistApi 端点 + DTO（driver/settings/form schema）

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`
- Create: `app/src/main/java/com/textvision/alistclient/admin/form/FormItem.kt`
- Test: `app/src/test/java/com/textvision/alistclient/network/dto/AdminDtosTest.kt`（追加测试）

**Interfaces:**
- Consumes: 已有 AlistApi, AdminDtos
- Produces:
  - `AlistApi.updateStorage(url, body)`, `listDrivers(url)`, `listSettings(url)`, `saveSettings(url, body)`
  - DTO: `StoragePatch`, `DriverInfo`, `DriverList`, `SettingItem`, `SettingsList`, `SettingSaveRequest`
  - `sealed class FormItem` 见 schema

- [ ] **Step 1: 写失败测试 — 新 DTO 解析**

在 `AdminDtosTest.kt` 追加：
```kotlin
@Test fun driverInfoMapsSnakeCaseKeys() {
    val raw = """
        {"name":"Local","label":"本地存储","config_items":[
          {"name":"root_folder_path","label":"根目录","type":"string","default":"/","required":true},
          {"name":"enable_index","label":"生成索引","type":"bool","default":false}
        ]}
    """.trimIndent()
    val d = json.decodeFromString<DriverInfo>(raw)
    assertEquals("Local", d.name)
    assertEquals("本地存储", d.label)
    assertEquals(2, d.configItems?.size)
    assertEquals("root_folder_path", d.configItems!![0].name)
    assertEquals("string", d.configItems[0].type)
    assertEquals(true, d.configItems[0].required)
    assertEquals("bool", d.configItems[1].type)
    assertEquals(false, d.configItems[1].default)
}

@Test fun settingItemParsesGroupAndFormItems() {
    val raw = """
        {"key":"site_title","value":"My Alist","type":"string","group":"site","help":"站点标题",
         "form_items":[{"name":"site_title","label":"站点标题","type":"string","required":true}]}
    """.trimIndent()
    val s = json.decodeFromString<SettingItem>(raw)
    assertEquals("site_title", s.key)
    assertEquals("My Alist", s.value)
    assertEquals("site", s.group)
    assertEquals(1, s.formItems?.size)
    assertEquals("string", s.formItems!![0].type)
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.network.dto.AdminDtosTest`
Expected: FAIL（DriverInfo/SettingItem 未定义）

- [ ] **Step 2: 在 AdminDtos.kt 追加 DTO**

在文件末尾追加：
```kotlin
@Serializable
data class StoragePatch(
    val id: Long,
    @SerialName("mount_path") val mountPath: String,
    val driver: String,
    val order: Int = 0,
    val remark: String? = null,
    val enabled: Boolean = true,
    @SerialName("addition") val addition: String = "{}",
    @SerialName("cache_expiration") val cacheExpiration: Int = 0,
    @SerialName("web_proxy") val webProxy: Boolean = false,
    @SerialName("down_proxy_url") val downProxyUrl: String? = null,
)

@Serializable
data class DriverInfo(
    val name: String,
    val label: String? = null,
    @SerialName("config_items") val configItems: List<ConfigItem>? = null,
    @SerialName("additional") val additional: String? = null,
)

@Serializable
data class ConfigItem(
    val name: String,
    val label: String? = null,
    val type: String? = null,
    val default: kotlinx.serialization.json.JsonElement? = null,
    val options: kotlinx.serialization.json.JsonElement? = null,
    val required: Boolean = false,
    val help: String? = null,
)

@Serializable
data class DriverList(
    val content: List<DriverInfo> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class SettingItem(
    val key: String,
    val value: String? = null,
    val type: String? = null,
    val group: String? = null,
    val help: String? = null,
    @SerialName("form_items") val formItems: List<ConfigItem>? = null,
    val options: kotlinx.serialization.json.JsonElement? = null,
)

@Serializable
data class SettingsList(
    val content: List<SettingItem> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class SettingSaveItem(
    val key: String,
    val value: String,
)

@Serializable
data class SettingSaveRequest(
    val items: List<SettingSaveItem>,
)
```

- [ ] **Step 3: 在 AlistApi.kt 追加 4 个端点**

```kotlin
@POST
suspend fun updateStorage(@Url url: String, @Body body: StoragePatch): AlistResponse<Unit>

@GET
suspend fun listDrivers(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<DriverList>

@GET
suspend fun listSettings(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<SettingsList>

@POST
suspend fun saveSettings(@Url url: String, @Body body: SettingSaveRequest): AlistResponse<Unit>
```

import 块加：
```kotlin
import com.textvision.alistclient.network.dto.SettingSaveRequest
import com.textvision.alistclient.network.dto.StoragePatch
import com.textvision.alistclient.network.dto.DriverList
import com.textvision.alistclient.network.dto.SettingsList
```

- [ ] **Step 4: 创建 FormItem schema**

`FormItem.kt`:
```kotlin
package com.textvision.alistclient.admin.form

import com.textvision.alistclient.network.dto.ConfigItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

sealed class FormItem {
    abstract val name: String
    abstract val label: String
    abstract val required: Boolean

    data class Text(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class Url(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class TextArea(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class Bool(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class Number(
        override val name: String,
        override val label: String,
        override val required: Boolean = false,
    ) : FormItem()

    data class Select(
        override val name: String,
        override val label: String,
        val options: List<Pair<String, String>>,
        override val required: Boolean = false,
    ) : FormItem()

    data class MultiSelect(
        override val name: String,
        override val label: String,
        val options: List<Pair<String, String>>,
        override val required: Boolean = false,
    ) : FormItem()

    companion object {
        fun fromConfigItem(item: ConfigItem): FormItem {
            val name = item.name
            val label = item.label ?: name
            val required = item.required
            return when (item.type?.lowercase()) {
                "string" -> Text(name, label, required)
                "url" -> Url(name, label, required)
                "text" -> TextArea(name, label, required)
                "bool", "boolean" -> Bool(name, label, required)
                "number", "int", "integer", "float", "double" -> Number(name, label, required)
                "select" -> Select(name, label, parseOptions(item.options), required)
                "multi-select", "multi_select", "list", "strings" ->
                    MultiSelect(name, label, parseOptions(item.options), required)
                else -> Text(name, label, required) // 降级为 Text
            }
        }

        private fun parseOptions(raw: kotlinx.serialization.json.JsonElement?): List<Pair<String, String>> {
            if (raw == null) return emptyList()
            return try {
                val arr = (raw as? JsonArray) ?: return emptyList()
                arr.map { element ->
                    val obj = element as? JsonObject
                    val value = obj?.get("value")?.jsonPrimitive?.content ?: element.jsonPrimitive.content
                    val label = obj?.get("label")?.jsonPrimitive?.content ?: value
                    value to label
                }
            } catch (t: Throwable) {
                emptyList()
            }
        }
    }
}
```

- [ ] **Step 5: 运行 DTO 测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.network.dto.AdminDtosTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt \
        app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt \
        app/src/main/java/com/textvision/alistclient/admin/form/FormItem.kt \
        app/src/test/java/com/textvision/alistclient/network/dto/AdminDtosTest.kt
git commit -m "feat(admin): add storage/settings DTOs and FormItem schema"
```

---

### Task 3: DynamicFormField Composable + 测试

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt`
- Test: `app/src/test/java/com/textvision/alistclient/admin/form/DynamicFormFieldTest.kt`

**Interfaces:**
- Consumes: `FormItem`, current value (`Any?`), `onValueChange: (Any?) -> Unit`
- Produces: `@Composable fun DynamicFormField(item: FormItem, value: Any?, onValueChange: (Any?) -> Unit)`

- [ ] **Step 1: 写失败测试 — 7 种类型 + 未知降级**

`DynamicFormFieldTest.kt`:
```kotlin
package com.textvision.alistclient.admin.form

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class DynamicFormFieldTest {
    @get:Rule val rule = createComposeRule()

    private fun render(item: FormItem, value: Any? = null, onChange: (Any?) -> Unit = {}) {
        rule.setContent { DynamicFormField(item, value, onChange) }
    }

    @Test fun textFieldRendersLabelAndAcceptsInput() {
        var captured: Any? = null
        render(FormItem.Text("name", "用户名", required = true), value = "", onChange = { captured = it })
        rule.onNodeWithText("用户名").assertExists()
        rule.onNodeWithText("用户名").performTextInput("alice")
        rule.mainClock.autoAdvance = false
    }

    @Test fun textAreaRendersLabel() {
        render(FormItem.TextArea("remark", "备注"))
        rule.onNodeWithText("备注").assertExists()
    }

    @Test fun urlFieldRendersLabel() {
        render(FormItem.Url("logo", "Logo URL"))
        rule.onNodeWithText("Logo URL").assertExists()
    }

    @Test fun boolFieldRendersLabel() {
        render(FormItem.Bool("enabled", "启用"))
        rule.onNodeWithText("启用").assertExists()
    }

    @Test fun numberFieldRendersLabel() {
        render(FormItem.Number("port", "端口"))
        rule.onNodeWithText("端口").assertExists()
    }

    @Test fun selectFieldRendersLabel() {
        render(FormItem.Select("driver", "Driver", listOf("Local" to "Local", "S3" to "S3")))
        rule.onNodeWithText("Driver").assertExists()
    }

    @Test fun multiSelectFieldRendersLabel() {
        render(FormItem.MultiSelect("tags", "标签", listOf("a" to "A", "b" to "B")))
        rule.onNodeWithText("标签").assertExists()
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.form.DynamicFormFieldTest`
Expected: FAIL（DynamicFormField 未定义）

- [ ] **Step 2: 实现 DynamicFormField Composable**

`DynamicFormField.kt`:
```kotlin
package com.textvision.alistclient.admin.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicFormField(
    item: FormItem,
    value: Any?,
    onValueChange: (Any?) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (item) {
        is FormItem.Text -> TextFieldRow(
            label = item.label,
            value = (value as? String).orEmpty(),
            keyboardType = KeyboardType.Text,
            onChange = onValueChange,
            modifier = modifier,
        )
        is FormItem.Url -> TextFieldRow(
            label = item.label,
            value = (value as? String).orEmpty(),
            keyboardType = KeyboardType.Uri,
            onChange = onValueChange,
            modifier = modifier,
        )
        is FormItem.TextArea -> Column(modifier.padding(vertical = 4.dp)) {
            Text(item.label, style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = (value as? String).orEmpty(),
                onValueChange = { onValueChange(it) },
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                minLines = 3,
            )
        }
        is FormItem.Bool -> BoolRow(
            label = item.label,
            checked = (value as? Boolean) ?: false,
            onChange = onValueChange,
            modifier = modifier,
        )
        is FormItem.Number -> TextFieldRow(
            label = item.label,
            value = value?.toString().orEmpty(),
            keyboardType = KeyboardType.Decimal,
            onChange = { onValueChange(it) },
            modifier = modifier,
        )
        is FormItem.Select -> SelectRow(
            label = item.label,
            options = item.options,
            value = (value as? String).orEmpty(),
            onChange = onValueChange,
            modifier = modifier,
        )
        is FormItem.MultiSelect -> MultiSelectRow(
            label = item.label,
            options = item.options,
            values = (value as? List<String>).orEmpty(),
            onChange = onValueChange,
            modifier = modifier,
        )
    }
}

@Composable
private fun TextFieldRow(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            singleLine = keyboardType != KeyboardType.Text || true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        )
    }
}

@Composable
private fun BoolRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectRow(
    label: String,
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = options.firstOrNull { it.first == value }?.second ?: value
    Column(modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        ) {
            OutlinedTextField(
                value = displayText,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )
            androidx.compose.material3.ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                options.forEach { (key, labelText) ->
                    DropdownMenuItem(
                        text = { Text(labelText) },
                        onClick = {
                            onChange(key)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiSelectRow(
    label: String,
    options: List<Pair<String, String>>,
    values: List<String>,
    onChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { (key, labelText) ->
                val selected = key in values
                FilterChip(
                    selected = selected,
                    onClick = {
                        val next = if (selected) values - key else values + key
                        onChange(next)
                    },
                    label = { Text(labelText) },
                )
            }
        }
    }
}
```

- [ ] **Step 3: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.form.DynamicFormFieldTest`
Expected: PASS

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt \
        app/src/test/java/com/textvision/alistclient/admin/form/DynamicFormFieldTest.kt
git commit -m "feat(admin): add DynamicFormField Composable for dynamic forms"
```

---

### Task 4: StorageRepository + StorageRepositoryContract

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/admin/storage/StorageRepository.kt`
- Create: `app/src/main/java/com/textvision/alistclient/admin/storage/StorageRepositoryContract.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`
- Test: `app/src/test/java/com/textvision/alistclient/admin/storage/StorageRepositoryTest.kt`

**Interfaces:**
- Consumes: `AlistApi`, `AdminRepository`, `SessionManager`, `@IoDispatcher`
- Produces:
  ```kotlin
  interface StorageRepositoryContract {
      suspend fun list(base: String): AdminResult<StorageList>
      suspend fun update(base: String, patch: StoragePatch): AdminResult<Unit>
      suspend fun listDrivers(base: String): AdminResult<List<DriverInfo>>
  }
  ```

- [ ] **Step 1: 写失败测试 — list/update/listDrivers**

`StorageRepositoryTest.kt`:
```kotlin
package com.textvision.alistclient.admin.storage

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.DriverList
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AlistApi
    private lateinit var store: MemoryStore
    private lateinit var session: SessionManager
    private lateinit var tokenProvider: AuthTokenProvider
    private lateinit var repo: StorageRepository

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
        val adminRepo = AdminRepository(api, session, AuthRepository(api, session), UnconfinedTestDispatcher())
        repo = StorageRepository(api, adminRepo, UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun listParsesStorages() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"code":200,"message":"success","data":{"content":[{"id":1,"mount_path":"/local","driver":"Local","status":"work"}],"total":1}}"""
        ))
        val r = repo.list(server.url("/").toString())
        assertTrue(r is com.textvision.alistclient.admin.AdminResult.Ok)
        val data = (r as com.textvision.alistclient.admin.AdminResult.Ok).data as StorageList
        assertEquals(1, data.content.size)
        assertEquals("/local", data.content[0].mountPath)
    }

    @Test fun updatePostsPatch() = runTest {
        var capturedPath: String? = null
        var capturedBody: String? = null
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                capturedPath = request.path
                capturedBody = request.body.readUtf8()
                return MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":null}""")
            }
        }
        val patch = StoragePatch(id = 1, mountPath = "/local", driver = "Local", enabled = false)
        val r = repo.update(server.url("/").toString(), patch)
        assertTrue(r is com.textvision.alistclient.admin.AdminResult.Ok)
        assertTrue("path: $capturedPath", capturedPath!!.contains("/api/admin/storage/update"))
        assertTrue("body: $capturedBody", capturedBody!!.contains("\"enabled\":false"))
    }

    @Test fun listDriversReturnsDriverInfo() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"code":200,"message":"success","data":{"content":[{"name":"Local","label":"本地存储","config_items":[]}],"total":1}}"""
        ))
        val r = repo.listDrivers(server.url("/").toString())
        assertTrue(r is com.textvision.alistclient.admin.AdminResult.Ok)
        val drivers = (r as com.textvision.alistclient.admin.AdminResult.Ok).data
        assertEquals(1, drivers.size)
        assertEquals("Local", drivers[0].name)
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.storage.StorageRepositoryTest`
Expected: FAIL（StorageRepository 未定义）

- [ ] **Step 2: 创建 Contract**

`StorageRepositoryContract.kt`:
```kotlin
package com.textvision.alistclient.admin.storage

import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch

interface StorageRepositoryContract {
    suspend fun list(base: String): AdminResult<StorageList>
    suspend fun update(base: String, patch: StoragePatch): AdminResult<Unit>
    suspend fun listDrivers(base: String): AdminResult<List<DriverInfo>>
}
```

- [ ] **Step 3: 创建实现**

`StorageRepository.kt`:
```kotlin
package com.textvision.alistclient.admin.storage

import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.DriverList
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val api: AlistApi,
    private val adminRepository: AdminRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : StorageRepositoryContract {
    override suspend fun list(base: String): AdminResult<StorageList> =
        adminRepository.runAdmin(base) { api.listStorage("${base}api/admin/storage/list") }

    override suspend fun update(base: String, patch: StoragePatch): AdminResult<Unit> =
        withContext(dispatcher) {
            adminRepository.runAdmin(base) { api.updateStorage("${base}api/admin/storage/update", patch) }
        }

    override suspend fun listDrivers(base: String): AdminResult<List<DriverInfo>> =
        when (val r = adminRepository.runAdmin(base) { api.listDrivers("${base}api/admin/driver/list") }) {
            is AdminResult.Ok -> AdminResult.Ok((r.data as DriverList).content)
            else -> @Suppress("UNCHECKED_CAST") (r as AdminResult<Nothing>)
        }
}
```

- [ ] **Step 4: AppModule.kt 绑 DI**

在 `CredentialModule` 块内追加（按已有 `@Binds` 风格）：
```kotlin
@Binds
@Singleton
abstract fun bindStorageRepository(impl: StorageRepository): StorageRepositoryContract
```

import 块加：
```kotlin
import com.textvision.alistclient.admin.storage.StorageRepository
import com.textvision.alistclient.admin.storage.StorageRepositoryContract
```

- [ ] **Step 5: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.storage.StorageRepositoryTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/admin/storage/StorageRepository.kt \
        app/src/main/java/com/textvision/alistclient/admin/storage/StorageRepositoryContract.kt \
        app/src/main/java/com/textvision/alistclient/di/AppModule.kt \
        app/src/test/java/com/textvision/alistclient/admin/storage/StorageRepositoryTest.kt
git commit -m "feat(admin): add StorageRepository with list/update/drivers"
```

---

### Task 5: SettingsRepository + SettingGroup 模型

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/admin/settings/SettingsRepository.kt`
- Create: `app/src/main/java/com/textvision/alistclient/admin/settings/SettingsRepositoryContract.kt`
- Create: `app/src/main/java/com/textvision/alistclient/admin/settings/SettingGroup.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/di/AppModule.kt`
- Test: `app/src/test/java/com/textvision/alistclient/admin/settings/SettingsRepositoryTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  interface SettingsRepositoryContract {
      suspend fun list(base: String): AdminResult<List<SettingGroup>>
      suspend fun save(base: String, patches: List<Pair<String, String>>): AdminResult<Unit>
  }
  data class SettingGroup(val key: String, val items: List<SettingItem>)
  ```

- [ ] **Step 1: 写失败测试 — group 聚合 + save**

`SettingsRepositoryTest.kt`:
```kotlin
package com.textvision.alistclient.admin.settings

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var api: AlistApi
    private lateinit var store: MemoryStore
    private lateinit var session: SessionManager
    private lateinit var tokenProvider: AuthTokenProvider
    private lateinit var repo: SettingsRepository

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
        val adminRepo = AdminRepository(api, session, AuthRepository(api, session), UnconfinedTestDispatcher())
        repo = SettingsRepository(api, adminRepo, UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    @Test fun listGroupsByKeyAndFiltersEmptyFormItems() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"code":200,"message":"success","data":{
              "content":[
                {"key":"site_title","value":"My Alist","group":"site",
                 "form_items":[{"name":"site_title","label":"站点标题","type":"string","required":true}]},
                {"key":"raw_token","value":"xyz","group":"aria2","form_items":null}
              ],
              "total":2
            }}
        """.trimIndent()))
        val r = repo.list(server.url("/").toString())
        assertTrue(r is AdminResult.Ok)
        val groups = (r as AdminResult.Ok).data
        assertEquals(1, groups.size)
        assertEquals("site", groups[0].key)
        assertEquals(1, groups[0].items.size)
        assertEquals("site_title", groups[0].items[0].key)
    }

    @Test fun savePostsPatchesAsItems() = runTest {
        var capturedBody: String? = null
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                capturedBody = request.body.readUtf8()
                return MockResponse().setResponseCode(200).setBody("""{"code":200,"message":"success","data":null}""")
            }
        }
        val r = repo.save(server.url("/").toString(), listOf("site_title" to "New Title"))
        assertTrue(r is AdminResult.Ok)
        assertTrue("body: $capturedBody", capturedBody!!.contains("\"key\":\"site_title\""))
        assertTrue("body: $capturedBody", capturedBody!!.contains("\"value\":\"New Title\""))
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.settings.SettingsRepositoryTest`
Expected: FAIL

- [ ] **Step 2: 创建 SettingGroup**

`SettingGroup.kt`:
```kotlin
package com.textvision.alistclient.admin.settings

import com.textvision.alistclient.network.dto.SettingItem

data class SettingGroup(
    val key: String,
    val items: List<SettingItem>,
)
```

- [ ] **Step 3: 创建 Contract**

`SettingsRepositoryContract.kt`:
```kotlin
package com.textvision.alistclient.admin.settings

import com.textvision.alistclient.admin.AdminResult

interface SettingsRepositoryContract {
    suspend fun list(base: String): AdminResult<List<SettingGroup>>
    suspend fun save(base: String, patches: List<Pair<String, String>>): AdminResult<Unit>
}
```

- [ ] **Step 4: 创建实现**

`SettingsRepository.kt`:
```kotlin
package com.textvision.alistclient.admin.settings

import com.textvision.alistclient.admin.AdminRepository
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.SettingSaveItem
import com.textvision.alistclient.network.dto.SettingSaveRequest
import com.textvision.alistclient.network.dto.SettingsList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val api: AlistApi,
    private val adminRepository: AdminRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : SettingsRepositoryContract {
    override suspend fun list(base: String): AdminResult<List<SettingGroup>> = withContext(dispatcher) {
        when (val r = adminRepository.runAdmin(base) { api.listSettings("${base}api/admin/setting/list") }) {
            is AdminResult.Ok -> {
                val list = r.data as SettingsList
                val filtered = list.content.filter { !it.formItems.isNullOrEmpty() }
                val groups = filtered
                    .groupBy { it.group ?: "_default" }
                    .map { (k, v) -> SettingGroup(key = k, items = v) }
                    .sortedBy { it.key }
                AdminResult.Ok(groups)
            }
            is AdminResult.Unauthorized -> AdminResult.Unauthorized
            is AdminResult.ServerError -> AdminResult.ServerError(r.code)
            is AdminResult.Network -> AdminResult.Network
        }
    }

    override suspend fun save(base: String, patches: List<Pair<String, String>>): AdminResult<Unit> =
        withContext(dispatcher) {
            val body = SettingSaveRequest(items = patches.map { (k, v) -> SettingSaveItem(k, v) })
            adminRepository.runAdmin(base) { api.saveSettings("${base}api/admin/setting/save", body) }
        }
}
```

- [ ] **Step 5: AppModule.kt 绑 DI**

在 `CredentialModule` 块内追加：
```kotlin
@Binds
@Singleton
abstract fun bindSettingsRepository(impl: SettingsRepository): SettingsRepositoryContract
```

import 块加：
```kotlin
import com.textvision.alistclient.admin.settings.SettingsRepository
import com.textvision.alistclient.admin.settings.SettingsRepositoryContract
```

- [ ] **Step 6: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.settings.SettingsRepositoryTest`
Expected: PASS

- [ ] **Step 7: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/admin/settings/SettingsRepository.kt \
        app/src/main/java/com/textvision/alistclient/admin/settings/SettingsRepositoryContract.kt \
        app/src/main/java/com/textvision/alistclient/admin/settings/SettingGroup.kt \
        app/src/main/java/com/textvision/alistclient/di/AppModule.kt \
        app/src/test/java/com/textvision/alistclient/admin/settings/SettingsRepositoryTest.kt
git commit -m "feat(admin): add SettingsRepository with group aggregation"
```

---

### Task 6: SettingsViewModel 改造 + StorageRowItem

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/SettingsViewModel.kt`
- Create: `app/src/main/java/com/textvision/alistclient/admin/storage/StorageRowItem.kt`
- Test: `app/src/test/java/com/textvision/alistclient/ui/screens/SettingsViewModelTest.kt`（覆盖现有）

**Interfaces:**
- Produces:
  - `SettingsViewModel.uiState: StateFlow<SettingsUiState>`
  - `SettingsViewModel.loadAdminData()` — 并行拉存储 + 常用设置
  - `SettingsViewModel.toggleStorage(id, enabled)` — 行内启/停
  - `SettingsViewModel.saveQuickSetting(key, value)` — 常用 4 项保存

- [ ] **Step 1: 写失败测试 — 状态 + 启/停成功 + 启/停失败回滚**

`SettingsViewModelTest.kt`（替换原文件）：
```kotlin
package com.textvision.alistclient.ui.screens

import app.cash.turbine.test
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.admin.settings.SettingsRepositoryContract
import com.textvision.alistclient.admin.storage.StorageRepositoryContract
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val authRepo: AuthRepository = mockk(relaxed = true)
    private val transfer: TransferManager = mockk(relaxed = true)
    private val preview: PreviewFileStore = mockk(relaxed = true)
    private val storage: StorageRepositoryContract = mockk(relaxed = true)
    private val settings: SettingsRepositoryContract = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = SettingsViewModel(
        authRepository = authRepo,
        transferManager = transfer,
        previewFileStore = preview,
        storageRepository = storage,
        settingsRepository = settings,
    )

    @Test fun loadAdminDataPopulatesUiStateOnSuccess() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { settings.list(any()) } returns AdminResult.Ok(emptyList())
        val viewModel = vm()
        viewModel.uiState.test {
            // initial loading
            var s = awaitItem()
            viewModel.loadAdminData()
            s = awaitItem()
            assertTrue(s.storages.isNotEmpty())
            assertEquals("/local", s.storages[0].mountPath)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun toggleStorageSuccessUpdatesItem() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { storage.update(any(), any()) } returns AdminResult.Ok(Unit)
        val viewModel = vm()
        viewModel.loadAdminData()
        viewModel.uiState.test {
            var s = awaitItem()
            val initialEnabled = s.storages[0].enabled
            viewModel.toggleStorage(id = 1, enabled = !initialEnabled)
            coVerify { storage.update(any(), match { it.id == 1L && it.enabled == !initialEnabled }) }
            s = awaitItem()
            assertEquals(!initialEnabled, s.storages[0].enabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun toggleStorageFailureRollsBack() = runTest {
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { storage.update(any(), any()) } returns AdminResult.ServerError(500)
        val viewModel = vm()
        viewModel.loadAdminData()
        viewModel.uiState.test {
            var s = awaitItem()
            val initialEnabled = s.storages[0].enabled
            viewModel.toggleStorage(id = 1, enabled = !initialEnabled)
            s = awaitItem()
            assertEquals(initialEnabled, s.storages[0].enabled)
            assertTrue(s.errorMessage?.contains("500") == true)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.ui.screens.SettingsViewModelTest`
Expected: FAIL（构造函数/状态不匹配）

- [ ] **Step 2: 改造 SettingsViewModel**

`SettingsViewModel.kt`（完整替换）：
```kotlin
package com.textvision.alistclient.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.admin.settings.SettingsRepositoryContract
import com.textvision.alistclient.admin.storage.StorageRepositoryContract
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StoragePatch
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val storages: List<StorageInfo> = emptyList(),
    val quickSettings: List<SettingItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val transferManager: TransferManager,
    private val previewFileStore: PreviewFileStore,
    private val storageRepository: StorageRepositoryContract,
    private val settingsRepository: SettingsRepositoryContract,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadAdminData() {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val storageResult = storageRepository.list(base)
            val settingsResult = settingsRepository.list(base)
            val storages = (storageResult as? AdminResult.Ok)?.data?.content.orEmpty()
            val allItems = (settingsResult as? AdminResult.Ok)?.data.orEmpty().flatMap { it.items }
            val quick = allItems.filter { it.key in QUICK_KEYS }
            _uiState.update {
                it.copy(
                    storages = storages,
                    quickSettings = quick,
                    isLoading = false,
                    errorMessage = failureMessage(storageResult, settingsResult),
                )
            }
        }
    }

    fun toggleStorage(id: Long, enabled: Boolean) {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        val current = _uiState.value.storages.firstOrNull { it.id == id } ?: return
        val previous = current
        _uiState.update { state ->
            state.copy(storages = state.storages.map { if (it.id == id) it.copy(status = if (enabled) "work" else "disabled") else it })
        }
        viewModelScope.launch {
            val patch = StoragePatch(
                id = id,
                mountPath = current.mountPath,
                driver = current.driver,
                enabled = enabled,
                addition = current.addition ?: "{}",
            )
            when (val r = storageRepository.update(base, patch)) {
                is AdminResult.Ok -> Unit
                else -> {
                    _uiState.update { state ->
                        state.copy(
                            storages = state.storages.map { if (it.id == id) previous else it },
                            errorMessage = failureMessage(r),
                        )
                    }
                }
            }
        }
    }

    fun saveQuickSetting(key: String, value: String) {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            when (val r = settingsRepository.save(base, listOf(key to value))) {
                is AdminResult.Ok -> {
                    _uiState.update { state ->
                        state.copy(
                            quickSettings = state.quickSettings.map { if (it.key == key) it.copy(value = value) else it },
                            errorMessage = null,
                        )
                    }
                }
                else -> _uiState.update { it.copy(errorMessage = failureMessage(r)) }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun logout() {
        transferManager.clearAllTasks()
        authRepository.logout()
        previewFileStore.clearPreviewFiles()
        _loggedOut.value = true
    }

    fun clearPreviewFiles(): Int = previewFileStore.clearPreviewFiles()

    companion object {
        val QUICK_KEYS = setOf("site_title", "logo", "login_background", "announcement")

        private fun failureMessage(vararg results: AdminResult<*>): String? = results
            .firstOrNull { it !is AdminResult.Ok && it !is AdminResult.Unauthorized }
            ?.let { "加载失败：${it.javaClass.simpleName}" }
    }
}
```

**注意**：`StorageInfo` 当前没 `addition` 字段；先在 `AdminDtos.kt` 给 `StorageInfo` 加 `addition: String? = null`，如：
```kotlin
@SerialName("addition") val addition: String? = null,
```

并对原 AdminDtosTest 不产生影响（nullable 默认 null）。

- [ ] **Step 3: 创建 StorageRowItem**

`StorageRowItem.kt`:
```kotlin
package com.textvision.alistclient.admin.storage

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.theme.CloudPrimary

@Composable
fun StorageRowItem(
    storage: StorageInfo,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = storage.status != "disabled"
    CloudListItem(
        title = storage.mountPath,
        subtitle = storage.driver,
        modifier = modifier,
        onClick = onClick,
        leading = {
            Icon(Icons.Outlined.Storage, contentDescription = null, tint = CloudPrimary, modifier = Modifier.size(24.dp))
        },
        trailing = {
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        },
    )
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.ui.screens.SettingsViewModelTest`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/screens/SettingsViewModel.kt \
        app/src/main/java/com/textvision/alistclient/admin/storage/StorageRowItem.kt \
        app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt \
        app/src/test/java/com/textvision/alistclient/ui/screens/SettingsViewModelTest.kt
git commit -m "feat(admin): extend SettingsViewModel with storage/quick settings"
```

---

### Task 7: SettingsScreen 改造 — 卡片 4（存储）+ 卡片 5（站点设置）

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt`

**Interfaces:**
- Consumes: `SettingsViewModel.uiState`
- Produces: SettingsScreen 渲染新增 2 卡片 + 跳转二级页

- [ ] **Step 1: 写失败测试 — Screen 渲染（基础编译）**

不需要新单元测试。本任务纯 UI 改造 + 路由。验证手段：编译通过 + 端到端（spec 列出的人工验收清单）。

- [ ] **Step 2: 在 AppRoute.kt 加 2 个路由**

```kotlin
data object StorageEdit : AppRoute("admin/storage_edit/{id}") {
    fun create(id: Long): String = "admin/storage_edit/$id"
}
data object AdminSiteSettings : AppRoute("admin/site_settings")
```

- [ ] **Step 3: 在 AppNavHost.kt 暂不接 — 先放骨架**

AppNavHost 的 composable 块**留到 Task 8/9 接入**。本任务只动 SettingsScreen。

- [ ] **Step 4: 改造 SettingsScreen**

`SettingsScreen.kt`（完整替换）：
```kotlin
package com.textvision.alistclient.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.storage.StorageRowItem
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudListItem
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.theme.CloudErrorText
import com.textvision.alistclient.ui.theme.CloudPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit = {},
    onStorageClick: (Long) -> Unit = {},
    onAdvancedSettings: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val loggedOut by viewModel.loggedOut.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val message = remember { mutableStateOf<String?>(null) }
    var editingSetting by remember { mutableStateOf<SettingItem?>(null) }

    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    LaunchedEffect(Unit) { viewModel.loadAdminData() }

    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "设置", subtitle = "账号与服务器管理")
        CloudCard {
            CloudListItem(
                title = "当前服务器",
                subtitle = "已登录的 Alist 服务",
                leading = { Icon(Icons.Outlined.Storage, contentDescription = null, tint = CloudPrimary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        CloudCard {
            CloudListItem(
                title = "清理临时预览文件",
                subtitle = "释放本机预览缓存",
                onClick = {
                    val count = viewModel.clearPreviewFiles()
                    message.value = "已清理 $count 个临时文件"
                },
                leading = { Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = CloudPrimary) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
            CloudListItem(
                title = "退出登录",
                subtitle = "清除当前会话并返回登录页",
                onClick = {
                    viewModel.logout()
                    message.value = "已退出登录"
                },
                leading = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = CloudErrorText) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        CloudCard {
            CloudListItem(
                title = "存储",
                subtitle = "${uiState.storages.size} 个存储",
                leading = { Icon(Icons.Outlined.Storage, contentDescription = null, tint = CloudPrimary) },
            )
            uiState.storages.forEach { s ->
                StorageRowItem(
                    storage = s,
                    onClick = { s.id?.let(onStorageClick) },
                    onToggle = { checked -> s.id?.let { viewModel.toggleStorage(it, checked) } },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        CloudCard {
            CloudListItem(
                title = "站点设置",
                subtitle = "${uiState.quickSettings.size} 个常用项",
                leading = { Icon(Icons.Outlined.Language, contentDescription = null, tint = CloudPrimary) },
            )
            uiState.quickSettings.forEach { item ->
                CloudListItem(
                    title = displayLabel(item.key),
                    subtitle = item.value?.takeIf { it.isNotBlank() } ?: "(未设置)",
                    onClick = { editingSetting = item },
                    trailing = { Text("›", color = CloudTextSecondary) },
                )
            }
            CloudListItem(
                title = "完整设置",
                subtitle = "所有带表单的设置项",
                onClick = onAdvancedSettings,
                leading = { Icon(Icons.Outlined.Settings, contentDescription = null, tint = CloudPrimary) },
                trailing = { Text("›", color = CloudTextSecondary) },
            )
        }
        Spacer(Modifier.height(10.dp))
        uiState.errorMessage?.let {
            CloudStatusBanner(text = it, kind = CloudBannerKind.Error)
        }
        message.value?.let {
            CloudStatusBanner(text = it, kind = CloudBannerKind.Info)
        }
    }

    editingSetting?.let { item ->
        QuickSettingDialog(
            item = item,
            onDismiss = { editingSetting = null },
            onSave = { newValue ->
                viewModel.saveQuickSetting(item.key, newValue)
                editingSetting = null
            },
        )
    }
}

@Composable
private fun QuickSettingDialog(
    item: SettingItem,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var text by remember { mutableStateOf(item.value.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(displayLabel(item.key)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = if (item.key == "announcement") 3 else 1,
            )
        },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun displayLabel(key: String): String = when (key) {
    "site_title" -> "站点标题"
    "logo" -> "Logo URL"
    "login_background" -> "登录页背景图"
    "announcement" -> "登录页公告"
    else -> key
}
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/ui/screens/SettingsScreen.kt \
        app/src/main/java/com/textvision/alistclient/navigation/AppRoute.kt
git commit -m "feat(admin): add storage + site settings cards to SettingsScreen"
```

---

### Task 8: StorageEditScreen + ViewModel + 路由接入

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/admin/storage/StorageEditViewModel.kt`
- Create: `app/src/main/java/com/textvision/alistclient/admin/storage/StorageEditScreen.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Test: `app/src/test/java/com/textvision/alistclient/admin/storage/StorageEditViewModelTest.kt`

**Interfaces:**
- Produces:
  - `StorageEditViewModel.uiState: StateFlow<StorageEditUiState>` (Loading / Form / Error)
  - `StorageEditViewModel.load(id)`, `save()`

- [ ] **Step 1: 写失败测试 — load + save**

`StorageEditViewModelTest.kt`:
```kotlin
package com.textvision.alistclient.admin.storage

import app.cash.turbine.test
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.StoragePatch
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StorageEditViewModelTest {
    private val auth: AuthRepository = mockk(relaxed = true)
    private val transfer: TransferManager = mockk(relaxed = true)
    private val preview: PreviewFileStore = mockk(relaxed = true)
    private val storage: StorageRepositoryContract = mockk(relaxed = true)
    private val session: SessionManager = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun loadPopulatesFormWithDriverItems() = runTest {
        coEvery { session.loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://x/", username = "u", password = "p", token = "t"
        )
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work", addition = "{\"root_folder_path\":\"/data\"}")))
        )
        coEvery { storage.listDrivers(any()) } returns AdminResult.Ok(
            listOf(DriverInfo(name = "Local", label = "本地存储", configItems = emptyList()))
        )
        val vm = StorageEditViewModel(auth, transfer, preview, storage, session)
        vm.load(1)
        vm.uiState.test {
            var s = awaitItem()
            assertTrue(s is StorageEditUiState.Form)
            s = s as StorageEditUiState.Form
            assertEquals("/local", s.storage.mountPath)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun saveCallsUpdate() = runTest {
        coEvery { session.loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://x/", username = "u", password = "p", token = "t"
        )
        coEvery { storage.list(any()) } returns AdminResult.Ok(
            StorageList(content = listOf(StorageInfo(id = 1, mountPath = "/local", driver = "Local", status = "work")))
        )
        coEvery { storage.listDrivers(any()) } returns AdminResult.Ok(emptyList())
        coEvery { storage.update(any(), any()) } returns AdminResult.Ok(Unit)
        val vm = StorageEditViewModel(auth, transfer, preview, storage, session)
        vm.load(1)
        vm.save()
        coVerify { storage.update(any(), match { it.id == 1L && it.mountPath == "/local" }) }
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.storage.StorageEditViewModelTest`
Expected: FAIL

- [ ] **Step 2: 实现 StorageEditViewModel**

`StorageEditViewModel.kt`:
```kotlin
package com.textvision.alistclient.admin.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.network.dto.DriverInfo
import com.textvision.alistclient.network.dto.StorageInfo
import com.textvision.alistclient.network.dto.StoragePatch
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StorageEditUiState {
    data object Loading : StorageEditUiState
    data class Form(
        val storage: StorageInfo,
        val driver: DriverInfo?,
        val formItems: List<FormItem>,
        val fieldValues: Map<String, Any?>,
        val enabled: Boolean,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val saved: Boolean = false,
    ) : StorageEditUiState
    data class Error(val message: String) : StorageEditUiState
}

@HiltViewModel
class StorageEditViewModel @Inject constructor(
    @Suppress("unused") private val authRepository: AuthRepository,
    @Suppress("unused") private val transferManager: TransferManager,
    @Suppress("unused") private val previewFileStore: PreviewFileStore,
    private val storageRepository: StorageRepositoryContract,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StorageEditUiState>(StorageEditUiState.Loading)
    val uiState: StateFlow<StorageEditUiState> = _uiState.asStateFlow()

    fun load(id: Long) {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.value = StorageEditUiState.Loading
            val listR = storageRepository.list(base)
            val s = (listR as? AdminResult.Ok)?.data?.content?.firstOrNull { it.id == id }
            if (s == null) {
                _uiState.value = StorageEditUiState.Error("找不到存储 #$id")
                return@launch
            }
            val driversR = storageRepository.listDrivers(base)
            val driver = (driversR as? AdminResult.Ok)?.data?.firstOrNull { it.name == s.driver }
            val formItems = driver?.configItems?.map { FormItem.fromConfigItem(it) } ?: emptyList()
            val fieldValues = parseAddition(s.addition)
            _uiState.value = StorageEditUiState.Form(
                storage = s,
                driver = driver,
                formItems = formItems,
                fieldValues = fieldValues,
                enabled = s.status != "disabled",
            )
        }
    }

    fun updateField(name: String, value: Any?) {
        _uiState.update { state ->
            if (state is StorageEditUiState.Form) {
                state.copy(fieldValues = state.fieldValues + (name to value))
            } else state
        }
    }

    fun setEnabled(enabled: Boolean) {
        _uiState.update { state -> if (state is StorageEditUiState.Form) state.copy(enabled = enabled) else state }
    }

    fun save() {
        val state = _uiState.value as? StorageEditUiState.Form ?: return
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val addition = serializeAddition(state.fieldValues)
            val patch = StoragePatch(
                id = state.storage.id ?: 0L,
                mountPath = state.storage.mountPath,
                driver = state.storage.driver,
                enabled = state.enabled,
                addition = addition,
            )
            when (val r = storageRepository.update(base, patch)) {
                is AdminResult.Ok -> _uiState.value = state.copy(isSaving = false, saved = true)
                else -> _uiState.value = state.copy(isSaving = false, errorMessage = "保存失败")
            }
        }
    }

    private fun parseAddition(raw: String?): Map<String, Any?> {
        if (raw.isNullOrBlank()) return emptyMap()
        return try {
            val json = kotlinx.serialization.json.Json.parseToJsonElement(raw).jsonObject
            json.mapValues { (_, v) ->
                when (v) {
                    is kotlinx.serialization.json.JsonPrimitive -> v.content
                    else -> v.toString()
                }
            }
        } catch (t: Throwable) {
            emptyMap()
        }
    }

    private fun serializeAddition(values: Map<String, Any?>): String {
        val obj = kotlinx.serialization.json.JsonObject(
            values.mapValues { (_, v) -> kotlinx.serialization.json.JsonPrimitive(v?.toString() ?: "") }
        )
        return obj.toString()
    }
}

private val kotlinx.serialization.json.JsonElement.jsonObject: kotlinx.serialization.json.JsonObject
    get() = this as kotlinx.serialization.json.JsonObject
```

- [ ] **Step 3: 实现 StorageEditScreen**

`StorageEditScreen.kt`:
```kotlin
package com.textvision.alistclient.admin.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.form.DynamicFormField
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar
import com.textvision.alistclient.ui.components.CloudBannerKind

@Composable
fun StorageEditScreen(
    storageId: Long,
    onBack: () -> Unit,
    viewModel: StorageEditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(storageId) { viewModel.load(storageId) }
    LaunchedEffect((state as? StorageEditUiState.Form)?.saved) {
        if ((state as? StorageEditUiState.Form)?.saved == true) onBack()
    }

    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(
            title = "编辑存储",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            },
        )
        when (val s = state) {
            is StorageEditUiState.Loading -> {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is StorageEditUiState.Error -> {
                CloudStatusBanner(text = s.message, kind = CloudBannerKind.Error)
            }
            is StorageEditUiState.Form -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    CloudCard {
                        Text("挂载路径：${s.storage.mountPath}")
                        Text("驱动：${s.storage.driver}")
                    }
                    Spacer(Modifier.height(10.dp))
                    if (s.formItems.isNotEmpty()) {
                        CloudCard {
                            Text("驱动参数")
                            s.formItems.forEach { item ->
                                DynamicFormField(
                                    item = item,
                                    value = s.fieldValues[item.name],
                                    onValueChange = { viewModel.updateField(item.name, it) },
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    CloudCard {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("启用存储")
                            Switch(
                                checked = s.enabled,
                                onCheckedChange = { viewModel.setEnabled(it) },
                            )
                        }
                    }
                    s.errorMessage?.let {
                        Spacer(Modifier.height(10.dp))
                        CloudStatusBanner(text = it, kind = CloudBannerKind.Error)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !s.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (s.isSaving) "保存中…" else "保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun Column(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(modifier, content = content)
}
```

- [ ] **Step 4: 在 AppNavHost 接入路由**

`AppNavHost.kt` 的 `composable` 块追加（imports 同步加）：
```kotlin
import com.textvision.alistclient.admin.storage.StorageEditScreen
import com.textvision.alistclient.ui.screens.SettingsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

composable(
    route = AppRoute.StorageEdit.route,
    arguments = listOf(navArgument("id") { type = NavType.LongType }),
) { entry ->
    val id = entry.arguments?.getLong("id") ?: 0L
    StorageEditScreen(
        storageId = id,
        onBack = { navController.popBackStack() },
    )
}
```

并把现有 `composable(AppRoute.Settings.route)` 块的 `SettingsScreen(...)` 调用加上 `onStorageClick` 和 `onAdvancedSettings` 回调（Task 9 完成时这些回调会接 `AdminSiteSettingsScreen`）：

```kotlin
composable(AppRoute.Settings.route) {
    SettingsScreen(
        onLoggedOut = {
            navController.navigate(AppRoute.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        },
        onStorageClick = { id ->
            navController.navigate(AppRoute.StorageEdit.create(id))
        },
        onAdvancedSettings = {
            navController.navigate(AppRoute.AdminSiteSettings.route)
        },
    )
}
```

- [ ] **Step 5: 编译 + 测试验证**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest --tests com.textvision.alistclient.admin.storage.StorageEditViewModelTest`
Expected: BUILD SUCCESSFUL + PASS

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/admin/storage/StorageEditViewModel.kt \
        app/src/main/java/com/textvision/alistclient/admin/storage/StorageEditScreen.kt \
        app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt \
        app/src/test/java/com/textvision/alistclient/admin/storage/StorageEditViewModelTest.kt
git commit -m "feat(admin): add StorageEditScreen with dynamic form"
```

---

### Task 9: AdminSiteSettingsScreen + ViewModel + 路由接入

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/admin/settings/AdminSiteSettingsViewModel.kt`
- Create: `app/src/main/java/com/textvision/alistclient/admin/settings/AdminSiteSettingsScreen.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt`
- Test: `app/src/test/java/com/textvision/alistclient/admin/settings/AdminSiteSettingsViewModelTest.kt`

**Interfaces:**
- Produces:
  - `AdminSiteSettingsViewModel.uiState: StateFlow<AdminSiteSettingsUiState>`
  - `AdminSiteSettingsViewModel.load()`, `updateField()`, `save()`

- [ ] **Step 1: 写失败测试 — load + save**

`AdminSiteSettingsViewModelTest.kt`:
```kotlin
package com.textvision.alistclient.admin.settings

import app.cash.turbine.test
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.network.dto.ConfigItem
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminSiteSettingsViewModelTest {
    private val auth: AuthRepository = mockk(relaxed = true)
    private val transfer: TransferManager = mockk(relaxed = true)
    private val preview: PreviewFileStore = mockk(relaxed = true)
    private val settings: SettingsRepositoryContract = mockk(relaxed = true)
    private val session: SessionManager = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun loadPopulatesGroups() = runTest {
        coEvery { session.loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://x/", username = "u", password = "p", token = "t"
        )
        coEvery { settings.list(any()) } returns AdminResult.Ok(listOf(
            SettingGroup("site", listOf(SettingItem(
                key = "site_title", value = "My Alist", group = "site",
                formItems = listOf(ConfigItem(name = "site_title", label = "标题", type = "string"))
            )))
        ))
        val vm = AdminSiteSettingsViewModel(auth, transfer, preview, settings, session)
        vm.load()
        vm.uiState.test {
            var s = awaitItem()
            assertTrue(s is AdminSiteSettingsUiState.Form)
            s = s as AdminSiteSettingsUiState.Form
            assertEquals(1, s.groups.size)
            assertEquals("site", s.groups[0].key)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test fun saveSendsAllPatches() = runTest {
        coEvery { session.loadSavedSession() } returns com.textvision.alistclient.auth.model.SavedSession(
            serverUrl = "http://x/", username = "u", password = "p", token = "t"
        )
        coEvery { settings.list(any()) } returns AdminResult.Ok(listOf(
            SettingGroup("site", listOf(SettingItem(
                key = "site_title", value = "old", group = "site",
                formItems = listOf(ConfigItem(name = "site_title", label = "标题", type = "string"))
            )))
        ))
        coEvery { settings.save(any(), any()) } returns AdminResult.Ok(Unit)
        val vm = AdminSiteSettingsViewModel(auth, transfer, preview, settings, session)
        vm.load()
        vm.updateField("site_title", "new")
        vm.save()
        coVerify { settings.save(any(), match { patches -> patches.any { it.first == "site_title" && it.second == "new" } }) }
    }
}
```

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.admin.settings.AdminSiteSettingsViewModelTest`
Expected: FAIL

- [ ] **Step 2: 实现 ViewModel**

`AdminSiteSettingsViewModel.kt`:
```kotlin
package com.textvision.alistclient.admin.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.textvision.alistclient.admin.AdminResult
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.preview.PreviewFileStore
import com.textvision.alistclient.transfer.TransferManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AdminSiteSettingsUiState {
    data object Loading : AdminSiteSettingsUiState
    data class Form(
        val groups: List<SettingGroup>,
        val fieldValues: Map<String, String?>,
        val isSaving: Boolean = false,
        val errorMessage: String? = null,
        val saved: Boolean = false,
    ) : AdminSiteSettingsUiState
    data class Error(val message: String) : AdminSiteSettingsUiState
}

@HiltViewModel
class AdminSiteSettingsViewModel @Inject constructor(
    @Suppress("unused") private val authRepository: AuthRepository,
    @Suppress("unused") private val transferManager: TransferManager,
    @Suppress("unused") private val previewFileStore: PreviewFileStore,
    private val settingsRepository: SettingsRepositoryContract,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<AdminSiteSettingsUiState>(AdminSiteSettingsUiState.Loading)
    val uiState: StateFlow<AdminSiteSettingsUiState> = _uiState.asStateFlow()

    fun load() {
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.value = AdminSiteSettingsUiState.Loading
            when (val r = settingsRepository.list(base)) {
                is AdminResult.Ok -> {
                    val values = r.data.flatMap { g -> g.items }.associate { it.key to it.value }
                    _uiState.value = AdminSiteSettingsUiState.Form(groups = r.data, fieldValues = values)
                }
                else -> _uiState.value = AdminSiteSettingsUiState.Error("加载失败")
            }
        }
    }

    fun updateField(key: String, value: String?) {
        _uiState.update { state ->
            if (state is AdminSiteSettingsUiState.Form) {
                state.copy(fieldValues = state.fieldValues + (key to value))
            } else state
        }
    }

    fun save() {
        val state = _uiState.value as? AdminSiteSettingsUiState.Form ?: return
        val base = sessionManager.loadSavedSession()?.serverUrl ?: return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)
            val patches = state.fieldValues
                .filterValues { it != null }
                .map { (k, v) -> k to (v ?: "") }
            when (val r = settingsRepository.save(base, patches)) {
                is AdminResult.Ok -> _uiState.value = state.copy(isSaving = false, saved = true)
                else -> _uiState.value = state.copy(isSaving = false, errorMessage = "保存失败")
            }
        }
    }
}
```

- [ ] **Step 3: 实现 Screen**

`AdminSiteSettingsScreen.kt`:
```kotlin
package com.textvision.alistclient.admin.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.textvision.alistclient.admin.form.DynamicFormField
import com.textvision.alistclient.admin.form.FormItem
import com.textvision.alistclient.network.dto.SettingItem
import com.textvision.alistclient.ui.components.CloudBannerKind
import com.textvision.alistclient.ui.components.CloudCard
import com.textvision.alistclient.ui.components.CloudScaffold
import com.textvision.alistclient.ui.components.CloudStatusBanner
import com.textvision.alistclient.ui.components.CloudTopBar

@Composable
fun AdminSiteSettingsScreen(
    onBack: () -> Unit,
    viewModel: AdminSiteSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect((state as? AdminSiteSettingsUiState.Form)?.saved) {
        if ((state as? AdminSiteSettingsUiState.Form)?.saved == true) onBack()
    }

    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(
            title = "完整设置",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            },
        )
        when (val s = state) {
            is AdminSiteSettingsUiState.Loading -> {
                Spacer(Modifier.height(40.dp))
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is AdminSiteSettingsUiState.Error -> {
                CloudStatusBanner(text = s.message, kind = CloudBannerKind.Error)
            }
            is AdminSiteSettingsUiState.Form -> {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    s.groups.forEach { group ->
                        CloudCard {
                            Text("分组：${group.key}")
                            group.items.forEach { item ->
                                RenderSettingItem(
                                    item = item,
                                    value = s.fieldValues[item.key],
                                    onChange = { viewModel.updateField(item.key, it) },
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    s.errorMessage?.let {
                        CloudStatusBanner(text = it, kind = CloudBannerKind.Error)
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.save() },
                        enabled = !s.isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (s.isSaving) "保存中…" else "保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderSettingItem(
    item: SettingItem,
    value: String?,
    onChange: (String?) -> Unit,
) {
    val formItem = item.formItems?.map { FormItem.fromConfigItem(it) }?.firstOrNull()
    if (formItem != null) {
        DynamicFormField(
            item = formItem,
            value = value,
            onValueChange = { v -> onChange(v?.toString()) },
        )
    }
}
```

- [ ] **Step 4: AppNavHost 接入 AdminSiteSettings 路由**

`AppNavHost.kt` 的 `composable` 块追加：
```kotlin
import com.textvision.alistclient.admin.settings.AdminSiteSettingsScreen

composable(AppRoute.AdminSiteSettings.route) {
    AdminSiteSettingsScreen(
        onBack = { navController.popBackStack() },
    )
}
```

- [ ] **Step 5: 编译 + 测试验证**

Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest --tests com.textvision.alistclient.admin.settings.AdminSiteSettingsViewModelTest`
Expected: BUILD SUCCESSFUL + PASS

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/admin/settings/AdminSiteSettingsViewModel.kt \
        app/src/main/java/com/textvision/alistclient/admin/settings/AdminSiteSettingsScreen.kt \
        app/src/main/java/com/textvision/alistclient/navigation/AppNavHost.kt \
        app/src/test/java/com/textvision/alistclient/admin/settings/AdminSiteSettingsViewModelTest.kt
git commit -m "feat(admin): add AdminSiteSettingsScreen with grouped dynamic form"
```

---

### Task 10: 全量验证 + 影响分析

**Files:** 无

**Steps:**

- [ ] **Step 1: 跑全量单元测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: ALL PASS

- [ ] **Step 2: 跑 lint**

Run: `./gradlew :app:lintDebug`
Expected: 无新增错误

- [ ] **Step 3: 跑 build**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: GitNexus 影响分析**

通过 mcp 调用 `mcp__gitnexus__impact({target: "AdminRepository", direction: "upstream"})` 和 `mcp__gitnexus__detect_changes({scope: "compare", base_ref: "main"})`，确认只影响预期范围。

- [ ] **Step 5: 端到端验收（按 spec "端到端验证" 清单人工执行）**

- 登录管理员账号 → 设置页应可见存储列表和 4 个常用设置项
- 启/停单个存储 → 列表 Switch 状态变化，二次进入仍正确
- 进入编辑页 → 修改 driver 字段 → 保存 → 回到列表正常显示
- 站点标题修改 → 保存 → 重新登录 → 看到新标题
- 故意断网 → 启/停应失败回滚、保存按钮给错误提示

- [ ] **Step 6: 提交（如有 lint 修复）**

```bash
git add -A
git commit -m "chore(admin): post-implementation cleanup"
```

---

## Self-Review

- ✅ 1. **Spec 覆盖**：
  - 存储管理（看/改、不增/不删）→ Task 4 + Task 6（行内启/停）+ Task 8（二级编辑）
  - 站点设置集成（常用 4 项 + 完整）→ Task 5 + Task 7（卡片 5 行内编辑）+ Task 9（完整设置）
  - AdminRepository 共享 → Task 1
  - 动态 FormItem + DynamicFormField → Task 2 + Task 3
  - 4 个 setting key 列出 → Task 5 (代码) + Task 6 (filter logic) + Task 7 (displayLabel)
  - 全量提交策略 → Task 4 (update 整 patch)
  - 风险 2（Map/List）→ Task 5 (listSettings 实现按 List + 注释说明可改)
  - 风险 3（未知字段类型降级为 Text）→ Task 2 (FormItem.fromConfigItem else 分支)
  - 风险 5（isSubmitting 守卫）→ Task 8 + Task 9 (isSaving flag)

- ✅ 2. **占位符扫描**：所有 step 含完整代码；无 TBD/TODO/实现待办。

- ✅ 3. **类型一致性**：
  - `AdminResult` sealed interface 在 Task 1 定义，后续 Task 4/5 复用
  - `StoragePatch` 字段在 Task 2 定义，Task 4/6 构造时一致
  - `SettingGroup(key, items)` 在 Task 5 定义，Task 9 复用
  - `FormItem` sealed class 在 Task 2 定义，Task 3 渲染全部 7 个子类
  - `StorageEditUiState` / `AdminSiteSettingsUiState` sealed interface 在各自 ViewModel 任务定义
  - `runAdmin` 签名 Task 1 锁定，Task 4/5 调用一致

- ✅ 4. **范围聚焦**：单一 feature，10 个任务串行，每步可独立测试。

- ✅ 5. **风险与未决项处理**：
  - setting/list List vs Map：Task 5 按 List 实现，注释说明如 Map 需 Adapter
  - 全量 vs partial：Task 4 用全量 patch
  - 未知字段类型：Task 2/3 降级为 Text
  - 4 个 key 实际名称差异：Task 6 `QUICK_KEYS` 常量可改

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-07-07-admin-storage-settings-plan.md`. Two execution options:

1. **Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. **Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
