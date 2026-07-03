# 首页 Dashboard 重新设计 — 服务器总览 / 仪表盘网格

日期：2026-07-03
状态：已确认，待用户审查

## 1. 背景与目标

`HomeScreen` 现行实现仅展示「服务器标题 + 存储概览 + 存储列表」，对连接到的 Alist v3 服务器描述力不足。本设计重写首页为「服务器总览仪表盘」，把 v3 公开/admin API 实际可拿到的全部关于服务器的核心信息组织进首页，让用户进入 App 第一眼了解：

- 这是哪台服务器，标题 / 版本 / 公告
- 服务器整体规模（用户 / 角色 / 活跃会话）
- 是否在跑任务、各类任务在跑几条
- 哪些存储可用、哪些挂了

### 1.1 范围

- **做**：站点信息卡 + 服务器指标 KPI 网格 + 后台任务卡 + 存储概览卡 + 存储列表 + 逐块独立降级；下拉刷新；首次进入不重复拉取；每个端点失败不影响其他端点
- **不做**：详见 §6 非目标

### 1.2 视觉布局（方案 B：仪表盘网格式 — 已确认）

```
┌──────────────────────────────────────────┐
│ ← 首页                                    │
├──────────────────────────────────────────┤
│ ╔══ Hero 渐变卡 ═══════════════════════╗ │
│ ║ 当前服务器                              ║ │
│ ║ 贾晓源的 Alist                          ║ │
│ ║ [v3.61.0] [● 开放注册] [主题色点]       ║ │
│ ║ 📢 公告：欢迎使用 Alist                 ║ │
│ ╚════════════════════════════════════════╝ │
│                                          │
│ ┌ 用户 ┐ ┌ 角色 ┐ ┌ 在线会话 ┐           │
│ │  3   │ │  2   │ │   1     │           │
│ └──────┘ └──────┘ └─────────┘           │
│                                          │
│ ┌── 后台任务 ────────────────────────┐   │
│ │ 进行中 0  已完成 12  失败 0          │   │
│ │ [上传 0][复制 0][离线 0][S3 0]…    │   │
│ └─────────────────────────────────────┘   │
│                                          │
│ ┌── 存储概览 ────────────────────────┐   │
│ │ 共 5 · 4 正常 · 1 异常                │   │
│ └─────────────────────────────────────┘   │
│                                          │
│ 存储 (5)                                 │
│ ┌── /我的文件    [正常] ──────────────┐   │
│ ┌── /我的百度    [异常 invalid_client] ┐  │
│ ...                                      │
│                                          │
│ [首页 | 文件 | 传输 | 设置]               │
└──────────────────────────────────────────┘
```

降级示例（用户 KPI 失败、其余正常）：

```
┌ 用户 ⚠ ┐ ┌ 角色 ┐ ┌ 在线会话 ┐
│数据加载  │ │  2   │ │   1     │
│  失败   │ └──────┘ └─────────┘
└────────┘
```

公开端点失败 → 整页 Error + 重试按钮（无站点信息没法渲染）。

## 2. 端点策略

### 2.1 v3 API 实际可达端点（实测确认）

公开端点（无 token）：
- `GET /api/public/settings` — Flag=PUBLIC 的 setting map（站点标题 / 版本 / 公告 / Logo / 主题色）

Admin 端点（需 admin JWT）：
- `GET /api/admin/storage/list` — 存储列表
- `GET /api/admin/user/list` — 用户列表
- `GET /api/admin/role/list` — 角色列表
- `GET /api/admin/session/list` — 当前会话
- `POST /api/admin/task/{upload,copy,offline_download,offline_download_transfer,s3_transition,decompress,decompress_upload}/undone` — 7 类任务进行中列表

**已确认不存在**：`/api/admin/info`（v3 路由表查无）。现有 `AdminInfo` DTO / Retrofit `adminInfo` 方法删除。

### 2.2 单页请求表

| Section | 端点 | 鉴权 | 失败时 |
|---|---|---|---|
| PublicSection | `GET /api/public/settings` | 无 | 整页 Error |
| StorageSection | `GET /api/admin/storage/list` | admin JWT | 该 section 显示 inline 失败，其他 section 正常 |
| ServerStatsSection | `GET /api/admin/user/list` + `GET /api/admin/role/list` | admin JWT | 任一失败 → 整段 Failed（用户/角色合并语义，不分别降级） |
| SessionSection | `GET /api/admin/session/list` | admin JWT | 同上 |
| TaskSection | 7 × `POST /api/admin/task/*/undone` | admin JWT | 任一 401/403 → 整段 Failed；NetworkError 单点忽略但合并显示 |

### 2.3 逐块降级规则

每个 section 三态独立：`Ok / Loading / Failed`：

| Section 状态 | UI 表现 |
|---|---|
| `Ok` | 正常渲染该卡 |
| `Loading`（仅首次进入） | 卡内骨架占位，**不阻碍滚动** |
| `Failed(Unauthorized)` | 该卡画 inline 灰色提示「需要管理员权限」+ 小图标 |
| `Failed(Network)` | inline 灰色提示「数据加载失败」+ 单卡重试按钮（点击只重试该 section） |
| `Failed(Server)` | inline 灰色提示「服务器返回 {code}」 |

首屏加载：`Loading` 必须短暂可见后再 `Ok/Failed`。
下拉刷新：成功的 section 立刻展示缓存，失败的 section 重新发起请求并显示 Loading。

### 2.4 401/403 公共拦截

`runAdminWithRefresh` 已存在于 `HomeRepository`：401 触发 `authRepository.login()` 重登，重试一次。403 不重试（说明账号本身无权限），直接 Failed(Unauthorized)。

## 3. 数据模型与组件边界

### 3.1 新增 DTO 与文件

```
app/src/main/java/com/textvision/alistclient/network/dto/
├── AdminDtos.kt          改：删 AdminInfo；新增 UserList/User/RoleList/Role
                          /Session/SessionInfo/TaskInfo/TaskState

app/src/main/java/com/textvision/alistclient/home/dto/
├── HomeData.kt           改：data class HomeData 容纳 5 个 section 结果
├── PublicSection.kt      新：解析 public/settings
├── StorageSection.kt     新：storage/list + 健康度统计
├── ServerStatsSection.kt 新：user + role 计数
├── SessionSection.kt     新：session list + 活跃计数
└── TaskSection.kt        新：7 类 undone 任务汇总
```

**HomeData**：

```kotlin
data class HomeData(
    val publicSection: SectionResult<PublicData>,
    val storageSection: SectionResult<StorageData>,
    val serverStatsSection: SectionResult<ServerStats>,
    val sessionSection: SectionResult<SessionData>,
    val taskSection: SectionResult<TaskData>,
)
```

**AdminInfo 删除清单**：
- `app/src/main/java/com/textvision/alistclient/network/dto/AdminDtos.kt`：删 `AdminInfo`、`AdminInfoRequest`
- `app/src/main/java/com/textvision/alistclient/network/api/AlistApi.kt`：删 `adminInfo` 方法
- `app/src/test/java/com/textvision/alistclient/home/HomeRepositoryTest.kt`：删 admin/info 相关 mock

### 3.2 网络层新增方法

```kotlin
@GET suspend fun listUsers(@Url url: String, @Query("page") Int = 1, @Query("per_page") Int = 0): AlistResponse<UserList>
@GET suspend fun listRoles(@Url url: String, @Query("page") Int = 1, @Query("per_page") Int = 0): AlistResponse<RoleList>
@GET suspend fun listSessions(@Url url: String): AlistResponse<List<SessionInfo>>
@POST suspend fun taskUndone(@Url url: String): AlistResponse<List<TaskInfo>>
```

DTO 字段对齐（仅列 JSON tag）：

| DTO | JSON tag | 类型 |
|---|---|---|
| `UserList.content` | `content` | `List<User>` |
| `UserList.total` | `total` | Int |
| `User.id` | `id` | Long |
| `User.username` | `username` | String |
| `User.disabled` | `disabled` | Boolean |
| `User.role` | `role` | String（int 数组序列化） |
| `RoleList.content` | `content` | `List<Role>` |
| `RoleList.total` | `total` | Int |
| `Role.id` | `id` | Long |
| `Role.name` | `name` | String |
| `Role.description` | `description` | String |
| `SessionInfo.session_id` | `session_id` | String |
| `SessionInfo.user_id` | `user_id` | Long |
| `SessionInfo.last_active` | `last_active` | Long |
| `SessionInfo.status` | `status` | Int（0=active / 1=inactive） |
| `SessionInfo.ua` | `ua` | String |
| `SessionInfo.ip` | `ip` | String |
| `TaskInfo.id` | `id` | String |
| `TaskInfo.name` | `name` | String |
| `TaskInfo.state` | `state` | String |
| `TaskInfo.status` | `status` | String |
| `TaskInfo.progress` | `progress` | Double |
| `TaskInfo.total_bytes` | `total_bytes` | Long |
| `TaskInfo.error` | `error` | String |

**User.password 字段不解析**——DTO 显式省略，规避 user.go 端点未置空风险。

### 3.3 Repository 与 ViewModel

**HomeRepository**（`HomeRepositoryContract.kt` 不变：`suspend fun loadDashboard(): ApiResult<HomeData>` 签名不变）。

实现要点：
1. `runAdminWithRefresh`：401 重登逻辑保留
2. 改为**并发拉取** 5 个 section，每个独立 try/catch + 翻译为 `SectionResult`
3. Public 失败 → 返回 `ApiResult.Failure(code, message)`（整页错误）
4. 其余 4 section 失败 → 当下填 `SectionResult.Failed(...)`，返回 `ApiResult.Success(HomeData(...))`，UI 按 section 逐块降级
5. Task 7 个端点：`coroutineScope { 7×async{} }`，**部分失败容忍**：单个 NetworkError 计入 failedBuckets，其他成功的不影响；任一 401/403 → 整段 Unauthorized

**HomeViewModel**：
- 保留 `loadIfNeeded` / `refresh` / `hasLoadedInitial` 语义
- 保留 `StubNetworkMonitor` 二次构造器（现有测试依赖）
- 新增 `retrySection(section: SectionKey)`：仅重新拉取一个 section，更新对应 result

**首次 Loading 状态机**：`HomeUiState.Loading` 仅在 `publicSection` 未返回时维持；public 一旦 Ok，无论其他 section 是否完成，立即切到 `HomeUiState.Success` 并按各 section 状态独立渲染。下拉刷新期间也是同样规则。

**HomeUiState**：

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState                       // 首次，5 section 都没回
    data class Success(val data: HomeData) : HomeUiState   // public 必 ok，其他 section 各自状态
    data class Error(val message: String) : HomeUiState    // public 失败
}
```

### 3.4 UI 组件

**HomeScreen.kt 重构**：现有 `HeroCard / StorageSummaryCard / StorageCard / StatusBadge / driverLabel` 全部内联私有函数保留或替换。新增以下私有 Composable：

```kotlin
@Composable private fun HeroCard(data: PublicData?, isGuest: Boolean)
@Composable private fun KpiTile(label: String, value: String?, state: SectionResult<*>, onRetry: () -> Unit)
@Composable private fun ServerStatsRow(sections: Result<ServerStats>, onRetryServerStats: () -> Unit)
@Composable private fun TaskCard(data: SectionResult<TaskData>, onRetryTask: () -> Unit)
@Composable private fun StorageSummaryStrip(storages: List<StorageInfo>)
@Composable private fun StorageCard(storage: StorageInfo, onClick: () -> Unit)
@Composable private fun SectionFailedHint(text: String, onRetry: () -> Unit)
```

`KpiTile.value` 仅在该 section 处于 `Ok` 时为非空字符串；`Loading` 时显示灰色 `—`；`Failed` 时不传 value，由 tile 内部画失败提示 + 重试按钮。

首屏 `Loading` 状态：仅当公开端点 `public/settings` 尚未返回时进入 `HomeUiState.Loading`。成功响应的 section 立刻渲染，不再回退 Loading。骨架只在公开端点返回 > 100ms 时显示，避免闪烁。

### 3.5 路由

**不改动**。`AppRoute.Home` 已存在，`AppNavHost` 已挂载。

## 4. 数据流

### 4.1 首次进入

```
HomeScreen LaunchedEffect(Unit)
  → vm.loadIfNeeded()
    → repository.loadDashboard()
        ├── publicSection = fetchPublic(base)        // suspend
        │     成功 → PublicData
        │     失败 → ApiResult.Failure(...)
        ├── return  (if public failed)
        ├── storageSection       = runAdmin(...)
        ├── serverStatsSection   = runAdmin(...)     // user + role 双调用，合并
        ├── sessionSection       = runAdmin(...)
        └── taskSection          = runAdmin(...)     // 7×并发
    → ApiResult.Success(HomeData(publicSection, storage, ...))
  → vm.uiState = HomeUiState.Success(data)
  → HomeScreen 渲染各 section
```

### 4.2 下拉刷新

```kotlin
fun refresh() {
    hasLoadedInitial = false
    load()
}
```

行为同首次：先回 `Loading`，再回 `Success`。

### 4.3 单 section 重试

```kotlin
fun retrySection(key: SectionKey) {
    viewModelScope.launch {
        val current = _uiState.value as? Success ?: return@launch
        val updated = repository.retrySection(current.data, key)  // 新增方法
        _uiState.value = HomeUiState.Success(updated)
    }
}
```

`HomeRepository.retrySection` 单 endpoint 重试，更新 HomeData 对应字段。

### 4.4 401 拦截

`runAdminWithRefresh` 已在使用：401 → `authRepository.login()` → 第二次调用。第二次仍失败则 Failed(Unauthorized)。

**重要**：retry 一个 section 时不重做整套 runAdminWithRefresh，否则会重复刷 token。仅在原 loadDashboard 路径里执行。

## 5. 错误处理矩阵

| 场景 | 处理 |
|---|---|
| `public/settings` 401 | 视为服务器问题 → 整页 Error（公开端点不应 401） |
| `public/settings` 网络失败 | 整页 Error |
| `admin/storage/list` 401 → 重登 → 成功 | 重试一次拿结果 |
| `admin/storage/list` 401 → 重登 → 仍 401 | section Failed(Unauthorized) |
| `admin/storage/list` 403 | section Failed(Unauthorized) |
| `admin/storage/list` 网络失败 | section Failed(Network)，UI 显示 inline 重试 |
| `admin/user/list` 401 + `admin/role/list` 200 | ServerStats 整段 Failed(Unauthorized)（合取语义） |
| `admin/task/upload/undone` 单点网络失败 | 该桶计入 failedBuckets，其他桶正常返回 |
| `admin/task/*/undone` 任一 401 | Task 整段 Failed(Unauthorized) |
| `startTime` / `announcement` 解析失败 | UI 显示空字符串或「—」 |
| 存储 `status = "fail"` | 卡片右下角红色「异常」徽章 + 错误描述行 |

## 6. 非目标（明确不做）

- **不做**：定时轮询；服务器切换 / 账号管理；快捷操作入口（上传 / 全局搜索 / 主题切换）；存储详情页（点击直接跳 FileScreen 对应路径）；存储用量条（v3 不暴露）；开始构建时间（v3 不暴露）；CPU/内存/磁盘；趋势线图表。
- **不做 v1**：会话/用户/角色/任务等明细页。本设计只展示**总数 / 汇总**，明细页合入「设置」页作为 phase 2 入口。
- **不做**：索引/搜索、Meta/Label/Setting 三类 admin 端点（隐私 / 冷门 / 与首页关系弱）。
- **清理**：删 `AdminInfo` DTO 与 Retrofit `adminInfo`（v3 无此端点）。

## 7. 测试策略

### 7.1 HomeRepositoryTest（MockWebServer）

每个用例使用 `serialized` 形式串行提交响应。`saved` 注入 admin JWT。

| # | 用例 | 公共 | storage | user | role | session | task | 断言 |
|---|---|---|---|---|---|---|---|---|
| 1 | 全 admin 成功 | 200 | 200 | 200 | 200 | 200 | 7×200 | 5 section 全 Ok，HomeData 字段匹配 mock |
| 2 | 公共 OK，admin 全 401 | 200 | 401→重登→401 | 401→... | 同上 | 同上 | 同上 | public Ok，其余 4 section Failed(Unauthorized) |
| 3 | 公共 OK，storage 401 → 重登 → 200 | 200 | 401→200 | 200 | 200 | 200 | 7×200 | storage Ok，其余同前 |
| 4 | 公共 OK，user 200，role 网络错 | 200 | 200 | 200 | throw | 200 | 7×200 | ServerStats Failed(Network)，其他 Ok |
| 5 | 公共 500 | 500 | — | — | — | — | — | 返回 ApiResult.Failure(500) |
| 6 | task 4×200，3×网络错 | 200 | 200 | 200 | 200 | 200 | 4×200 + 3×throw | TaskSection.Ok，failedBuckets 含 3 个 id |
| 7 | task 任一 401 | 200 | 200 | 200 | 200 | 200 | 6×200 + 1×401 | TaskSection.Failed(Unauthorized) |
| 8 | retrySection(storage) | — | — | — | — | — | — | 仅重试 storage 端点，其他 section 保持原结果 |

### 7.2 HomeViewModelTest

| # | 用例 | 断言 |
|---|---|---|
| 1 | 首次 load | Loading → Success |
| 2 | loadIfNeeded 已加载 | 不再触发 loadJob |
| 3 | refresh 已加载 | 触发新 loadJob |
| 4 | 重试某 section | uiState.data 中该 section 更新，其他不变 |
| 5 | 加载失败 | uiState = Error(message) |

### 7.3 HomeScreenTest (Compose UI)

| # | 用例 | 断言 |
|---|---|---|
| 1 | Success 全部 Ok | 显示 Hero + 3 KPI tile + Task 卡 + 概览 + 5 存储卡 |
| 2 | Section Failed（ServerStats） | ServerStats 区域显示 inline 失败卡 + 重试；其他正常 |
| 3 | Public 失败 | 整页 Error + 重试 |
| 4 | 点击存储卡 | onStorageClick(mountPath) 回调被调用 |

### 7.4 DTO 解析测试

`AdminDtosTest`：用真实服务器抓包 JSON 作为 fixture，验证 `listUsers / listRoles / listSessions / taskUndone` 各 DTO 字段能正确解析。（避免 JSON tag 漏字段导致运行时崩溃）

## 8. 关键设计决策（已确认）

1. **方案 B（仪表盘网格式）**：3 个 KPI tile + 任务卡 + 概览 + 存储列表。
2. **ServerStats 合并语义**：user + role 任一失败整段 Failed，不分别降级（避免出现"用户 3 但角色 —"的半成品状态；测试 §7.1 用例 4 显式覆盖此点）。
3. **Public 失败 → 整页 Error**：站点信息是首屏渲染根基。
4. **逐块降级**：4 个 admin section 各自三态独立；KPI tile 内可单独 Failed。
5. **Task 7 桶容忍部分网络错**：单点 NetworkError 计入失败桶，UI 列出哪些桶失败；任一鉴权错则整段 Unauthorized。
6. **详情页延后**：用户在「设置」页加 admin 明细入口（phase 2）。
7. **删除死代码**：`AdminInfo` DTO 与 `AlistApi.adminInfo` 方法。
8. **不做**：容量、趋势图、开始时间、CPI/内存、定时轮询。

## 9. 风险与未决

1. **AdminDtos JSON tag**：以本机 MockWebServer 抓的 v3.61.0 响应为准实现；本地真服务器实测时若发现字段名差异，需修正 `@SerialName`。
2. **Task 端点路由**：v3 的 7 类任务（upload / copy / offline_download / offline_download_transfer / s3_transition / decompress / decompress_upload）路径来自 `server/handles/task.go::SetupTaskRoute`。若服务端版本较低可能少几类——用反射式解析不行，只能按字面路径拼，缺失的 path 静默跳过（计入 failedBuckets）。
3. **路由权限**：`/api/admin/task/*/undone` 在非 admin 账号下服务端只返回该用户自己创建的 task，admin 账号返回全部。鉴权失败时整段走 Failed(Unauthorized)。
4. **session 列表大小**：用户量大时 list 可能很长；首屏只展示 active count，不展开列表。

## 10. 附录：mockup 摘要

已生成的浏览器可视化 mockup 在 `C:\Users\32115\AppData\Local\Temp\home-mockups.html`，三方案并排对比，已确认选 B。
