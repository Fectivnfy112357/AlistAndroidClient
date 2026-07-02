# 首页 Dashboard 设计

日期：2026-07-02

## 背景与目标

当前 Android 客户端启动后直接进入文件列表，缺少对当前 Alist 服务器的"总览"视角。用户连接一台新服务器时，最关心的问题通常不是某目录下的文件，而是：

- 这是哪台服务器？版本多新？启动了多久？
- 总空间还剩多少？
- 都有哪些挂载的存储？哪些能用、哪些挂了？

本设计新增一个底部导航 tab "首页"，承载以上信息。MVP 范围只做"基础版"：站点信息 + 总用量 + 存储列表，不做最近活动。

## 非目标

- 不做快捷操作入口（上传 / 全局搜索 / 主题切换）。
- 不做服务器之间的切换或账号管理。
- 不做定时轮询——只用"首次进入 + 下拉刷新"。
- 不做存储卡片详情页（v1 点击直接跳文件列表对应路径）。

## 端点与权限策略

Alist v3 提供以下相关端点：

| 端点 | 方法 | 需要 admin | 返回内容 |
|---|---|---|---|
| `/api/admin/info` | POST | 是 | 版本、构建时间、启动时间、总用量 |
| `/api/admin/storage/list` | POST | 是 | 所有存储的 mountPath / driver / 状态 / 用量 |
| `/api/public/settings` | GET | 否 | 站点标题、版本号、Logo |
| `/api/fs/list` 路径 `/` | POST | 登录即可 | 根目录文件列表（兜底聚合） |

**降级策略**（推荐方案 A）：

1. 优先并发请求 `admin/info` + `admin/storage/list`。
2. 若任一返回 `code != 200` 且不是网络错误（典型为 401/403），视为非管理员：
   - Hero 区降级为 `public/settings`（站点名 + 版本号），运行时长显示 `—`。
   - 存储列表区显示 `CloudStatusBanner(kind = Info, text = "当前为游客身份，存储详情不可用")`。
3. 若两个 admin 端点都返回 `NetworkError`，再降级请求 `public/settings`（如果 public 也失败，渲染错误态 + 重试按钮）。
4. `runAlistWithRefresh` 已经覆盖 401 自动重登；403 通常意味着账号是 guest，重登也无用，直接走降级。

## 架构与组件边界

### 新增文件

```
app/src/main/java/com/textvision/alistclient/home/
  HomeRepository.kt              数据层（端点选择 + 401 重试 + 兜底）
  HomeRepositoryContract.kt      接口（便于测试）
  HomeViewModel.kt               @HiltViewModel
  HomeScreen.kt                  @Composable
  HomeUiState.kt                 sealed class 状态
  dto/AdminDtos.kt               AdminInfo / StorageInfo / StorageList / PublicSettings
```

### 改动文件

| 文件 | 改动 |
|---|---|
| `network/api/AlistApi.kt` | 增加 `adminInfo`、`listStorage`、`getPublicSettings` 三个方法 |
| `navigation/AppRoute.kt` | 增加 `data object Home : AppRoute("home")` |
| `navigation/AppNavHost.kt` | 增加 `composable(Home.route) { HomeScreen(...) }`；`showBottomBar` 集合加入 Home |
| `ui/components/CloudBottomBar.kt` | 增加第 1 个 item："首页"，图标 `Icons.Outlined.Cloud` |
| `ui/screens/FileScreen.kt` | 接受可选 `initialPath: String?` 参数；`LaunchedEffect` 用其触发 `vm.loadIfNeeded(initialPath ?: "/")` |
| `di/AppModule.kt` | 增加 `@Binds bindHomeRepository` |
| `file/FileRepository.kt` | 增加 `listRoot()` 方法用于兜底聚合（可选实现；若不使用此兜底，可省略） |

**职责**：
- `HomeRepository` 是数据唯一出口，对外只暴露 `loadDashboard(): ApiResult<HomeData>`。
- `HomeViewModel` 持有 `HomeUiState`，对外暴露 `load()`、`refresh()`、`isOnline`。
- `HomeScreen` 只渲染 + 把"点击存储卡片"翻译成 `onStorageClick(mountPath)` 回调。
- `FileScreen` 的 `initialPath` 参数兼容现有调用（`AppRoute.Files.route` 不带参时默认 `/`）。

## 数据模型

```kotlin
// AdminInfo：/api/admin/info 返回 data
@Serializable
data class AdminInfo(
    val version: String? = null,
    val buildDate: String? = null,
    val startTime: String? = null,
)

// StorageList：/api/admin/storage/list 返回 data
@Serializable
data class StorageList(
    val content: List<StorageInfo> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class StorageInfo(
    val id: Long? = null,
    val mountPath: String,
    val driver: String,
    val status: String? = null,
    val usedBytes: Long = 0,
    val totalBytes: Long = 0,
)

// PublicSettings：/api/public/settings 返回 data
@Serializable
data class PublicSettings(
    val title: String? = null,
    val logo: String? = null,
    val version: String? = null,
)
```

> **实现注意**：以上字段是按 Alist 文档大致推断的占位，**实现时需用 MockWebServer/真实响应核对实际 JSON 字段名并修正**。如果字段名是 `total` / `used` / `Used` 之类，DTO 加 `@SerialName` 显式映射。

## UI 状态机

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(
        val serverTitle: String,
        val serverVersion: String?,
        val startTime: Instant?,         // null 时运行时长显示 "—"
        val totalBytes: Long,
        val usedBytes: Long,
        val storages: List<StorageInfo>, // 空列表 + isGuest=true 时显示降级提示
        val isGuest: Boolean,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
```

`HomeViewModel.loadIfNeeded()` 与 `refresh()`：
- `loadIfNeeded()`：当 `hasLoadedInitial == true` 直接返回，避免每次切回 Home 都重新拉取。
- `refresh()`：强制重新加载。
- `launchIn(viewModelScope)`，cancel 上一个 `loadJob`。

## 数据流

```
HomeScreen LaunchedEffect(Unit) -> vm.loadIfNeeded()
  -> HomeRepository.loadDashboard()
       并发 awaitAll([adminInfo(), listStorage()])
         → 成功：组装 AdminDashboardData
         → 任一失败且 cause 是 ApiResult.Failure 且 code in {401,403}:
              → PublicDashboardData（只调 public/settings）
              → 标记 isGuest=true
         → 两者都是 NetworkError：再降级 public/settings；public 也失败返回 ApiResult.Failure(...)
  -> HomeViewModel: 根据 ApiResult 分发到 Loading/Success/Error
  -> HomeScreen: 按 UiState 渲染
```

## UI 设计

### 整体布局

```
┌──────────────────────────────────────────────┐
│ ← CloudTopBar "首页" / "http://..."          │
├──────────────────────────────────────────────┤
│ [PullToRefreshBox]                           │
│                                              │
│ ┌── Hero Card ──────────────────────────────┐ │
│ │ 站点名（大字 22sp）                       │ │
│ │ v3.25.0  · 已运行 3 天 4 小时             │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ ┌── Total Usage Card ──────────────────────┐ │
│ │ 已用 12.4 GB / 50 GB                      │ │
│ │ [████░░░░░░░░░░░░] 24%                    │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ 存储 (3)                                     │
│ ┌── Storage Card ─────────────────────────┐ │
│ │ 📁 /local      [正常]                    │ │
│ │ 本机存储      12.4 GB / 50 GB            │ │
│ │ [████░░░░░░░░] 24%                       │ │
│ └──────────────────────────────────────────┘ │
│ ┌── Storage Card ─────────────────────────┐ │
│ │ ☁️ /aliyun      [正常]                   │ │
│ │ 阿里云盘       2.1 GB / 100 GB          │ │
│ │ [█░░░░░░░░░░░] 2%                        │ │
│ └──────────────────────────────────────────┘ │
│                                              │
│ [CloudBottomBar: 首页 | 文件 | 传输 | 设置]  │
└──────────────────────────────────────────────┘
```

### 组件复用

- `CloudScaffold` 容器
- `CloudTopBar` 标题 + 副标题（副标题为当前 serverUrl）
- `CloudCard` 包装每张卡
- `CloudListItem` 不直接使用（卡片要展示进度条，自定义 Column 即可）
- `CloudStatusBanner` 错误/降级提示
- `LinearProgressIndicator` (Material3) 已用比例
- `PullToRefreshBox` (Material3 1.3+) 下拉刷新
- 加载骨架：3 个 `CloudCard` + 占位 Box (shimmer 可选，MVP 用纯灰块即可)

### 降级状态（isGuest = true）

- Hero 卡片保留但运行时长字段显示 `—`
- 在 Hero 卡片下方插入：`CloudStatusBanner(kind = Info, text = "当前为游客身份，存储详情不可用")`
- 存储列表标题 `存储 (—)`，列表区域显示单条提示文字

### 错误状态

```
┌── CloudStatusBanner (Error) ────────────┐
│ 无法连接到服务器 / 服务器拒绝访问       │
│                       [重试]            │
└─────────────────────────────────────────┘
```

### 运行时长计算

`HomeScreen` 持有 `LaunchedEffect(startTime)` 启动一个 60s tick 的 `Flow`，每秒或每分钟（折中：每 30s）重算 `Duration.between(startTime, now)` 并触发 recomposition。组件离开时通过 `cancel` 取消。

## 路由改动

```kotlin
// AppRoute.kt
sealed class AppRoute(val route: String) {
    data object Home : AppRoute("home")          // 新增
    data object Files : AppRoute("files?path={path}") {  // 改为可带 path
        fun create(path: String = "/"): String =
            "files?path=${Uri.encode(path)}"
    }
    data object Transfers : AppRoute("transfers")
    data object Settings : AppRoute("settings")
    // ... 其余不变
}
```

`AppNavHost`：
- `composable(Home.route) { HomeScreen(onStorageClick = { mountPath -> navController.navigate(AppRoute.Files.create(mountPath)) }) }`
- `composable(Files.route, arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "/" })) { entry -> FileScreen(initialPath = entry.arguments?.getString("path") ?: "/", ...) }`

`CloudBottomBar`：第一项改为 `Home`，其余顺序不变：`首页 / 文件 / 传输 / 设置`。

## 错误处理矩阵

| 场景 | 处理 |
|---|---|
| admin/info 返回 401 | 降级公共端点，isGuest=true |
| admin/info 返回 403 | 同上 |
| admin/storage/list 返回 401 | 降级：Hero 用 admin/info 结果，存储列表用降级提示 |
| admin 端点 NetworkError | 二次降级到 public/settings |
| public/settings NetworkError | 渲染 Error UiState，附重试按钮 |
| `startTime` 解析失败 | 运行时长字段显示 `—` |
| 存储 `status = "fail"` | 卡片右下角红色 `CloudBannerKind.Error` 小标签 |
| 存储 `totalBytes = 0` | 进度条显示为灰色 0%，副标题省略已用 |

## 测试策略

### HomeRepositoryTest（MockWebServer）

| 用例 | 输入 | 断言 |
|---|---|---|
| admin 全部成功 | admin/info 200 + admin/storage/list 200 | 返回 AdminDashboardData，isGuest=false |
| 仅 admin/info 成功 | admin/info 200, storage/list 401 | 返回 PartialData，存储列表为空 |
| 全失败降级 | admin 401 + public 200 | 返回 PublicDashboardData，isGuest=true |
| 全失败 | admin 401 + public 500 | 返回 ApiResult.Failure(500, ...) |
| 401 自动重登 | admin 401 → 重登 → admin 200 | 第二次 admin 调用，token 已更新 |

### HomeViewModelTest（StandardTestDispatcher）

| 用例 | 断言 |
|---|---|
| 首次 load | UiState 从 Loading → Success |
| refresh 已加载 | 再次触发 load，UiState 经历 Loading → Success |
| loadIfNeeded 已加载 | 不触发新的 loadJob |
| 加载失败 | UiState = Error(message) |
| refresh 取消上一次 | 旧 loadJob.cancel() 被调用 |

### HomeScreenTest（Compose UI Test）

| 用例 | 断言 |
|---|---|
| 渲染 Loading | 显示 3 个骨架卡片 |
| 渲染 Success | Hero 显示站点名 + 版本；存储列表项数 == 数据 |
| 渲染 Error | 显示 CloudStatusBanner + 重试按钮 |
| 渲染 isGuest | 顶部显示降级提示 Banner |
| 点击存储 | 触发 `onStorageClick(mountPath)` 回调，参数正确 |

### 路由测试

| 用例 | 断言 |
|---|---|
| `AppRoute.Files.create("/local")` | `"files?path=%2Flocal"` |
| `AppRoute.Files.create()` | `"files?path=%2F"` |
| 默认 path 参数 | 当路由不含 path 时，`AppNavHost` 给 `initialPath` 传 `/` |

## 风险与未决

1. **字段名不确定**：Alist `/api/admin/storage/list` 实际 JSON 字段名需要在实现时通过 MockWebServer 或真实服务器抓包核对；DTO 中已注明 `usedBytes`/`totalBytes` 是占位。
2. **路径编码**：mountPath 如 `/local` 含斜杠，Uri.encode 会编为 `%2Flocal`；路由解码后 `/` 必须正确还原。需要 `navArgument` + `Uri.decode` 配合。
3. **拉取时机**：`PullToRefreshBox` 触发回调时要避免和 `LaunchedEffect(Unit)` 重复拉取，需在 ViewModel 维护 `hasLoadedInitial` 并只让 `refresh()` 重置它。
4. **admin/storage/list 在 Alist v3.25+ 可能改成分页**：如果实际是分页接口，需要循环 page=1,2,... 直到 content 为空。实现时核对。