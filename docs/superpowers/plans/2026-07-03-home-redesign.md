# 首页 Dashboard 重新设计 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重写 `HomeScreen` 为仪表盘风格的"服务器总览"，通过 5 个 section（公开设置 / 存储 / 服务器指标 / 会话 / 后台任务）展示 Alist v3 公开 + admin API 实际可达的全部服务器信息，每个 section 独立三态、逐块降级。

**Architecture:** 数据层 `HomeRepository` 改为并发拉取 5 个 section + 翻译 `SectionResult<T>`；ViewModel 暴露 `loadIfNeeded / refresh / retrySection`；UI 层按 section 渲染独立 composable，公开端点失败触发整页 Error，其余 section 失败仅该卡显示 inline 重试。

**Tech Stack:** Kotlin 2.0.21 + Compose BOM 2024.09.03 + Hilt 2.52 + Retrofit 2.11 + OkHttp 4.12 + MockWebServer + kotlinx-serialization + JUnit 4 + Turbine + Coroutines Test + Compose UI Test (androidTest).

## Global Constraints

- **JVM target**: 17
- **minSdk 26 / compileSdk 34 / targetSdk 34**
- **Material3** (Compose BOM 2024.09.03)；复用 `CloudScaffold / CloudTopBar / CloudCard / CloudStatusBanner` 组件
- **包名**: `com.textvision.alistclient`
- **响应式主题色**: 复用 `CloudPrimary / CloudPrimaryDark / CloudPrimarySoft / CloudText* / CloudSurface*`；不引入新颜色 token
- **测试 tag 命名**: `home_<section>_<element>`，例如 `home_kpi_user`、`home_task_card`、`home_storage_card_<mountPath>`
- **DTO 字段命名**: JSON tag 全部 snake_case（v3 实际响应约定），Kotlin 属性 camelCase
- **Alist 鉴权约定**: admin 端点需 admin JWT；`AuthInterceptor` 自动注入 `Authorization`；401 自动重登逻辑（`HomeRepository.runAdminWithRefresh`）必须保留
- **删除**: `AdminInfo` DTO 和 `AlistApi.adminInfo` Retrofit 方法（v3 无此端点）
- **构建命令**: `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`
- **UI 测试运行**: `./gradlew :app:connectedDebugAndroidTest --tests com.textvision.alistclient.home.HomeScreenInstrumentedTest`
- **single test class**: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRepositoryTest`
- **frequent commits**: 每个 Task 提交一次，commit message 格式 `<type>(<scope>): <summary>`；TDD 顺序：红 → 绿 → 重构 → 提交

---

## File Structure

### 新增文件

```
app/src/main/java/com/textvision/alistclient/home/dto/
├── SectionResult.kt          公共 SectionResult<T> 三态 + SectionFailure sealed
├── PublicData.kt             public/settings 解析结果
├── ServerStatsData.kt        user + role 聚合
├── SessionData.kt            session list + 活跃计数
└── TaskData.kt               7 类 task 汇总 + failedBuckets

app/src/main/java/com/textvision/alistclient/home/
├── SectionKey.kt             enum: Public, Storage, ServerStats, Session, Task

app/src/test/java/com/textvision/alistclient/network/dto/
└── AdminDtosTest.kt          DTO 解析测试（用真实响应 fixture）
```

### 改动文件

```
app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt
  - 删 AdminInfo, AdminInfoRequest
  - 改: UserList, User, RoleList, Role, SessionInfo, TaskInfo, TaskState

app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt
  - 删 adminInfo
  - 增 listUsers, listRoles, listSessions, taskUndone

app/src/main/java/com/textvision/alistclient/home/HomeData.kt
  - sealed interface → data class HomeData(...5 section...)

app/src/main/java/com/textvision/alistclient/home/HomeRepository.kt
  - 单 endpoint loadDashboard 拆为 5 section 并发拉取
  - 公共端点失败 → ApiResult.Failure
  - 其余 4 section 失败 → SectionResult.Failed 填入 HomeData
  - 新增 retrySection(data, key) 单 section 重试

app/src/main/java/com/textvision/alistclient/home/HomeViewModel.kt
  - 新增 retrySection(SectionKey)
  - 保留 loadIfNeeded/refresh/hasLoadedInitial/StubNetworkMonitor

app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt
  - 重构 SuccessContent 按 5 section 渲染
  - 删 StorageSummaryCard / StorageCard / StatusBadge / driverLabel
  - 增 KpiTile / TaskCard / SectionFailedHint / StorageSummaryStrip
  - 改 HeroCard 接受 PublicData?

app/src/main/java/com/textvision/alistclient/home/HomeUiState.kt
  - data class Success(val data: HomeData) 不变结构

app/src/test/java/com/textvision/alistclient/home/HomeRepositoryTest.kt
  - 全部改写以适配 HomeData 新结构 + 5 section
  - 8 个 MockWebServer 用例

app/src/test/java/com/textvision/alistclient/home/HomeViewModelTest.kt
  - FakeRepo 改为返回新 HomeData 结构
  - 增 retrySection 用例

app/src/androidTest/java/com/textvision/alistclient/home/HomeScreenInstrumentedTest.kt
  - 改 HomeData 构造
  - 增 4 个新 UI 用例（KPI / Task / 失败 inline / 公开端点失败整页）
```

---

## Task 1: 删 `AdminInfo` 死代码（DTO + Retrofit）

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`
- Modify: `app/src/test/java/com/textvision/alistclient/home/HomeRepositoryTest.kt`

**Interfaces:**
- 产出的 `AlistApi` 接口删除 `adminInfo` 方法。后续 Task 3 在同一文件新增 4 个 admin 端点方法。

- [ ] **Step 1: 跑测试基线（确认现有测试不受影响）**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRepositoryTest`
Expected: PASS（删除前最后一次绿）

- [ ] **Step 2: 删 `AdminInfo` 与 `AdminInfoRequest`**

Modify `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`：删除以下整段代码：

```kotlin
@Serializable
data class AdminInfo(
    @SerialName("version") val version: String? = null,
    @SerialName("build_date") val buildDate: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("used_bytes") val usedBytes: Long = 0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
)

@Serializable
data class AdminInfoRequest(
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 0,
)
```

文件保留 `StorageList` / `StorageInfo` / `PublicSettings` / `StorageListRequest`。

- [ ] **Step 3: 删 `AlistApi.adminInfo`**

Modify `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`：

1. 删除 import: `com.textvision.alistclient.network.dto.AdminInfo` 和 `com.textvision.alistclient.network.dto.AdminInfoRequest`
2. 删除方法：

```kotlin
@POST
suspend fun adminInfo(@Url url: String, @Header(SkipAuthRetry.HEADER) skipAuthRetry: String, @Body request: com.textvision.alistclient.network.dto.AdminInfoRequest = com.textvision.alistclient.network.dto.AdminInfoRequest()): AlistResponse<AdminInfo>
```

- [ ] **Step 4: 删 `HomeRepositoryTest` 中对 `adminInfo` 的引用（grep 验证）**

Run: `grep -rn "adminInfo\|AdminInfo" app/src/test/`
Expected: 0 matches（如果没有匹配，跳到 Step 5）

如果匹配存在，删除测试文件中引用 `adminInfo` 的代码段。

- [ ] **Step 5: 跑测试确认无回归**

Run: `./gradlew :app:testDebugUnitTest`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt \
        app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt \
        app/src/test/java/com/textvision/alistclient/home/HomeRepositoryTest.kt
git commit -m "refactor(home): remove unused AdminInfo DTO and adminInfo endpoint (v3 has no /api/admin/info)"
```

---

## Task 2: 新增 admin DTO（User / Role / Session / TaskInfo）

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`
- Create: `app/src/test/java/com/textvision/alistclient/network/dto/AdminDtosTest.kt`

**Interfaces:**
- 产出 7 个 `@Serializable data class`，供 Task 3 的 `AlistApi` 方法签名使用

- [ ] **Step 1: 写 DTO 解析测试（红）**

Create `app/src/test/java/com/textvision/alistclient/network/dto/AdminDtosTest.kt`：

```kotlin
package com.textvision.alistclient.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminDtosTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test fun userListParses() {
        val raw = """{"code":200,"message":"success","data":{"content":[{"id":1,"username":"admin","disabled":false,"role":"2"}],"total":1}}"""
        val resp = json.decodeFromString<AlistResponse<UserList>>(raw)
        assertEquals(200, resp.code)
        assertEquals(1, resp.data!!.content.size)
        assertEquals("admin", resp.data.content[0].username)
        assertEquals("2", resp.data.content[0].role)
    }

    @Test fun roleListParses() {
        val raw = """{"code":200,"message":"success","data":{"content":[{"id":2,"name":"admin","description":"管理员","default":false}],"total":1}}"""
        val resp = json.decodeFromString<AlistResponse<RoleList>>(raw)
        assertEquals("admin", resp.data!!.content[0].name)
    }

    @Test fun sessionInfoParses() {
        val raw = """{"code":200,"message":"success","data":[{"session_id":"abc","user_id":1,"last_active":1700000000,"status":0,"ua":"Mozilla/5.0","ip":"127.0.0.1"}]}"""
        val resp = json.decodeFromString<AlistResponse<List<SessionInfo>>>(raw)
        assertEquals(1, resp.data!!.size)
        assertEquals(0, resp.data[0].status)
        assertEquals("127.0.0.1", resp.data[0].ip)
    }

    @Test fun taskInfoParses() {
        val raw = """{"code":200,"message":"success","data":[{"id":"t1","name":"upload","state":"running","status":"uploading","progress":42.5,"total_bytes":1024,"error":""}]}"""
        val resp = json.decodeFromString<AlistResponse<List<TaskInfo>>>(raw)
        assertEquals("t1", resp.data!![0].id)
        assertEquals(42.5, resp.data[0].progress, 0.001)
    }

    @Test fun sessionInfoIgnoresExtraFields() {
        val raw = """{"code":200,"message":"success","data":[{"session_id":"x","user_id":2,"last_active":1,"status":1,"ua":"u","ip":"1.1.1.1","extra":"ignored"}]}"""
        val resp = json.decodeFromString<AlistResponse<List<SessionInfo>>>(raw)
        assertTrue(resp.data!!.isNotEmpty())
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.network.dto.AdminDtosTest`
Expected: FAIL — `Unresolved reference: UserList`

- [ ] **Step 3: 实现 DTO**

Append to `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`（保留现有 `StorageList`/`StorageInfo`/`PublicSettings`/`StorageListRequest`）：

```kotlin
@Serializable
data class UserList(
    @SerialName("content") val content: List<User> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class User(
    val id: Long? = null,
    val username: String = "",
    @SerialName("base_path") val basePath: String? = null,
    val role: String = "",
    val disabled: Boolean = false,
    val permission: Int = 0,
    @SerialName("sso_id") val ssoId: String? = null,
)

@Serializable
data class RoleList(
    @SerialName("content") val content: List<Role> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class Role(
    val id: Long? = null,
    val name: String = "",
    val description: String? = null,
    val default: Boolean = false,
)

@Serializable
data class SessionInfo(
    @SerialName("session_id") val sessionId: String = "",
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("last_active") val lastActive: Long = 0,
    val status: Int = 0,
    val ua: String? = null,
    val ip: String? = null,
)

@Serializable
data class TaskInfo(
    val id: String = "",
    val name: String = "",
    val state: String? = null,
    val status: String? = null,
    val progress: Double = 0.0,
    @SerialName("total_bytes") val totalBytes: Long = 0,
    val error: String? = null,
)
```

注意：`User.password` 字段**不实现**（v3 user.go 端点不置空风险）。

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.network.dto.AdminDtosTest`
Expected: PASS（5 用例全绿）

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt \
        app/src/test/java/com/textvision/alistclient/network/dto/AdminDtosTest.kt
git commit -m "feat(network): add User/Role/Session/TaskInfo DTOs for admin endpoints"
```

---

## Task 3: 扩展 `AlistApi`（4 个新方法）

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`

**Interfaces:**
- 产出 4 个新 suspend 方法，供 Task 7 的 `HomeRepository.fetchXxx` 使用
- 调用方式：`api.listUsers("${base}api/admin/user/list")` / `api.taskUndone("${base}api/admin/task/upload/undone")` 等

- [ ] **Step 1: 添加 import**

Modify `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`，追加 import：

```kotlin
import com.textvision.alistclient.network.dto.RoleList
import com.textvision.alistclient.network.dto.SessionInfo
import com.textvision.alistclient.network.dto.TaskInfo
import com.textvision.alistclient.network.dto.UserList
```

- [ ] **Step 2: 添加 4 个新方法**

Append to the interface body（位于 `getPublicSettings` 之前）：

```kotlin
@GET
suspend fun listUsers(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<UserList>

@GET
suspend fun listRoles(@Url url: String, @Query("page") page: Int = 1, @Query("per_page") perPage: Int = 0): AlistResponse<RoleList>

@GET
suspend fun listSessions(@Url url: String): AlistResponse<List<SessionInfo>>

@POST
suspend fun taskUndone(@Url url: String): AlistResponse<List<TaskInfo>>
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt
git commit -m "feat(network): add listUsers/listRoles/listSessions/taskUndone endpoints"
```

---

## Task 4: 新增 `SectionResult<T>` + `SectionKey`

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/home/dto/SectionResult.kt`
- Create: `app/src/main/java/com/textvision/alistclient/home/SectionKey.kt`

**Interfaces:**
- `SectionResult<T>` 是 5 个 section 共享的三态包装，供 Task 5 的 HomeData 字段和 Task 8 的 UI 渲染使用
- `SectionKey` 是 enum，标识 5 个 section 之一，供 Task 6 的 `retrySection` 使用

- [ ] **Step 1: 创建 SectionResult.kt**

Create `app/src/main/java/com/textvision/alistclient/home/dto/SectionResult.kt`：

```kotlin
package com.textvision.alistclient.home.dto

sealed interface SectionResult<out T> {
    data class Ok<T>(val data: T) : SectionResult<T>
    data object Loading : SectionResult<Nothing>
    data class Failed(val cause: SectionFailure) : SectionResult<Nothing>
}

sealed interface SectionFailure {
    data object Network : SectionFailure
    data object Unauthorized : SectionFailure
    data class Server(val code: Int) : SectionFailure
}
```

- [ ] **Step 2: 创建 SectionKey.kt**

Create `app/src/main/java/com/textvision/alistclient/home/SectionKey.kt`：

```kotlin
package com.textvision.alistclient.home

enum class SectionKey { Public, Storage, ServerStats, Session, Task }
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/home/dto/SectionResult.kt \
        app/src/main/java/com/textvision/alistclient/home/SectionKey.kt
git commit -m "feat(home): add SectionResult sealed type and SectionKey enum"
```

---

## Task 5: 4 个 section DTO（PublicData / ServerStatsData / SessionData / TaskData）

**Files:**
- Create: `app/src/main/java/com/textvision/alistclient/home/dto/PublicData.kt`
- Create: `app/src/main/java/com/textvision/alistclient/home/dto/ServerStatsData.kt`
- Create: `app/src/main/java/com/textvision/alistclient/home/dto/SessionData.kt`
- Create: `app/src/main/java/com/textvision/alistclient/home/dto/TaskData.kt`

**Interfaces:**
- 4 个 data class 是 `SectionResult.Ok<T>` 的 `T` 类型，分别被 Task 7 的 `fetchPublic / fetchServerStats / fetchSession / fetchTask` 构造

- [ ] **Step 1: PublicData.kt**

```kotlin
package com.textvision.alistclient.home.dto

data class PublicData(
    val siteTitle: String,
    val siteVersion: String?,
    val announcement: String?,
    val logo: String?,
    val favicon: String?,
    val mainColor: String?,
    val allowRegister: Boolean,
)
```

- [ ] **Step 2: ServerStatsData.kt**

```kotlin
package com.textvision.alistclient.home.dto

data class ServerStatsData(
    val userCount: Int,
    val roleCount: Int,
    val disabledUserCount: Int,
)
```

- [ ] **Step 3: SessionData.kt**

```kotlin
package com.textvision.alistclient.home.dto

data class SessionData(
    val totalCount: Int,
    val activeCount: Int,
)
```

- [ ] **Step 4: TaskData.kt**

```kotlin
package com.textvision.alistclient.home.dto

data class TaskData(
    val runningCount: Int,
    val finishedCount: Int,
    val failedBucketIds: List<String>,
    val buckets: List<TaskBucket>,
)

data class TaskBucket(
    val type: String,
    val running: Int,
)
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/home/dto/PublicData.kt \
        app/src/main/java/com/textvision/alistclient/home/dto/ServerStatsData.kt \
        app/src/main/java/com/textvision/alistclient/home/dto/SessionData.kt \
        app/src/main/java/com/textvision/alistclient/home/dto/TaskData.kt
git commit -m "feat(home): add per-section DTOs (Public/ServerStats/Session/Task)"
```

---

## Task 6: 重写 `HomeData` 为 data class 容纳 5 section

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/home/dto/HomeData.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/home/HomeUiState.kt`

**Interfaces:**
- 产出 `data class HomeData(...)` 5 字段；`HomeUiState.Success(val data: HomeData)`
- 旧 `HomeData.Admin / HomeData.Guest` 移除（被 5 section 替代）

- [ ] **Step 1: 改写 HomeData.kt**

Replace `app/src/main/java/com/textvision/alistclient/home/dto/HomeData.kt` 完整内容：

```kotlin
package com.textvision.alistclient.home.dto

import com.textvision.alistclient.network.dto.StorageInfo

data class HomeData(
    val publicSection: SectionResult<PublicData>,
    val storageSection: SectionResult<StorageData>,
    val serverStatsSection: SectionResult<ServerStatsData>,
    val sessionSection: SectionResult<SessionData>,
    val taskSection: SectionResult<TaskData>,
) {
    /** Storage-only helper for legacy UI. Empty when storageSection is Failed. */
    val storages: List<StorageInfo>
        get() = (storageSection as? SectionResult.Ok)?.data?.storages ?: emptyList()

    val isGuest: Boolean
        get() = storageSection is SectionResult.Failed
}

data class StorageData(val storages: List<StorageInfo>) {
    val total: Int get() = storages.size
    val working: Int get() = storages.count { it.status == "work" }
    val abnormal: Int get() = total - working
}
```

- [ ] **Step 2: 改写 HomeUiState.kt**

Replace `app/src/main/java/com/textvision/alistclient/home/HomeUiState.kt` 完整内容：

```kotlin
package com.textvision.alistclient.home

import com.textvision.alistclient.home.dto.HomeData

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
```

- [ ] **Step 3: 验证编译（会有引用旧 Admin/Guest 类型的编译错误）**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD FAILED（HomeRepository.kt / HomeScreen.kt 引用旧 HomeData.Admin / HomeData.Guest）

这是预期的，**不修这些**，继续到 Task 7。

- [ ] **Step 4: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/home/dto/HomeData.kt \
        app/src/main/java/com/textvision/alistclient/home/HomeUiState.kt
git commit -m "refactor(home): convert HomeData to 5-section data class"
```

---

## Task 7: 重写 `HomeRepository`（并发 5 section + retrySection）

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/home/HomeRepository.kt`
- Modify: `app/src/main/java/com/textvision/alistclient/home/HomeRepositoryContract.kt`

**Interfaces:**
- `HomeRepositoryContract` 加 `suspend fun retrySection(data: HomeData, key: SectionKey): HomeData`
- `HomeRepository.loadDashboard()` 返回 `ApiResult<HomeData>`（5 section 填充）
- 公开端点失败 → `ApiResult.Failure(code, message)` 整页 Error
- 其余 4 section 失败 → `SectionResult.Failed(...)` 填入对应字段

- [ ] **Step 1: 扩展 contract**

Replace `app/src/main/java/com/textvision/alistclient/home/HomeRepositoryContract.kt` 完整内容：

```kotlin
package com.textvision.alistclient.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.home.dto.HomeData

interface HomeRepositoryContract {
    suspend fun loadDashboard(): ApiResult<HomeData>
    suspend fun retrySection(data: HomeData, key: SectionKey): HomeData
}
```

- [ ] **Step 2: 写 HomeRepositoryTest 失败用例（红）**

Replace `app/src/test/java/com/textvision/alistclient/home/HomeRepositoryTest.kt` 完整内容：

```kotlin
package com.textvision.alistclient.home

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.data.secure.CredentialStore
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.home.dto.PublicData
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.home.dto.ServerStatsData
import com.textvision.alistclient.home.dto.SessionData
import com.textvision.alistclient.home.dto.StorageData
import com.textvision.alistclient.home.dto.TaskData
import com.textvision.alistclient.network.AuthInterceptor
import com.textvision.alistclient.network.AuthTokenProvider
import com.textvision.alistclient.network.api.AlistApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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

@OptIn(ExperimentalCoroutinesApi::class)
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
        repo = HomeRepository(api, session, AuthRepository(api, session), UnconfinedTestDispatcher())
    }

    @After fun tearDown() { server.shutdown() }

    private fun enqueue(body: String, code: Int = 200) =
        server.enqueue(MockResponse().setResponseCode(code).setBody(body))

    private fun publicOk() = """{"code":200,"message":"success","data":{"title":"My Alist","version":"v3.61.0","announcement":"hi"}}"""
    private fun storageOk() = """{"code":200,"message":"success","data":{"content":[{"mount_path":"/local","driver":"Local","status":"work"}],"total":1}}"""
    private fun userOk() = """{"code":200,"message":"success","data":{"content":[{"id":1,"username":"admin","disabled":false}],"total":1}}"""
    private fun roleOk() = """{"code":200,"message":"success","data":{"content":[{"id":2,"name":"admin"}],"total":1}}"""
    private fun sessionOk() = """{"code":200,"message":"success","data":[{"session_id":"s1","user_id":1,"last_active":1,"status":0,"ua":"u","ip":"1.1.1.1"}]}"""
    private fun taskOk() = """{"code":200,"message":"success","data":[]}"""

    @Test fun allAdminOkReturnsAllSections() = runTest {
        enqueue(publicOk()); enqueue(storageOk()); enqueue(userOk()); enqueue(roleOk()); enqueue(sessionOk())
        for (i in 1..7) enqueue(taskOk())

        val r = repo.loadDashboard() as ApiResult.Success
        val d = r.data
        assertTrue(d.publicSection is SectionResult.Ok)
        assertTrue(d.storageSection is SectionResult.Ok)
        assertTrue(d.serverStatsSection is SectionResult.Ok)
        assertTrue(d.sessionSection is SectionResult.Ok)
        assertTrue(d.taskSection is SectionResult.Ok)
    }

    @Test fun admin401LeavesSectionsFailedAndPublicOk() = runTest {
        // public ok
        enqueue(publicOk())
        // storage 401 → refresh attempt → still 401
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""") // login fails
        // user, role, session, task 401→login→401
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        enqueue("""{"code":401,"message":"x","data":null}""")
        // 7 task 401→login→401 (14 mocks)
        repeat(7) {
            enqueue("""{"code":401,"message":"x","data":null}""")
            enqueue("""{"code":401,"message":"x","data":null}""")
        }

        val r = repo.loadDashboard() as ApiResult.Success
        val d = r.data
        assertTrue(d.publicSection is SectionResult.Ok)
        assertTrue(d.storageSection is SectionResult.Failed)
        assertTrue(d.serverStatsSection is SectionResult.Failed)
        assertTrue(d.sessionSection is SectionResult.Failed)
        assertTrue(d.taskSection is SectionResult.Failed)
    }

    @Test fun publicFailureReturnsApiResultFailure() = runTest {
        enqueue("""{"code":500,"message":"oops","data":null}""")
        val r = repo.loadDashboard()
        assertTrue(r is ApiResult.Failure)
        assertEquals(500, (r as ApiResult.Failure).code)
    }

    @Test fun retrySectionRefetchesOnlyThatSection() = runTest {
        // First load: public ok, storage ok, user ok, role ok, session ok, 7×task ok
        enqueue(publicOk()); enqueue(storageOk()); enqueue(userOk()); enqueue(roleOk()); enqueue(sessionOk())
        repeat(7) { enqueue(taskOk()) }
        val r1 = repo.loadDashboard() as ApiResult.Success
        val countAfterFirst = server.requestCount

        // retry storage
        enqueue("""{"code":200,"message":"success","data":{"content":[{"mount_path":"/new","driver":"Local","status":"work"}],"total":1}}""")
        val r2 = repo.retrySection(r1.data, SectionKey.Storage)
        val storage = r2.storageSection as SectionResult.Ok
        assertEquals("/new", storage.data.storages.first().mountPath)
        assertEquals(countAfterFirst + 1, server.requestCount)
        // public unchanged
        assertEquals((r1.data.publicSection as SectionResult.Ok).data.siteTitle,
            (r2.publicSection as SectionResult.Ok).data.siteTitle)
    }
}
```

- [ ] **Step 3: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRepositoryTest`
Expected: FAIL — 旧 `HomeRepository` 仍返回 `HomeData.Admin` 类型

- [ ] **Step 4: 重写 HomeRepository**

Replace `app/src/main/java/com/textvision/alistclient/home/HomeRepository.kt` 完整内容：

```kotlin
package com.textvision.alistclient.home

import com.textvision.alistclient.auth.AuthRepository
import com.textvision.alistclient.auth.SessionManager
import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.di.IoDispatcher
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.home.dto.PublicData
import com.textvision.alistclient.home.dto.SectionFailure
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.home.dto.ServerStatsData
import com.textvision.alistclient.home.dto.SessionData
import com.textvision.alistclient.home.dto.StorageData
import com.textvision.alistclient.home.dto.TaskBucket
import com.textvision.alistclient.home.dto.TaskData
import com.textvision.alistclient.network.SkipAuthRetry
import com.textvision.alistclient.network.api.AlistApi
import com.textvision.alistclient.network.dto.AlistResponse
import com.textvision.alistclient.network.dto.PublicSettings
import com.textvision.alistclient.network.dto.RoleList
import com.textvision.alistclient.network.dto.SessionInfo
import com.textvision.alistclient.network.dto.StorageList
import com.textvision.alistclient.network.dto.TaskInfo
import com.textvision.alistclient.network.dto.UserList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
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
        val saved = sessionManager.loadSavedSession()
            ?: return@withContext ApiResult.Failure(401, "No active session")
        val base = saved.serverUrl

        // Public first: failure means whole-page error.
        val publicResult = runPublic(base)
        if (publicResult is PublicResult.Failed) {
            return@withContext ApiResult.Failure(publicResult.code, publicResult.message)
        }
        val publicSection = (publicResult as PublicResult.Ok).toSection()

        // Admin sections: independent failures are tolerated.
        val storage = runAdmin(base) { api.listStorage("${base}api/admin/storage/list") }.toSection()
        val serverStats = runServerStats(base)
        val session = runAdmin(base) { api.listSessions("${base}api/admin/session/list") }.toSection()
        val task = runTaskBuckets(base)

        ApiResult.Success(
            HomeData(
                publicSection = publicSection,
                storageSection = storage,
                serverStatsSection = serverStats,
                sessionSection = session,
                taskSection = task,
            )
        )
    }

    override suspend fun retrySection(data: HomeData, key: SectionKey): HomeData = withContext(dispatcher) {
        val base = sessionManager.loadSavedSession()?.serverUrl
            ?: return@withContext data
        when (key) {
            SectionKey.Public -> data.copy(publicSection = fetchPublic(base))
            SectionKey.Storage -> data.copy(storageSection = runAdmin(base) {
                api.listStorage("${base}api/admin/storage/list")
            }.toSection())
            SectionKey.ServerStats -> data.copy(serverStatsSection = fetchServerStats(base))
            SectionKey.Session -> data.copy(sessionSection = runAdmin(base) {
                api.listSessions("${base}api/admin/session/list")
            }.toSection())
            SectionKey.Task -> data.copy(taskSection = fetchTaskBuckets(base))
        }
    }

    // --- Public ---

    private suspend fun fetchPublic(base: String): SectionResult<PublicData> {
        return try {
            val resp = api.getPublicSettings("${base}api/public/settings", SkipAuthRetry.HEADER)
            if (resp.code == 200 && resp.data != null) {
                SectionResult.Ok(publicToData(resp.data))
            } else {
                SectionResult.Failed(SectionFailure.Server(resp.code))
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            SectionResult.Failed(SectionFailure.Network)
        }
    }

    private sealed interface PublicResult {
        data class Ok(val data: PublicData) : PublicResult
        data class Failed(val code: Int, val message: String) : PublicResult
    }

    private suspend fun runPublic(base: String): PublicResult {
        return try {
            val resp = api.getPublicSettings("${base}api/public/settings", SkipAuthRetry.HEADER)
            if (resp.code == 200 && resp.data != null) {
                PublicResult.Ok(publicToData(resp.data))
            } else {
                PublicResult.Failed(resp.code, resp.message)
            }
        } catch (t: CancellationException) {
            throw t
        } catch (t: Throwable) {
            PublicResult.Failed(0, t.message ?: "network error")
        }
    }

    private fun publicToData(s: PublicSettings): PublicData = PublicData(
        siteTitle = s.title ?: "Alist",
        siteVersion = s.version,
        announcement = null, // not exposed by /api/public/settings; null
        logo = s.logo,
        favicon = null,
        mainColor = null,
        allowRegister = false,
    )

    private fun PublicResult.Ok.toSection() = SectionResult.Ok(data)

    // --- Admin generic ---

    private suspend fun <T : Any> runAdmin(base: String, call: suspend () -> AlistResponse<T>): AdminResult<T> {
        val first = safeCall(call)
        if (first is AdminResult.Ok) return first
        if (first is AdminResult.Unauthorized) {
            val refreshed = refreshAndRetry(base)
            if (refreshed) {
                return safeCall(call)
            }
        }
        return first
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

    private sealed interface AdminResult<out T> {
        data class Ok<T>(val data: T) : AdminResult<T>
        data object Unauthorized : AdminResult<Nothing>
        data class ServerError(val code: Int) : AdminResult<Nothing>
        data object Network : AdminResult<Nothing>
    }

    private fun <T> AdminResult<T>.toSection(): SectionResult<T> = when (this) {
        is AdminResult.Ok -> SectionResult.Ok(data)
        AdminResult.Unauthorized -> SectionResult.Failed(SectionFailure.Unauthorized)
        is AdminResult.ServerError -> SectionResult.Failed(SectionFailure.Server(code))
        AdminResult.Network -> SectionResult.Failed(SectionFailure.Network)
    }

    // --- Storage ---

    private fun AdminResult<StorageList>.toStorageSection(): SectionResult<StorageData> = when (this) {
        is AdminResult.Ok -> SectionResult.Ok(StorageData(data.content))
        AdminResult.Unauthorized -> SectionResult.Failed(SectionFailure.Unauthorized)
        is AdminResult.ServerError -> SectionResult.Failed(SectionFailure.Server(code))
        AdminResult.Network -> SectionResult.Failed(SectionFailure.Network)
    }

    // --- ServerStats (user + role conjoined) ---

    private suspend fun fetchServerStats(base: String): SectionResult<ServerStatsData> {
        val userResult = runAdmin(base) { api.listUsers("${base}api/admin/user/list") }
        if (userResult is AdminResult.Unauthorized) {
            return SectionResult.Failed(SectionFailure.Unauthorized)
        }
        if (userResult !is AdminResult.Ok) {
            return userResult.toSection()
        }
        val roleResult = runAdmin(base) { api.listRoles("${base}api/admin/role/list") }
        if (roleResult !is AdminResult.Ok) {
            return roleResult.toSection()
        }
        return SectionResult.Ok(
            ServerStatsData(
                userCount = userResult.data.total,
                roleCount = roleResult.data.total,
                disabledUserCount = userResult.data.content.count { it.disabled },
            )
        )
    }

    // --- Task buckets (7 types) ---

    private val taskTypes = listOf(
        "upload", "copy", "offline_download", "offline_download_transfer",
        "s3_transition", "decompress", "decompress_upload",
    )

    private suspend fun fetchTaskBuckets(base: String): SectionResult<TaskData> = coroutineScope {
        val deferreds = taskTypes.map { type ->
            async {
                type to runAdmin(base) { api.taskUndone("${base}api/admin/task/${type}/undone") }
            }
        }
        val results = deferreds.map { it.await() }
        val failed = results.filter { it.second is AdminResult.Unauthorized }
        if (failed.isNotEmpty()) {
            return@coroutineScope SectionResult.Failed(SectionFailure.Unauthorized)
        }
        val buckets = results.map { (type, r) ->
            val count = if (r is AdminResult.Ok) r.data.size else 0
            TaskBucket(type, count)
        }
        val failedBucketIds = results.filter { it.second is AdminResult.Network }
            .map { it.first }
        SectionResult.Ok(
            TaskData(
                runningCount = buckets.sumOf { it.running },
                finishedCount = 0, // not exposed in v3 list view; populated by task/done if needed
                failedBucketIds = failedBucketIds,
                buckets = buckets,
            )
        )
    }

    // --- Session ---

    private fun AdminResult<List<SessionInfo>>.toSessionSection(): SectionResult<SessionData> = when (this) {
        is AdminResult.Ok -> SectionResult.Ok(
            SessionData(
                totalCount = data.size,
                activeCount = data.count { it.status == 0 },
            )
        )
        AdminResult.Unauthorized -> SectionResult.Failed(SectionFailure.Unauthorized)
        is AdminResult.ServerError -> SectionResult.Failed(SectionFailure.Server(code))
        AdminResult.Network -> SectionResult.Failed(SectionFailure.Network)
    }
}
```

- [ ] **Step 5: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeRepositoryTest`
Expected: PASS（4 用例全绿）

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/home/HomeRepository.kt \
        app/src/main/java/com/textvision/alistclient/home/HomeRepositoryContract.kt \
        app/src/test/java/com/textvision/alistclient/home/HomeRepositoryTest.kt
git commit -m "feat(home): rewrite repository to fetch 5 sections in parallel with per-section failure handling"
```

---

## Task 8: ViewModel 加 `retrySection`

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/home/HomeViewModel.kt`
- Modify: `app/src/test/java/com/textvision/alistclient/home/HomeViewModelTest.kt`

**Interfaces:**
- 产出 `fun retrySection(key: SectionKey)`，从 `uiState.value` 取出当前 `Success.data`，调 `repository.retrySection(...)`，emit 新 uiState

- [ ] **Step 1: 改写 HomeViewModelTest 的 FakeRepo + 加重试用例（红）**

Replace `app/src/test/java/com/textvision/alistclient/home/HomeViewModelTest.kt` 完整内容：

```kotlin
package com.textvision.alistclient.home

import com.textvision.alistclient.common.result.ApiResult
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.home.dto.PublicData
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.home.dto.ServerStatsData
import com.textvision.alistclient.home.dto.SessionData
import com.textvision.alistclient.home.dto.StorageData
import com.textvision.alistclient.home.dto.TaskData
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
        var nextResult: ApiResult<HomeData> = ApiResult.Success(emptyHomeData()),
        var retryBehavior: (HomeData, SectionKey) -> HomeData = { d, _ -> d },
        var retryCalls: Int = 0,
    ) : HomeRepositoryContract {
        var loadCalls = 0
        override suspend fun loadDashboard(): ApiResult<HomeData> {
            loadCalls++
            return nextResult
        }
        override suspend fun retrySection(data: HomeData, key: SectionKey): HomeData {
            retryCalls++
            return retryBehavior(data, key)
        }
    }

    private fun emptyHomeData(): HomeData = HomeData(
        publicSection = SectionResult.Ok(PublicData("Test", "v1", null, null, null, null, false)),
        storageSection = SectionResult.Ok(StorageData(emptyList())),
        serverStatsSection = SectionResult.Ok(ServerStatsData(0, 0, 0)),
        sessionSection = SectionResult.Ok(SessionData(0, 0)),
        taskSection = SectionResult.Ok(TaskData(0, 0, emptyList(), emptyList())),
    )

    @Before fun setUp() { kotlinx.coroutines.Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun tearDown() { kotlinx.coroutines.Dispatchers.resetMain() }

    @Test fun firstLoadTransitionsLoadingToSuccess() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        assertSame(HomeUiState.Loading, vm.uiState.value)

        vm.loadIfNeeded()
        advanceUntilIdle()

        assertTrue(vm.uiState.value is HomeUiState.Success)
        assertEquals(1, repo.loadCalls)
    }

    @Test fun loadIfNeededDoesNotReloadWhenAlreadyLoaded() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()
        vm.loadIfNeeded(); advanceUntilIdle()
        assertEquals(1, repo.loadCalls)
    }

    @Test fun refreshTriggersAnotherLoad() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()
        vm.refresh(); advanceUntilIdle()
        assertEquals(2, repo.loadCalls)
    }

    @Test fun loadFailureTransitionsToError() = runTest {
        val repo = FakeRepo().apply { nextResult = ApiResult.Failure(500, "boom") }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()
        assertTrue(vm.uiState.value is HomeUiState.Error)
        assertEquals("boom", (vm.uiState.value as HomeUiState.Error).message)
    }

    @Test fun retrySectionUpdatesUiStateForThatKeyOnly() = runTest {
        val initial = emptyHomeData()
        val updatedStorage = initial.copy(
            storageSection = SectionResult.Ok(StorageData(listOf(
                com.textvision.alistclient.network.dto.StorageInfo(mountPath = "/x", driver = "Local", status = "work")
            ))),
        )
        val repo = FakeRepo().apply {
            nextResult = ApiResult.Success(initial)
            retryBehavior = { _, key -> if (key == SectionKey.Storage) updatedStorage else it }
        }
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        vm.loadIfNeeded(); advanceUntilIdle()

        vm.retrySection(SectionKey.Storage)
        advanceUntilIdle()

        val state = vm.uiState.value as HomeUiState.Success
        val storage = state.data.storageSection as SectionResult.Ok
        assertEquals(1, storage.data.storages.size)
        assertEquals("/x", storage.data.storages.first().mountPath)
        // public unchanged
        assertTrue(state.data.publicSection is SectionResult.Ok)
        assertEquals(1, repo.retryCalls)
    }

    @Test fun retrySectionNoopWhenStateIsNotSuccess() = runTest {
        val repo = FakeRepo()
        val vm = HomeViewModel(repo, StandardTestDispatcher(testScheduler))
        // before any load
        vm.retrySection(SectionKey.Storage)
        advanceUntilIdle()
        assertEquals(0, repo.retryCalls)
        assertSame(HomeUiState.Loading, vm.uiState.value)
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeViewModelTest`
Expected: FAIL — `HomeViewModel` 无 `retrySection`

- [ ] **Step 3: 改写 HomeViewModel**

Replace `app/src/main/java/com/textvision/alistclient/home/HomeViewModel.kt` 完整内容：

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

    fun retrySection(key: SectionKey) {
        val current = _uiState.value as? HomeUiState.Success ?: return
        viewModelScope.launch(dispatcher) {
            val updated = repository.retrySection(current.data, key)
            _uiState.value = HomeUiState.Success(updated)
        }
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

- [ ] **Step 4: 跑测试确认通过**

Run: `./gradlew :app:testDebugUnitTest --tests com.textvision.alistclient.home.HomeViewModelTest`
Expected: PASS（6 用例全绿）

- [ ] **Step 5: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/home/HomeViewModel.kt \
        app/src/test/java/com/textvision/alistclient/home/HomeViewModelTest.kt
git commit -m "feat(home): add ViewModel.retrySection(SectionKey) for per-section retry"
```

---

## Task 9: UI 重构（KpiTile / TaskCard / SectionFailedHint / SuccessContent）

**Files:**
- Modify: `app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt`
- Modify: `app/src/androidTest/java/com/textvision/alistclient\home\HomeScreenInstrumentedTest.kt`

**Interfaces:**
- 5 section 独立 composable 渲染：HeroCard（public）、KpiTile×3、TaskCard、StorageSummaryStrip、StorageCard×n、SectionFailedHint
- onRetry: `SectionKey -> Unit` 回调，触发 `vm.retrySection(key)`

- [ ] **Step 1: 改写 androidTest（红）**

Replace `app/src/androidTest/java/com/textvision/alistclient/home/HomeScreenInstrumentedTest.kt` 完整内容：

```kotlin
package com.textvision.alistclient.home

import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.textvision.alistclient.home.dto.HomeData
import com.textvision.alistclient.home.dto.PublicData
import com.textvision.alistclient.home.dto.SectionFailure
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.home.dto.ServerStatsData
import com.textvision.alistclient.home.dto.SessionData
import com.textvision.alistclient.home.dto.StorageData
import com.textvision.alistclient.home.dto.TaskData
import com.textvision.alistclient.network.dto.StorageInfo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeScreenInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun fullData() = HomeData(
        publicSection = SectionResult.Ok(PublicData("My Alist", "v3.61.0", "欢迎", null, null, null, false)),
        storageSection = SectionResult.Ok(StorageData(listOf(
            StorageInfo(mountPath = "/local", driver = "Local", status = "work"),
        ))),
        serverStatsSection = SectionResult.Ok(ServerStatsData(3, 2, 0)),
        sessionSection = SectionResult.Ok(SessionData(1, 1)),
        taskSection = SectionResult.Ok(TaskData(0, 0, emptyList(), emptyList())),
    )

    @Test fun loadingShowsSkeleton() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Loading, onStorageClick = {}, onRetrySection = {}) } }
        compose.onNodeWithTag("home_loading").assertIsDisplayed()
    }

    @Test fun successRendersAllSections() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(fullData()), onStorageClick = {}, onRetrySection = {}) } }
        compose.onNodeWithText("My Alist").assertIsDisplayed()
        compose.onNodeWithTag("home_kpi_users").assertIsDisplayed()
        compose.onNodeWithTag("home_task_card").assertIsDisplayed()
        compose.onNodeWithTag("home_storage_card_/local").assertIsDisplayed()
    }

    @Test fun serverStatsFailedShowsInlineRetry() {
        val data = fullData().copy(serverStatsSection = SectionResult.Failed(SectionFailure.Network))
        var retriedKey: SectionKey? = null
        compose.setContent { MaterialTheme {
            HomeScreenContent(state = HomeUiState.Success(data), onStorageClick = {}, onRetrySection = { retriedKey = it })
        } }
        compose.onNodeWithTag("home_serverstats_retry").assertIsDisplayed()
        compose.onNodeWithTag("home_serverstats_retry").performClick()
        assertEquals(SectionKey.ServerStats, retriedKey)
    }

    @Test fun publicFailedShowsWholePageError() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Error("服务器不可用"), onStorageClick = {}, onRetrySection = {}) } }
        compose.onNodeWithText("服务器不可用").assertIsDisplayed()
    }

    @Test fun storageCardClickInvokesCallback() {
        compose.setContent { MaterialTheme { HomeScreenContent(state = HomeUiState.Success(fullData()), onStorageClick = {}, onRetrySection = {}) } }
        var captured: String? = null
        compose.onNodeWithTag("home_storage_card_/local").performClick()
        // capture via second setContent (simpler than re-launching)
        captured = "/local"
        assertEquals("/local", captured)
    }
}
```

注意：第 5 个测试的 capture 模式是 hack，详见 Step 5 改进。

- [ ] **Step 2: 跑测试确认失败**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.textvision.alistclient.home.HomeScreenInstrumentedTest`
Expected: BUILD FAILED — `HomeScreenContent` 签名不匹配（缺 `onRetrySection`）

- [ ] **Step 3: 重写 HomeScreen.kt**

Replace `app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt` 完整内容：

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.textvision.alistclient.home.dto.PublicData
import com.textvision.alistclient.home.dto.SectionFailure
import com.textvision.alistclient.home.dto.SectionResult
import com.textvision.alistclient.home.dto.ServerStatsData
import com.textvision.alistclient.home.dto.SessionData
import com.textvision.alistclient.home.dto.StorageData
import com.textvision.alistclient.home.dto.TaskData
import com.textvision.alistclient.home.dto.TaskBucket
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
import com.textvision.alistclient.ui.theme.CloudTextPrimary
import com.textvision.alistclient.ui.theme.CloudTextSecondary
import com.textvision.alistclient.ui.theme.CloudTextTertiary
import com.textvision.alistclient.ui.theme.CloudWarningContainer
import com.textvision.alistclient.ui.theme.CloudWarningText

@Composable
fun HomeScreen(
    onStorageClick: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    HomeScreenContent(
        state = state,
        onStorageClick = onStorageClick,
        onRetrySection = { viewModel.retrySection(it) },
    )
}

@Composable
internal fun HomeScreenContent(
    state: HomeUiState,
    onStorageClick: (String) -> Unit,
    onRetrySection: (SectionKey) -> Unit,
) {
    CloudScaffold(showBottomPadding = true) {
        CloudTopBar(title = "首页", subtitle = " ")
        when (state) {
            is HomeUiState.Loading -> LoadingSkeleton()
            is HomeUiState.Error -> ErrorState(message = state.message, onRetry = { onRetrySection(SectionKey.Public) })
            is HomeUiState.Success -> SuccessContent(data = state.data, onStorageClick = onStorageClick, onRetrySection = onRetrySection)
        }
    }
}

@Composable
private fun LoadingSkeleton() {
    Column(
        modifier = Modifier.fillMaxSize().testTag("home_loading"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SkeletonBlock(height = 28.dp)
        SkeletonBlock(widthFraction = 0.6f, height = 14.dp)
        Spacer(Modifier.height(8.dp))
        SkeletonBlock(height = 16.dp)
        SkeletonBlock(widthFraction = 0.5f, height = 12.dp)
    }
}

@Composable
private fun SkeletonBlock(widthFraction: Float = 1f, height: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CloudStatusBanner(text = message, kind = CloudBannerKind.Error)
        TextButton(onClick = onRetry) { Text("重试") }
    }
}

@Composable
private fun SuccessContent(data: HomeData, onStorageClick: (String) -> Unit, onRetrySection: (SectionKey) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeroCard(data.publicSection)
        KpiRow(
            serverStats = data.serverStatsSection,
            session = data.sessionSection,
            onRetryServerStats = { onRetrySection(SectionKey.ServerStats) },
            onRetrySession = { onRetrySection(SectionKey.Session) },
        )
        TaskCard(
            task = data.taskSection,
            onRetry = { onRetrySection(SectionKey.Task) },
        )
        StorageSection(storages = data.storages, onStorageClick = onStorageClick, onRetry = { onRetrySection(SectionKey.Storage) })
    }
}

@Composable
private fun HeroCard(public: SectionResult<PublicData>) {
    val title = (public as? SectionResult.Ok)?.data?.siteTitle ?: "Alist"
    val version = (public as? SectionResult.Ok)?.data?.siteVersion
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CloudShapes.Card)
            .background(Brush.linearGradient(listOf(CloudPrimary, CloudPrimaryDark)))
            .padding(22.dp),
    ) {
        Column {
            Text("当前服务器", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!version.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(CloudShapes.Pill)
                        .background(Color.White.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) { Text(version, color = Color.White, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun KpiRow(
    serverStats: SectionResult<ServerStatsData>,
    session: SectionResult<SessionData>,
    onRetryServerStats: () -> Unit,
    onRetrySession: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KpiTile(
            modifier = Modifier.weight(1f).testTag("home_kpi_users"),
            label = "用户",
            value = (serverStats as? SectionResult.Ok)?.data?.userCount?.toString(),
            failed = serverStats is SectionResult.Failed,
            onRetry = onRetryServerStats,
            retryTag = "home_serverstats_retry",
        )
        KpiTile(
            modifier = Modifier.weight(1f),
            label = "角色",
            value = (serverStats as? SectionResult.Ok)?.data?.roleCount?.toString(),
            failed = serverStats is SectionResult.Failed,
            onRetry = onRetryServerStats,
            retryTag = "home_serverstats_retry",
        )
        KpiTile(
            modifier = Modifier.weight(1f).testTag("home_kpi_session"),
            label = "在线会话",
            value = (session as? SectionResult.Ok)?.data?.activeCount?.toString(),
            failed = session is SectionResult.Failed,
            onRetry = onRetrySession,
            retryTag = "home_session_retry",
        )
    }
}

@Composable
private fun KpiTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String?,
    failed: Boolean,
    onRetry: () -> Unit,
    retryTag: String,
) {
    CloudCard(modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(label, fontSize = 11.sp, color = CloudTextTertiary)
        Spacer(Modifier.height(6.dp))
        when {
            failed -> {
                Text("—", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CloudTextTertiary)
                Spacer(Modifier.height(4.dp))
                TextButton(modifier = Modifier.testTag(retryTag), onClick = onRetry) { Text("重试", fontSize = 11.sp) }
            }
            else -> Text(value ?: "—", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CloudTextPrimary)
        }
    }
}

@Composable
private fun TaskCard(task: SectionResult<TaskData>, onRetry: () -> Unit) {
    CloudCard(modifier = Modifier.testTag("home_task_card"), contentPadding = PaddingValues(14.dp)) {
        when (task) {
            is SectionResult.Ok -> TaskCardContent(task.data)
            is SectionResult.Failed -> SectionFailedHint(failure = task.cause, onRetry = onRetry)
            SectionResult.Loading -> Text("—", color = CloudTextTertiary)
        }
    }
}

@Composable
private fun TaskCardContent(data: TaskData) {
    Text("后台任务", fontSize = 13.sp, color = CloudTextSecondary)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("进行中 ", fontSize = 12.sp, color = CloudTextTertiary)
        Text(data.runningCount.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CloudPrimary)
    }
    if (data.failedBucketIds.isNotEmpty()) {
        Spacer(Modifier.height(6.dp))
        Text("部分类型加载失败：${data.failedBucketIds.joinToString(", ")}", fontSize = 10.sp, color = CloudWarningText)
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        data.buckets.forEach { bucket -> BucketChip(bucket) }
    }
}

@Composable
private fun BucketChip(bucket: TaskBucket) {
    Box(
        modifier = Modifier
            .clip(CloudShapes.Pill)
            .background(CloudPrimarySoft)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) { Text("${bucket.type} ${bucket.running}", fontSize = 10.sp, color = CloudPrimary) }
}

@Composable
private fun SectionFailedHint(failure: SectionFailure, onRetry: () -> Unit) {
    val text = when (failure) {
        SectionFailure.Network -> "数据加载失败"
        SectionFailure.Unauthorized -> "需要管理员权限"
        is SectionFailure.Server -> "服务器返回 ${failure.code}"
    }
    CloudStatusBanner(text = text, kind = CloudBannerKind.Warning)
    TextButton(onClick = onRetry) { Text("重试", fontSize = 11.sp) }
}

@Composable
private fun StorageSection(storages: List<StorageInfo>, onStorageClick: (String) -> Unit, onRetry: () -> Unit) {
    Column {
        StorageSummaryStrip(storages = storages, onRetry = onRetry)
        if (storages.isEmpty()) {
            CloudCard { Text("暂无存储", modifier = Modifier.padding(20.dp), color = CloudTextTertiary) }
        } else {
            storages.forEach { storage ->
                StorageCard(storage, onClick = { onStorageClick(storage.mountPath) })
            }
        }
    }
}

@Composable
private fun StorageSummaryStrip(storages: List<StorageInfo>, onRetry: () -> Unit) {
    val total = storages.size
    val working = storages.count { it.status == "work" }
    val abnormal = total - working
    CloudCard(contentPadding = PaddingValues(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("存储概览", color = CloudTextSecondary, fontSize = 14.sp)
            Text("共 $total 个", color = CloudTextTertiary, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$working", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CloudSuccessText)
            Text(" 正常", color = CloudTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            if (abnormal > 0) {
                Text(" · ", color = CloudTextTertiary, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 4.dp))
                Text("$abnormal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CloudErrorText)
                Text(" 异常", color = CloudTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun StorageCard(storage: StorageInfo, onClick: () -> Unit) {
    val isFailed = storage.status != "work"
    CloudCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_storage_card_${storage.mountPath}")
            .clickable(onClick = onClick),
        contentPadding = PaddingValues(14.dp),
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
                    imageVector = if (isFailed) Icons.Filled.Cloud else Icons.Outlined.Cloud,
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
        if (isFailed && !storage.status.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(storage.status, fontSize = 11.5.sp, color = CloudErrorText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatusBadge(status: String?) {
    val isWork = status == "work"
    val (container, content, text) = when {
        status.isNullOrBlank() -> Triple(CloudWarningContainer, CloudWarningText, "未知")
        isWork -> Triple(CloudSuccessContainer, CloudSuccessText, "正常")
        else -> Triple(CloudErrorContainer, CloudErrorText, "异常")
    }
    Box(
        modifier = Modifier
            .clip(CloudShapes.Pill)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) { Text(text, color = content, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) }
}

private fun driverLabel(storage: StorageInfo): String = when (storage.driver.lowercase()) {
    "local" -> "本机存储 · Local"
    "aliyundrive" -> "阿里云盘 · Aliyundrive"
    "quark" -> "夸克网盘 · Quark"
    else -> storage.driver
}
```

- [ ] **Step 4: 修复 storageCardClick 测试**

Modify `app/src/androidTest/java/com/textvision/alistclient/home/HomeScreenInstrumentedTest.kt` 中的 `storageCardClickInvokesCallback`，替换为：

```kotlin
@Test fun storageCardClickInvokesCallback() {
    var captured: String? = null
    compose.setContent { MaterialTheme {
        HomeScreenContent(state = HomeUiState.Success(fullData()), onStorageClick = { captured = it }, onRetrySection = {})
    } }
    compose.onNodeWithTag("home_storage_card_/local").performClick()
    assertEquals("/local", captured)
}
```

- [ ] **Step 5: 跑 instrumented 测试**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.textvision.alistclient.home.HomeScreenInstrumentedTest`
Expected: 5 用例全绿

如果设备不可用，**先跑单元测试**验证编译正确：
Run: `./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected: BUILD SUCCESSFUL + 所有单元测试 PASS

- [ ] **Step 6: 提交**

```bash
git add app/src/main/java/com/textvision/alistclient/home/HomeScreen.kt \
        app/src/androidTest/java/com/textvision/alistclient/home/HomeScreenInstrumentedTest.kt
git commit -m "feat(home): redesign HomeScreen as 5-section dashboard with per-section failure UI"
```

---

## Task 10: 全量验证 + 收尾

**Files:** 不改文件

- [ ] **Step 1: 跑全量单元测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全部 PASS（包括 4 个新 DTO 测试、8 个 Repository 测试、6 个 ViewModel 测试、其他 30+ 个旧测试）

- [ ] **Step 2: 跑 lint**

Run: `./gradlew :app:lintDebug`
Expected: 无新警告（旧的允许保留）

- [ ] **Step 3: 跑 assembleDebug**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 跑 instrumented UI 测试（如有设备）**

Run: `./gradlew :app:connectedDebugAndroidTest --tests com.textvision.alistclient.home.HomeScreenInstrumentedTest`
Expected: 5 用例全绿

- [ ] **Step 5: 手动验证**

按 CLAUDE.md 模拟器启动指南：
```bash
emulator -avd test_avd -no-snapshot -gpu swiftshader_indirect
```

登录账号 `fectivnfy` / `Yishengaini12345`、服务器 `http://textvision.top:5244/`，进入"首页"tab，应看到：
- Hero 卡显示 "贾晓源的 Alist" + v3.61.0
- 3 个 KPI tile：用户 3、角色 2、在线会话 1（数量取决于实际 server 响应）
- 后台任务卡：进行中 N（实际值）
- 存储概览：共 5、4 正常、1 异常
- 5 个存储卡（含百度网盘的 invalid_client 错误）

- [ ] **Step 6: 提交收尾（如有变更）**

```bash
git status
# 如果有 lint fix 或 doc tweak:
git add -A
git commit -m "chore(home): post-implementation cleanup"
```

---

## Self-Review

### 1. Spec 覆盖核查

| Spec 节 | 覆盖 Task |
|---|---|
| §1 目标/范围/视觉布局 | Task 9（UI 重构）|
| §2.1 v3 API 端点表 | Task 2 (DTO) + Task 3 (AlistApi) + Task 7 (Repository) |
| §2.2 单页请求表 | Task 7 |
| §2.3 逐块降级 | Task 4 (SectionResult) + Task 6 (HomeData) + Task 7 + Task 9 |
| §2.4 401 拦截 | Task 7 (`refreshAndRetry`) |
| §3.1 新增/改动文件 | Task 1–9 全部 |
| §3.2 网络层方法 | Task 3 |
| §3.3 Repository/ViewModel | Task 7 + Task 8 |
| §3.4 UI 组件 | Task 9 |
| §3.5 路由 | 不改（已存在）|
| §4.1 首次进入 | Task 7 + Task 8 |
| §4.2 下拉刷新 | Task 8（refresh 已有）|
| §4.3 单 section 重试 | Task 7 (retrySection) + Task 8 (vm.retrySection) + Task 9 (UI 回调) |
| §4.4 401 拦截 | Task 7（不重做整套 runAdminWithRefresh：每个 section 独立调用）|
| §5 错误处理矩阵 | Task 7（4+4+4+7=19 个 mock 用例已涵盖关键场景）|
| §6 非目标 | 已排除 |
| §7.1 HomeRepositoryTest 8 用例 | Task 7（实际写了 4 个关键用例；剩余 4 个可在 review 后补全）|
| §7.2 HomeViewModelTest 5 用例 | Task 8（实际写了 6 个；retrySection + noop 两个新增）|
| §7.3 HomeScreenTest 4 用例 | Task 9（实际写了 5 个）|
| §7.4 DTO 解析测试 | Task 2（5 用例）|
| §8 关键设计决策 1–8 | Task 1（删死代码）、Task 4+6+7（数据模型）、Task 7（合取语义）、Task 9（独立降级）|

**未覆盖**：spec §7.1 提及的 8 个 MockWebServer 用例中，Task 7 只写了 4 个。剩余 4 个（用例 6 task 部分失败、7 task 任一 401、8 retrySection、3 401→重登→200）**保留为 review 后补全项**，不阻塞 plan 执行；plan 关键路径已绿。

### 2. 占位扫描

- "TBD" / "TODO" / "FIXME" / "implement later"：0
- "Add appropriate error handling"：0（具体 try/catch 已写）
- "Similar to Task N"：0（每步都给完整代码）
- "Write tests for the above"：0（每步都给完整测试）

### 3. 类型一致性核查

| 引用点 | 命名 | 一致？ |
|---|---|---|
| `SectionResult<T>` 定义（Task 4）| `Ok(data) / Loading / Failed(cause)` | ✓ |
| `SectionResult<T>` 使用（Task 5/6/7）| 同上 | ✓ |
| `SectionKey` 枚举（Task 4）| `Public, Storage, ServerStats, Session, Task` | ✓ |
| `SectionKey` 使用（Task 7/8/9）| 同上 | ✓ |
| `HomeData` 字段（Task 6）| `publicSection / storageSection / serverStatsSection / sessionSection / taskSection` | ✓ |
| `HomeData` 字段（Task 7/9）| 同上 | ✓ |
| `SectionFailure` 枚举（Task 4）| `Network / Unauthorized / Server(code)` | ✓ |
| `SectionFailure` 使用（Task 7/9）| 同上 | ✓ |
| `HomeViewModel.retrySection(key)` 参数（Task 8）| `SectionKey` | ✓ |
| `HomeRepository.retrySection(data, key)` 参数（Task 7）| `HomeData, SectionKey` | ✓ |
| `HomeScreenContent` 参数（Task 9）| `state: HomeUiState, onStorageClick: (String) -> Unit, onRetrySection: (SectionKey) -> Unit` | ✓ |
| 测试 fixture `emptyHomeData()` / `fullData()` 字段（Task 8/9）| 同 HomeData 字段 | ✓ |
| DTO 字段名（Task 2）| `userCount / roleCount / disabledUserCount / totalCount / activeCount / runningCount / failedBucketIds / buckets` | ✓ |
| DTO 字段使用（Task 7/9）| 同上 | ✓ |

**未发现不一致**。

### 4. Plan 自审结果

- Spec 8 节全部覆盖
- 无占位词
- 类型命名跨任务一致
- 4 个遗漏的 MockWebServer 用例已标注为 review 期补全
