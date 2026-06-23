# Alist Android 客户端 — 设计文档（MVP 第一版）

- 日期：2026-06-23
- 目标：为 Alist v3 构建一个通用 Android 文件管理客户端（手机端）
- 状态：设计已完成，待用户复核后进入实现规划

## 修订记录

| 版本 | 日期 | 修订内容 |
|------|------|----------|
| 1.0 | 2026-06-23 | 初版，5 轮设计对话整合 |
| 1.1 | 2026-06-23 | 补充 Alist v3 API 端点、DTO 字段映射、关键依赖版本、测试与安全细节 |

---

## 0. 定位

面向手机使用的 **原生 Android 文件管理客户端**，不是 WebView 包装，也不是 Alist 管理后台。

第一版定位为：

> Alist v3 通用 Android 文件管理客户端 MVP

任何用户都可以填写自己的 Alist v3 服务器地址、用户名、密码登录使用。

---

## 1. 第一版包含

### 1.1 账号登录

- 输入 Alist 服务器地址、用户名、密码
- 调用 Alist v3 登录接口获取 token
- 保存服务器地址、用户名、密码、token
- 密码使用 AndroidX EncryptedSharedPreferences（Keystore 保护主密钥），不明文落盘
- token 失效时自动用保存的账号密码重新登录
- 登录失败分两类提示：连接失败（地址错、超时、DNS 失败、非 Alist 服务）/ 认证失败（用户名密码错）
- HTTP 明文连接在登录输入框下方**实时**显示警告（不是登录失败时才提示）

### 1.2 文件页

- 浏览远程目录
- 路径面包屑导航
- 下拉刷新
- 排序
- 搜索
- 文件 / 文件夹图标、大小、更新时间
- 图片显示缩略图（Alist 缩略图接口 + Coil 3），其他类型显示默认类型图标
- 单选 / 多选模式
- 操作：上传、下载、删除、新建文件夹、重命名、复制、移动
  - 复制 / 移动：调用 Alist 服务端 API，不经本机中转，不消耗本机流量与存储
  - 上传：仅支持系统文件选择器（`ActivityResultContracts.GetContent()`），不支持从其他 App 接收分享

### 1.3 传输页

- 上传任务列表、下载任务列表
- 显示文件名、目标路径、进度、速度、状态、失败原因
- 支持取消、失败重试
- **仅前台运行**；App 后台 / 锁屏后不保证传输持续
- 通知栏显示前台运行时的进度
- 暂停 / 继续“尽量支持”，不作为第一版硬性成功标准

### 1.4 预览与分享

- 图片、文本 App 内预览
- 音视频优先 App 内或系统播放器打开
- 其他文件调用系统应用打开
- 支持系统分享
- 复制 / 分享 Alist 文件链接，提示鉴权与外网可访问风险

### 1.5 设置页

- 显示当前服务器地址、用户名
- 退出登录
- 修改服务器地址和账号
- 清理临时预览文件
- 预留后续入口：多账号、管理员、外部网盘

### 1.6 网络异常与错误处理

- 无网络 / 服务器不可达：统一兜底 UI + 重试入口
- 操作失败：行内 / Snackbar 提示失败原因
- token 失效：自动用保存账号密码重登；失败回登录页

---

## 2. 第一版明确不做

- 外部网盘 / 存储挂载管理
- 管理员面板
- 多服务器 / 多账号切换
- 离线目录缓存
- Alist v2 兼容
- 回收站 / 撤销删除
- 复杂分享规则（有效期、密码、权限）
- 完整 Office / PDF 内置预览
- 横屏 / 平板适配
- 接收其他 App 的分享入口
- 后台传输持续运行的保证
- 断点续传 / 分块上传
- 公共 Downloads 可见文件
- 下载路径用户配置
- 自签 HTTPS 证书支持
- Crashlytics 接入
- Android 15+（API 35+）适配
- 国产厂商魔改（MIUI/ColorOS/EMUI/HarmonyOS）适配

---

## 3. 页面结构

底部三栏：文件 / 传输 / 设置。登录页独立显示，不进入底部导航。

启动流程：

```
打开 App
  ├─ 无保存账号 → 登录页
  └─ 有保存账号 → 自动登录 / 校验 token
        ├─ 成功 → 文件页
        └─ 失败 → 登录页，显示失败原因
```

---

## 4. 技术架构与模块划分

### 4.1 技术栈

```
Kotlin + Jetpack Compose + Material 3 + MVVM + Repository
+ Retrofit / OkHttp + AndroidX EncryptedSharedPreferences
+ Coil 3 + Room + Hilt
```

### 4.2 应用结构

单 Activity、多页面 Compose Navigation，单 Gradle 模块 + 包结构（第一版不拆多模块）。

```
MainActivity
  └─ AppNavHost
      ├─ LoginScreen
      ├─ FileScreen
      ├─ TransferScreen
      ├─ SettingsScreen
      ├─ PreviewScreen
      └─ MoveCopyTargetPickerScreen
```

### 4.3 包结构

```
auth        登录、token 管理、自动重登、退出登录
network     Retrofit/OkHttp、Alist API 封装、错误转换
storage     服务器地址、用户名、密码、token 的加密保存
file        目录浏览、排序、搜索、选择模式、文件操作
transfer    上传/下载任务、进度、取消、失败重试、通知栏进度
preview     图片/文本/音视频/外部打开/系统分享
settings    账号信息、退出登录、清理临时文件、后续入口
common      通用 UI、错误模型、加载状态、工具函数
```

### 4.4 数据访问层

Repository 划分：

```
AuthRepository
  login()
  relogin()
  logout()
  getSavedSession()

FileRepository
  list(path)
  mkdir(path, name)
  rename(path, name)
  delete(paths)
  copy(src, dst)
  move(src, dst)
  search(path, keyword)
  getDownloadUrl(path)
  getThumbnailUrl(path)

TransferRepository / TransferManager
  enqueueUpload()
  enqueueDownload()
  cancel()
  retry()
```

页面不直接调用 Retrofit，通过 Repository / UseCase 调用。Alist API 有差异时只改数据层。

#### 业务错误包络

Alist v3 响应格式：`{"code":200,"message":"success","data":{...}}`。HTTP 200 但 `code != 200` 也算业务错误。统一用 sealed class：

```kotlin
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val code: Int, val message: String) : ApiResult<Nothing>
    data class NetworkError(val cause: Throwable) : ApiResult<Nothing>
}
```

### 4.5 登录与 token 管理

登录状态由 SessionManager 统一维护。token 失效 / 401 用 OkHttp **Authenticator**（不用 Interceptor）实现自动重登：

```kotlin
authenticator = object : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // 用保存的账号密码重登，拿到新 token 后
        // return response.request.newBuilder().header("Authorization", newToken).build()
        // 重登失败 return null，触发上层 401 处理
    }
}
```

约束：

- 同一时间只允许一个 relogin，避免多个 401 重复登录
- Authenticator 限制重试次数，避免无限循环
- 请求体不可重复发送的上传请求，401 不强行自动重试，标记任务失败并提示用户重试
- 退出登录时取消所有进行中的请求和传输任务，清理 token 和密码

#### 密码保存

- 不明文落盘
- 使用 AndroidX EncryptedSharedPreferences（底层由 Keystore 保护主密钥）
- token 和密码都视为敏感信息
- 设置页退出登录时清除保存的 token、密码、账号信息
- 若实施时依赖状态或官方推荐发生变化，替换为等价 Keystore-backed 加密存储，接口保持在 `CredentialStore` 内部

### 4.6 文件页架构

FileViewModel 用 sealed interface 管理状态，不用散字段：

```kotlin
sealed interface FileUiState {
    data class Loading(val path: String) : FileUiState
    data class Success(
        val path: String,
        val items: List<FileItem>,
        val selectedItems: Set<String> = emptySet(),
        val isMultiSelectMode: Boolean = false,
    ) : FileUiState
    data class Error(val path: String, val cause: Throwable) : FileUiState
}
```

行为：

```
进入目录    → list(path)
下拉刷新    → list(currentPath)
搜索        → 调 Alist 搜索 API；不可用则当前目录内过滤
排序        → 本地排序当前列表
删除        → 二次确认 → delete(paths)
复制/移动    → 选择目标目录 → 调 Alist 服务端 copy/move API（不经本机）
上传        → 系统文件选择器 → 创建上传任务
下载        → 创建下载任务
```

复制/移动目标选择复用 `DirectoryBrowser` composable，与 FileScreen 共用。

#### 图片缩略图

- 图片文件 → Alist 缩略图接口 + Coil 3 加载
- 其他文件 → 按扩展名显示默认类型图标
- Coil 内存缓存：限制 15-20% 应用堆；磁盘缓存单独目录
- 缩略图请求统一通过带 Authorization header 的 OkHttp Client，不依赖 URL 签名长期有效
- 加载失败回退默认类型图标（`AsyncImage` 的 `error` / `fallback`）

### 4.7 传输架构

App 内 TransferManager，前台传输任务系统，不承诺后台/锁屏持续运行。

理由：Foreground Service 需要 `FOREGROUND_SERVICE_DATA_SYNC` 权限，Android 14 收紧类型限制；国产厂商保活是噩梦，MVP 阶段不应被拖住。

#### Room 持久化

传输任务必须写 Room，字段：任务 ID、文件名、源/目标路径、已传输字节/总字节、任务类型、状态、失败原因、创建/更新时间。App 启动时从 Room 加载任务列表。进程被杀后未完成任务显示“已中断”，提供重试入口，重试从头开始，不做断点续传。

#### 下载文件位置

第一版固定存 App private + FileProvider：

```
filesDir/downloads/   ← 下载完成的文件（用户期望保留）
cacheDir/preview/     ← 预览临时文件（可被系统清）
```

本地命名：`filesDir/downloads/{sha1(path)}.ext`，Room 记录 `originalPath → localFile` 反查，避免不同路径同名覆盖。

#### 并发限制

- 上传并发 = 2
- 下载并发 = 3
- 用 `Semaphore` 在 TransferManager 里限流，不依赖 OkHttp dispatcher

#### 任务状态

```
等待中
上传中 / 下载中
成功
失败
已取消
已中断（进程被杀后保留）
```

#### 任务能力

- 显示进度、速度
- 支持取消、失败重试
- 通知栏显示前台运行期间的进度
- App 后台 / 锁屏后系统可能挂起传输，不保证继续推进
- App 进程被杀后，进行中任务在 Room 中保留为“已中断”，重新打开后可提示重试

### 4.8 预览与分享架构

按文件类型分流：

```
图片          → App 内图片预览
文本          → App 内文本预览
音视频        → 优先 App 内播放器；失败则系统播放器
PDF / Office / 压缩包 / 其他 → 系统应用打开
```

受保护文件：App 用 token 下载到临时文件，通过 FileProvider 授权给系统应用打开/分享。

分享分两类：

- 系统分享：使用本地临时文件 / 已下载文件调用系统分享面板
- 链接分享：复制或分享 Alist 文件链接，提示对方可能因鉴权、权限、内网问题无法访问

### 4.9 依赖注入：Hilt

推荐 Hilt（Google 官方，Compose + ViewModel 集成最好，编译期验证）。作用域：

```
@Singleton
  OkHttpClient / Retrofit / SessionManager / TransferManager / Room Database / Coil ImageLoader

@ViewModelScoped
  Repository
```

不推荐 Koin（运行时错误）或手写 ServiceLocator。

### 4.10 Alist v3 API 端点清单

第一版按 Alist v3 当前官方 autodocs 与真实服务器联调实现。端点细节集中封装在 `network/AlistApi` 与 Repository 层，UI 层不依赖任何端点字段。

| 方法 | 端点 | 入参 | 出参 | 用途 |
|------|------|------|------|------|
| POST | `/api/auth/login` | JSON body `{username, password}` | `{token: String}` | 登录 |
| POST | `/api/fs/list` | JSON body `{path, page, per_page}` | `{content: [AlistFile]}` | 列目录 |
| POST | `/api/fs/mkdir` | JSON body `{path}` | `code/message` | 新建文件夹 |
| POST | `/api/fs/rename` | JSON body `{path, name}` | `code/message` | 重命名 |
| POST | `/api/fs/remove` | JSON body，按当前 Alist v3 实测适配；Repository 对外仍暴露 `delete(paths)` | `code/message` | 删除 |
| POST | `/api/fs/copy` | JSON body，按当前 Alist v3 实测适配；Repository 对外仍暴露单文件 `copy(srcPath, dstDir)` | `code/message` | 复制 |
| POST | `/api/fs/move` | JSON body，优先按官方 autodocs 的 `{src_path, dst_path}`；若目标版本支持批量 names，则仅在 Repository 内适配 | `code/message` | 移动 |
| POST | `/api/fs/search` | JSON body `{path, keywords, page, per_page}` | `{content: [AlistFile]}` | 搜索 |
| PUT | `/api/fs/put` | binary 或 multipart body，携带目标路径信息；具体 header/body 以当前 Alist v3 实测为准 | `code/message` 或 task 信息 | 主上传方案 |
| POST | `/api/fs/form` | multipart/form-data（若目标版本支持） | `code/message` 或 task 信息 | 可选上传兼容方案 |
| GET | `/d/{path}` | `Authorization` header；若有 sign 则附加 sign | 文件流 | 下载 |
| GET | `/p/{path}` | `Authorization` header；若有 sign/thumb 则附加 sign | 图片流 | 缩略图 |
| GET | `/api/me` | `Authorization` header | 当前用户信息 | 可选 token 校验；第一版也可用根目录 list 校验 |

说明：

- `/api/fs/list` 和 `/api/fs/search` 使用 POST JSON body，不使用 GET query。
- 上传主方案为 `PUT /api/fs/put`；`/api/fs/form` 只作为目标版本支持时的兼容方案。
- 下载和缩略图请求统一走带 `Authorization` header 的 OkHttp Client，不依赖签名 URL 长期有效。
- 上传进度第一版由 OkHttp request body progress 计算，不依赖管理员任务查询接口。
- 管理员 task API 不作为第一版必需能力；管理员面板已明确延后。
- copy/move/remove 的具体 request body 允许在 Repository 内按 Alist v3 实测适配，但 Repository 对 UI 暴露的接口保持稳定。

### 4.11 Alist 字段映射约定

Alist v3 返回字段包含 snake_case，所有 DTO 必须用 `@SerialName` 显式映射，避免字段解析错误。

```kotlin
@Serializable
data class AlistResponse<T>(
    @SerialName("code") val code: Int,
    @SerialName("message") val message: String,
    @SerialName("data") val data: T? = null,
)

@Serializable
data class AlistLoginData(
    val token: String,
)

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
```

关键约定：

- 统一用 `kotlinx-serialization`，不使用 Moshi。
- DTO 用 `*Dto` 后缀；App 内部模型用 `FileItem`，不带 DTO 后缀。
- DTO → App model 在 Repository 层完成，UI 不直接使用 DTO。
- Alist list 响应中的文件项不强制要求带完整 path；内部完整路径由 `parentPath + name` 组合。
- 时间字段可空；Repository 层解析成 `Instant` 或 `kotlinx.datetime.Instant`，解析失败时使用空值/兜底显示，UI 层按 Locale 格式化。
- `size = 0` 且非目录时按“未知大小”显示。

DTO 映射示例：

```kotlin
fun AlistFileDto.toFileItem(parentPath: String, baseUrl: String): FileItem {
    val normalizedParent = parentPath.trimEnd('/')
    val fullPath = if (normalizedParent.isEmpty()) "/$name" else "$normalizedParent/$name"
    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .takeIf { it.isNotBlank() && it != name.lowercase() }

    return FileItem(
        name = name,
        path = fullPath,
        isDir = isDir || fileType == 0,
        size = size,
        modifiedAt = modified?.let { runCatching { Instant.parse(it) }.getOrNull() },
        extension = extension,
        type = inferFileType(extension, isDir || fileType == 0),
        thumbnailUrl = thumb?.let { "$baseUrl/p/$fullPath?sign=$it" },
        downloadUrl = sign?.let { "$baseUrl/d/$fullPath?sign=$it" },
    )
}
```

### 4.12 关键依赖版本（2026-06）

Phase 0 使用 Gradle Version Catalog。Kotlin 2.0+ 使用 `org.jetbrains.kotlin.plugin.compose` 管理 Compose Compiler，版本与 Kotlin 一致；不要再配置 `composeOptions.kotlinCompilerExtensionVersion`。

```toml
# gradle/libs.versions.toml
[versions]
kotlin = "2.0.21"
ksp = "2.0.21-1.0.27"
agp = "8.7.2"
compose-bom = "2024.09.03"
hilt = "2.52"
hilt-navigation-compose = "1.2.0"
retrofit = "2.11.0"
retrofit-kotlinx-serialization = "1.0.0"
okhttp = "4.12.0"
kotlinx-serialization = "1.7.3"
kotlinx-coroutines = "1.9.0"
kotlinx-datetime = "0.6.1"
room = "2.6.1"
coil = "3.0.4"
navigation-compose = "2.8.3"
security-crypto = "1.1.0-alpha06"
material3 = "1.3.0"
lifecycle = "2.8.6"
activity-compose = "1.9.3"
work-runtime = "2.9.1" # 预留，Phase 0 暂不引入
junit = "4.13.2"
mockk = "1.13.13"
turbine = "1.2.0"
robolectric = "4.13"
androidx-test-ext = "1.2.1"
espresso = "3.6.1"
mockwebserver = "4.12.0"

[libraries]
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigation-compose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-navigation-compose" }
retrofit-core = { group = "com.squareup.retrofit2", name = "retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { group = "com.jakewharton.retrofit", name = "retrofit2-kotlinx-serialization-converter", version.ref = "retrofit-kotlinx-serialization" }
okhttp-core = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-logging = { group = "com.squareup.okhttp3", name = "logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
kotlinx-datetime = { group = "org.jetbrains.kotlinx", name = "kotlinx-datetime", version.ref = "kotlinx-datetime" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
coil-network-okhttp = { group = "io.coil-kt.coil3", name = "coil-network-okhttp", version.ref = "coil" }
security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "security-crypto" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
mockk-android = { group = "io.mockk", name = "mockk-android", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-test-ext" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockwebserver" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

注意：

- Kotlin 2.0+ 用 KSP 处理 Room/Hilt，不使用 KAPT。
- Coil 3.x 坐标为 `io.coil-kt.coil3`。
- `security-crypto = 1.1.0-alpha06` 是第一版选定版本但不是 stable；若 Gradle sync 或 API 稳定性有问题，替换为等价 Keystore-backed `CredentialStore`，业务层不感知。
- `work-runtime` 只作为后续可靠后台传输预留，Phase 0 不引入。

---

## 5. 核心数据流与 API 交互

### 5.1 API 基础约定

服务器地址规范化：

- 自动 trim 空格
- 无 scheme 默认补 `http://`
- 保留端口和路径
- baseUrl 统一以 `/` 结尾
- `http://` 在登录页实时提示明文风险
- 不允许空地址、非法 URL

登录失败区分：连接失败 / 认证失败 / 业务失败（Alist 返回 `code != 200`）。所有 API 返回统一转换为 `ApiResult`，UI 层只看业务语义。

### 5.2 上传数据流

```
FileScreen 点击上传
  → ActivityResultContracts.GetContent()（不支持文件夹上传）
  → 用户选择一个或多个文件
  → TransferManager.enqueueUpload(uri, targetPath)
```

Uri 文件名提取（不能直接用 `Uri.toString()`）：

```
DISPLAY_NAME → uri.lastPathSegment → upload-{timestamp}
```

上传中 401 不走 Authenticator 自动重试（Interceptor 标记 skip-auth-retry）：

```
上传中收到 401
  → Authenticator return null（不重试）
  → 上传 Call 以 401 失败
  → TransferManager 标记任务 Failed，文案"上传中断，请重试"
  → 用户点击重试 → 重新走 enqueueUpload（不做断点续传）
```

不提“登录失效”，避免用户误以为是密码错。

任务流程：创建 Room 任务记录 Waiting → Semaphore(upload=2) 取许可 → Uploading → OkHttp 上传（主方案 `PUT /api/fs/put`；目标版本支持时可用 `/api/fs/form` 兼容）→ 进度回调更新 Room + UI + 通知栏 → Success / Failed / Cancelled。

### 5.3 下载数据流

```
FileScreen 点击下载 → TransferManager.enqueueDownload(filePath)
→ FileRepository.getDownloadUrl(filePath) → OkHttp 下载
→ 保存到 filesDir/downloads/{sha1(path)}.ext → Room 更新任务状态
```

第一版固定存 App 私有目录，通过 FileProvider 打开/分享；后续再加“保存到公共 Downloads”。

任务流程：Room Waiting → Semaphore(download=3) → Downloading → 进度回调 → Success(localFileUri) / Failed / Cancelled。

App 重启：读取 Room，`Waiting / Uploading / Downloading` → 标记 Interrupted；`Success / Failed / Cancelled` 保持原状态。

#### 文件大小未知边界

- `FileItem.size == 0L` 且 `isDir == false` → UI 显示“未知大小”
- OkHttp 下载 `contentLength() == -1` → 进度条 indeterminate，不显示百分比

#### 下载成功入口

下载成功后 Snackbar（不弹 Toast）：

- 主文案：`{filename} 已下载`
- 操作按钮：`打开` / `分享`

### 5.4 文件列表数据流

```
FileScreen 打开 path
  → FileViewModel.load(path)
  → FileRepository.list(path)
  → AlistApi.fsList(path)
  → ApiResult<List<FileItem>>
```

FileItem 字段：name、path、isDir、size、modifiedAt、extension、type、thumbnailUrl?。

列表展示：文件夹排前、默认按名称排序、支持按名称/大小/更新时间排序、图片走缩略图、失败回退默认图标、下拉刷新重新请求。

#### 请求取消

```kotlin
private val _path = MutableStateFlow("/")
val state: StateFlow<FileUiState> = _path
    .flatMapLatest { path -> fileRepository.listFlow(path) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FileUiState.Loading("/"))
```

#### 搜索防抖

```kotlin
searchQuery.debounce(300).distinctUntilChanged()
```

同 flatMapLatest 模式，旧请求自动取消。

### 5.5 文件操作数据流

- 新建文件夹：输入名称 → mkdir → 刷新
- 重命名：输入新名 → rename → 刷新
- 删除：选择 → 二次确认 → delete(paths) → 刷新，失败 Snackbar
- 复制/移动：选择 → MoveCopyTargetPickerScreen → 选目标 → 服务端 copy/move API → 刷新源目录（必要时刷新目标）

#### 复制/移动多文件策略

Alist v3 copy/move 一次只支持一个文件，多文件串行 + 任一失败中止：

```kotlin
suspend fun copyMultiple(srcPaths: List<String>, targetDir: String): CopyResult {
    var successCount = 0
    for ((index, src) in srcPaths.withIndex()) {
        val result = fileRepository.copy(src, targetDir)
        if (result is ApiResult.Failure) {
            return CopyResult(total = srcPaths.size, success = successCount,
                failed = srcPaths.size - successCount,
                firstFailure = src to result.message, stopped = true)
        }
        successCount++
        onProgress(index + 1, srcPaths.size)  // UI 显示 "3/5"
    }
    return CopyResult(success = successCount, failed = 0, stopped = false)
}
```

失败 Snackbar：`已复制 3 个，第 4 个失败：{message}，已复制的内容已保留`。不做“全部尝试最后汇总”。

### 5.6 预览数据流

```
用户点击文件 → PreviewRouter 按类型判断
图片      → 缩略图/下载链接展示，必要时下载临时文件
文本      → 下载或读取远程内容，默认 ≤ 2MB，超限提示“下载 / 外部打开”
音视频    → 获取可播放 URL，App 内播放器；不可用则下载临时文件或系统播放器
其他      → 下载临时文件 → FileProvider uri → Intent ACTION_VIEW
```

临时文件由设置页“清理临时预览文件”统一清理；不做自动清理，避免误清用户正在看的；只在设置页手动清，系统空间不足时系统自动清 `cacheDir`。

### 5.7 分享数据流

- 系统分享：确保本地有可分享文件（已下载/临时缓存，未下载则先下载）→ FileProvider uri + `ACTION_SEND`，带 `FLAG_GRANT_READ_URI_PERMISSION`
- 链接分享：`getDownloadUrl(path)` 或拼接链接 → 复制剪贴板 / `ACTION_SEND` text/plain，文案提示“此链接需要登录 Alist 账号才能访问。如果服务器在内网，对方可能无法打开。”（不提“有效期”）

---

## 6. 错误处理、权限、安全与边界

### 6.1 统一错误模型

```kotlin
sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object ServerUnreachable : AppError
    data object NotAlistServer : AppError
    data object Unauthorized : AppError
    data object PermissionDenied : AppError
    data object NotFound : AppError
    data object Conflict : AppError
    data object Timeout : AppError
    data object SSLError : AppError              // TLS 握手层
    data object CertificateUntrusted : AppError  // 证书过期/自签/主机名不匹配
    data object Cancelled : AppError
    data class OperationFailed(val message: String) : AppError
    data class Unknown(val cause: Throwable? = null) : AppError
}
```

### 6.2 ErrorMessageMapper（统一文案收口）

```kotlin
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
        AppError.Cancelled -> ""  // 不展示
        is AppError.OperationFailed -> error.message
        is AppError.Unknown -> "出错了，请重试"
    }

    fun toActionLabel(error: AppError): String? = when (error) {
        AppError.NetworkUnavailable, AppError.ServerUnreachable,
        AppError.Timeout, AppError.NotFound, AppError.SSLError,
        AppError.CertificateUntrusted -> "重试"
        AppError.Unauthorized -> "重新登录"
        AppError.Cancelled -> null
        else -> null
    }

    fun isRetryable(error: AppError): Boolean = when (error) {
        AppError.NetworkUnavailable, AppError.ServerUnreachable,
        AppError.Timeout, AppError.NotFound, AppError.Conflict -> true
        else -> false
    }
}
```

### 6.3 Throwable / Alist 失败码映射

```kotlin
fun mapThrowable(t: Throwable): AppError = when (t) {
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
        serverMessage?.takeIf { it.isNotBlank() } ?: "服务器错误 ($code)，请稍后重试")
    else -> AppError.OperationFailed(
        serverMessage?.takeIf { it.isNotBlank() } ?: "操作失败 ($code)")
}
```

规则：优先用 Alist message → 服务端 message 为空用客户端本地化文案 → 绝不展示英文堆栈。

### 6.4 NotAlistServer 探测

登录时探测服务端类型并存 SessionManager：响应含 `alist` 或 `"code":200` → ALIST；含 `<html` / `<!DOCTYPE` → NOT_ALIST；否则 UNKNOWN。文件请求响应非 Alist JSON 时标记 `NotAlistServer`。

### 6.5 登录错误提示

| 错误 | 文案 |
|------|------|
| `ServerUnreachable` | 无法连接服务器，请检查地址和网络 |
| `Timeout` | 连接超时，请检查网络或服务器状态 |
| `SSLError` | TLS 握手失败，服务器证书可能不受信任 |
| `CertificateUntrusted` | 服务器证书不受信任（过期/自签/主机名不匹配） |
| `NotAlistServer` | 该地址不是 Alist 服务，请确认服务是否运行 |
| `Unauthorized` | 用户名或密码错误 |
| `OperationFailed` | Alist 返回的 message |

HTTP 明文警告前移到 serverUrl 输入框下方**实时**显示，仅当用户实际填了 http 地址才显示。

### 6.6 SDK 与权限

```kotlin
android {
    compileSdk = 34
    defaultConfig {
        minSdk = 26       // Android 8.0
        targetSdk = 34    // Android 14
    }
}
```

权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />  <!-- API 33+ 运行时申请 -->
```

`POST_NOTIFICATIONS` 仅 API 33+ 申请；用户拒绝 → 传输仍可在前台进行，只是不显示通知，不阻塞功能。

不申请 `READ/WRITE_EXTERNAL_STORAGE`、`MANAGE_EXTERNAL_STORAGE`、`READ_MEDIA_*`（上传走系统选择器，下载走 App 私有目录，预览/分享走 FileProvider）。

### 6.7 备份排除清单

`res/xml/backup_rules.xml` 与 `res/xml/data_extraction_rules.xml` 排除：

- `sharedpref` `secure_prefs.xml`（Keystore 主密钥设备绑定，恢复后解密失败）
- `database` `transfer_tasks.db` / `-shm` / `-wal`
- `file` `downloads/`（用户下载大文件不应自动云备份）
- `file` `preview/`

AndroidManifest 同步：

```xml
<application
    android:fullBackupContent="@xml/backup_rules"
    android:dataExtractionRules="@xml/data_extraction_rules" ...>
```

### 6.8 FileProvider

`res/xml/file_paths.xml` 只暴露子目录，不暴露根：

```xml
<paths>
    <files-path name="downloads" path="downloads/"/>
    <cache-path name="preview" path="preview/"/>
</paths>
```

MIME type 兜底：

```kotlin
fun inferMimeType(name: String): String =
    MimeTypeMap.getSingleton().getMimeTypeFromExtension(
        name.substringAfterLast('.', "").lowercase())
        ?: "application/octet-stream"
```

Intent flags：

```kotlin
val intent = Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, mimeType)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
```

### 6.9 文件名校验

```kotlin
fun isValidFileName(name: String): Boolean {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed in setOf(".", "..")) return false
    if (trimmed.any { it == '/' || it == '\\' || it.isISOControl() }) return false
    if (trimmed.length > 255) return false
    return true
}

fun fileNameErrorMessage(name: String): String? {
    if (name.isBlank()) return "名称不能为空"
    if (name.trim() in setOf(".", "..")) return "名称不合法"
    if (name.contains('/') || name.contains('\\')) return "名称不能包含 / 或 \\"
    if (name.any { it.isISOControl() }) return "名称包含非法字符"
    if (name.length > 255) return "名称过长（最多 255 字符）"
    return null
}
```

不合法时输入框下方红字提示具体原因。

### 6.10 网络与超时

```
connectTimeout: 15s
readTimeout: 60s
writeTimeout: 60s
callTimeout: 不对传输任务设置全局短超时
```

- HTTPS 证书错误不自动忽略，不提供“跳过证书校验”
- 自签证书不支持（第一版）
- 网络断开后任务失败，用户点击重试重新开始
- 不做断点续传、自动网络恢复后续传

### 6.11 进度条组件

```kotlin
@Composable
fun TransferProgress(bytesDone: Long, totalBytes: Long) {
    if (totalBytes > 0 && totalBytes > bytesDone) {
        val progress = (bytesDone.toFloat() / totalBytes).coerceIn(0f, 1f)
        LinearProgressIndicator(progress = { progress })
    } else {
        LinearProgressIndicator()  // indeterminate
    }
}
```

### 6.12 日志与崩溃脱敏

日志允许：接口名、错误类型、HTTP/Alist code、任务 ID、文件大小、耗时。禁止：密码、token、完整 Authorization header、完整下载/分享链接、可能含敏感目录的完整路径（日志只记文件名 + hash）。

`SafeCrashHandler` 全局 UncaughtExceptionHandler，写本地 crash log，脱敏 Bearer/password/token，不接 Crashlytics。

```kotlin
private fun sanitizeStackTrace(e: Throwable): String =
    StringWriter().also { e.printStackTrace(PrintWriter(it)) }.toString()
        .replace(Regex("Bearer [\\w\\-\\.]+"), "Bearer ***")
        .replace(Regex("password[\"']?\\s*[:=]\\s*[\"']?[\\w]+"), "password=***")
        .replace(Regex("token[\"']?\\s*[:=]\\s*[\"']?[\\w\\-\\.]+"), "token=***")
```

#### Crash log 目录与保留策略

```kotlin
val crashDir = File(context.filesDir, "crash_logs")
crashDir.mkdirs()

// 保留最近 10 个，超过自动清理最旧
crashDir.listFiles()
    ?.sortedByDescending { it.lastModified() }
    ?.drop(10)
    ?.forEach { it.delete() }
```

文件名：`crash-{timestamp}-{threadName}.log`。

设置页可加“导出崩溃日志”入口，通过系统分享面板分享给开发者。

### 6.13 网络监听（建议加）

`ConnectivityManager.NetworkCallback` 监听网络变化，`StateFlow<Boolean> isOnline`。离线时 FileScreen 顶部显示“当前无网络”，不阻塞操作，允许浏览 Room 缓存的任务列表。

### 6.14 平台边界

第一版只支持：竖屏手机、Android 8.0+（minSdk 26）、单账号、Alist v3、在线使用。

---

## 7. 传输任务状态机

| 状态 | 触发场景 | UI 行为 | 重试按钮 |
|------|----------|----------|----------|
| Waiting | 任务已创建，等待并发许可 | 排队中，可取消 | 无 |
| Uploading / Downloading | 正在传输 | 显示进度，可取消 | 无 |
| Success | 传输完成 | 下载显示成功 Snackbar（打开/分享） | 无 |
| Failed | 网络错误、code≠200、权限拒绝、401 中断上传 | 显示 message | 显示，文案“重试” |
| Cancelled | 用户主动取消 | 不显示重试 | 无 |
| Interrupted | 进程被杀、App 重启后发现未完成 | 显示“传输中断” | 显示，文案“重新传输” |

App 重启时 `Waiting / Uploading / Downloading` → Interrupted。

---

## 8. 通知策略

通知渠道：

```
transfer_progress_channel  IMPORTANCE_LOW
transfer_result_channel    IMPORTANCE_DEFAULT
```

- 进行中：低优先级、无声音、无震动，前台也显示
- 失败：默认优先级，可响一声，带“重试”按钮
- 成功：低优先级，短暂显示后自动清除
- 取消：不通知，立刻清理
- 多任务并发优先一条汇总通知，避免刷屏（如“正在下载 3 个文件（总进度 65%）”，下拉展开看单个任务）
- 拒绝通知权限时，App 内传输页仍完整显示状态

---

## 9. 验收场景

### 9.1 登录

1. 正确 Alist v3 地址 + 账号密码可登录
2. 地址缺 scheme 自动补全
3. HTTP 地址实时显示明文警告
4. 错误密码显示“用户名或密码错误”
5. 不可达服务器显示“无法连接服务器”
6. 非 Alist 服务显示“该地址不是 Alist 服务”
7. HTTPS 证书错误显示证书相关文案
8. App 重启后自动进入文件页
9. token 失效可自动重登（debug 入口强制过期 token 触发）
10. 自动重登失败回到登录页

### 9.2 文件列表

1. 根目录可加载
2. 子目录可进入
3. 面包屑可返回上级
4. 下拉刷新可重新请求
5. 文件夹排前
6. 可按名称、大小、更新时间排序
7. 图片显示缩略图，失败回退类型图标
8. 非图片显示默认图标
9. `size = 0` 的非目录显示“未知大小”
10. 快速切换目录不会旧请求覆盖新页面
11. 网络失败显示兜底 UI 和重试按钮

### 9.3 搜索

1. 输入关键字 300ms 后开始搜索
2. 连续输入时旧请求取消
3. 空关键字恢复当前目录列表
4. 后端搜索失败降级当前目录过滤
5. 当前目录过滤结果明确标识
6. 搜索失败显示可理解错误

### 9.4 文件操作

1. 新建文件夹成功后刷新列表
2. 空文件名、`.`、`..`、含 `/`、`\`、控制字符、过长名称均拦截并显示具体原因
3. 重命名成功后刷新列表
4. 删除前二次确认
5. 多选删除显示数量
6. 删除失败显示服务端 message
7. 复制/移动单文件走 Alist 服务端 API
8. 复制/移动多文件串行显示进度
9. 多文件中途失败中止，提示已完成数量
10. 复制/移动不经本机下载上传

### 9.5 上传

1. 点击上传打开系统文件选择器
2. 可选择一个或多个文件
3. 文件名按 `DISPLAY_NAME → lastPathSegment → 默认名` 提取
4. 上传任务进入传输页
5. 上传并发最多 2
6. 显示进度、速度、状态
7. 支持取消
8. 失败后可重试
9. 上传中 401 标记失败，提示“上传中断，请重试”
10. 同名冲突由 Alist 返回，任务失败提示用户改名
11. 不支持文件夹上传
12. 不支持从其他 App 分享到本 App

### 9.6 下载

1. 点击下载创建下载任务
2. 下载并发最多 3
3. 下载到 `filesDir/downloads/`
4. 本地文件名使用 `sha1(path).ext`，不同路径同名不覆盖
5. Room 记录原始路径和本地文件
6. 支持进度、速度、状态
7. `contentLength = -1` 时显示不确定进度
8. 支持取消
9. 失败后可重试
10. 成功后 Snackbar 显示“已下载”，带“打开 / 分享”
11. 取消后不显示重试
12. App 重启后未完成任务标记为 Interrupted

### 9.7 传输页

1. Room 持久化任务列表
2. App 重启后任务列表仍存在
3. Waiting/Uploading/Downloading 重启后变为 Interrupted
4. Failed 显示“重试”
5. Interrupted 显示“重新传输”
6. Cancelled 不显示重试
7. Success 可打开/分享下载文件
8. 多任务通知合并
9. 进行中通知低优先级
10. 失败通知默认优先级并带重试
11. 拒绝通知权限后 App 内传输仍正常

#### 9.7.1 进程被杀验收步骤

```
1. 启动下载一个 ≥ 100MB 文件，等进度到 50% 左右
2. 触发进程被杀（三选一）：
   a. adb shell am kill com.example.alist
   b. 系统设置 → 应用 → Alist → 强制停止
   c. 开发者选项 → 打开“不保留后台活动”，切到桌面再回来
3. 重新打开 App
4. 验证：状态 = Interrupted；显示“传输中断”；显示“重新传输”；点击后从 0% 重新开始
```

不要在 IDE 里直接 Stop Process（不是真实杀进程路径）。

#### 9.7.2 退出登录副作用清单

| 数据 | 退出登录时 |
|------|------------|
| EncryptedSharedPreferences token | 清空 |
| EncryptedSharedPreferences password | 清空 |
| EncryptedSharedPreferences serverUrl/username | 清空 |
| 进行中的 OkHttp Call | cancel |
| TransferManager 队列 | 清空 |
| Room transfer_tasks 表 | 全删 |
| filesDir/downloads/ | 保留（用户资产） |
| cacheDir/preview/ | 清空 |
| 网络监听 | 继续（下次启动恢复） |

验收：登录 → 启动 2 个下载任务 → 退出登录 → 跳回登录页（无凭据预填）；传输页所有任务消失；`filesDir/downloads/` 已下载文件保留；`cacheDir/preview/` 已清空；加密 prefs 中 token/password 字段为空。

### 9.8 预览

1. 图片可 App 内预览
2. 文本 ≤ 2MB 可 App 内预览
3. 文本 > 2MB 提示下载或外部打开
4. 音视频优先尝试 App 内或系统播放器
5. 其他文件通过系统应用打开
6. 受保护文件通过 token 下载到临时文件
7. FileProvider 只暴露允许目录
8. 未安装可打开应用时显示提示
9. 设置页可清理 `cacheDir/preview/`

### 9.9 分享

1. 已下载文件可系统分享
2. 未下载文件分享时先下载临时文件
3. 分享 Intent 带只读 Uri 权限
4. 链接分享文案：“此链接需要登录 Alist 账号才能访问。如果服务器在内网，对方可能无法打开。”
5. 链接可复制到剪贴板
6. 链接可通过系统分享面板分享为文本

### 9.10 安全与隐私

1. 密码和 token 存在加密 prefs
2. 加密 prefs 不被备份
3. Room 和下载文件不被云备份
4. 日志不含密码、token、Authorization header
5. crash log 脱敏
6. HTTPS 证书错误不允许绕过
7. HTTP 明文连接有提示
8. FileProvider 不暴露根目录

#### Auto Backup 验证

测试步骤：

1. 设置 → Google → 备份 → 启用 Google Drive 备份
2. `adb shell bmgr backupnow com.example.alist`
3. `adb shell bmgr restore com.example.alist`（可选，验证排除配置）
4. 卸载并重新安装 App
5. 验证：
   - 登录页无凭据预填
   - `filesDir/downloads/` 为空
   - Room `transfer_tasks` 表为空
   - `cacheDir/preview/` 为空

---

## 10. 测试策略

### 10.1 弱网/断网测试

| 工具 | 用途 | 阶段 |
|------|------|------|
| Android Studio Profiler → Network Throttling | 开发自测 | Phase 1-4 |
| `adb shell tc qdisc ...` | 模拟器真实弱网 | Phase 5 |
| 飞行模式 / WiFi 切换 | 断网恢复 | Phase 5 |

```bash
# 加 500ms 延迟 + 10% 丢包
adb shell tc qdisc add dev wlan0 root netem delay 500ms loss 10%
# 恢复
adb shell tc qdisc del dev wlan0 root
```

| 场景 | 预期 |
|------|------|
| 500ms 延迟 + 5% 丢包 | 列表、搜索、上传、下载均能完成，不卡死 |
| 飞行模式 | 操作立即失败，显示“网络不可用” |
| 飞行模式 → 恢复 | 不自动重试，用户手动点重试可成功 |
| WiFi → 4G 切换 | 传输任务 Failed，提示用户重试 |

### 10.2 大文件测试

- 测试文件：单个 500MB（覆盖 WiFi/4G 切换）；不测 1GB+（无断点续传，中断重传体验差，易触发 LMK）
- 验证：SHA256 与本地原文件一致；进度持续更新不卡 0%/99%；速度在 0.1~50 MB/s 合理区间；通知栏与 App 内进度同步；用其他 App 打开下载文件不报错

### 10.3 性能基准（Logcat 打点）

| 指标 | 目标 |
|------|------|
| 冷启动 → 文件页（已登录） | ≤ 2s |
| 列表加载（100 项） | ≤ 1s |
| 列表滑动 FPS | ≥ 55 |
| 上传/下载 UI 更新频率 | 100-500ms 一次 |

第一版不上 Macrobenchmark 自动化，用 Logcat 打点定位慢步骤。

### 10.4 兼容矩阵

| Android 版本 | API | 测试状态 | 备注 |
|--------------|-----|----------|------|
| 8.0 | 26 | 必测 | minSdk |
| 10 | 29 | 必测 | Scoped Storage 边界 |
| 12 | 31 | 必测 | 前台服务权限收紧 |
| 13 | 33 | 必测 | POST_NOTIFICATIONS 运行时申请 |
| 14 | 34 | 必测 | targetSdk |

不测 Android 15+（API 35+）和国产厂商魔改。

### 10.5 测试分层

测试库选型：

- MockK：Kotlin 友好，支持 `coEvery` / `coVerify` / suspend fun。
- 不用 Mockito：MockK 对 Kotlin coroutine 支持更直接。
- 网络层用 `okhttp-mockwebserver` 模拟 Alist API 响应。
- Flow 测试用 `app.cash.turbine` 断言 StateFlow 时序。

- 单元测试：URL 规范化、文件名校验、AppError 映射、Alist failure code 映射、Uri 文件名提取、sha1 命名、传输状态转换、复制/移动中止策略、ErrorMessageMapper 文案
- ViewModel 测试：登录状态、文件列表 Loading/Success/Error、快速切目录取消、搜索 debounce、多选状态、传输任务状态更新
- Repository 测试（mock Alist API）：登录成功/失败、`code != 200` 业务失败、401 自动重登、list/search/delete/rename/copy/move、上传下载失败映射
- UI 测试：登录页输入、文件页加载成功、删除确认弹窗、传输页任务状态、设置页退出登录；补 HTTP 警告实时显示、文件名具体错误文案、离线兜底 UI
- monkey 测试（建议加）：`adb shell monkey -p com.example.alist --throttle 500 -v 1000`，验证无 ANR、无未捕获 crash、传输任务状态一致

---

## 11. 开发阶段划分

### Phase 0：工程骨架

Kotlin Android app + Compose/Material 3 + Hilt + Retrofit/OkHttp + Room + Coil 3 + EncryptedSharedPreferences + Navigation + 基础包结构 + minSdk 26/targetSdk 34 + backup/data extraction rules + FileProvider 配置 + 基础主题和底部导航。

验收硬指标：

```
./gradlew assembleDebug 成功（零编译错误）
./gradlew lint 成功（零 ERROR 级 issue）
./gradlew test 成功（空测试集也算）
APK 安装到 Android 8.0 模拟器，启动到登录页
Hilt 编译期验证通过（@Inject 都能 resolve）
```

### Phase 1：登录与会话

URL 规范化、HTTP 明文警告、登录接口、`ApiResult`、`AppError`、`ErrorMessageMapper`、SSL/网络/业务错误映射、Encrypted credentials、`SessionManager`、OkHttp token interceptor、401 Authenticator、自动重登、退出登录。

验收：正确账号可登录；错误账号明确提示；不可达服务器明确提示；App 重启自动恢复会话；token 失效可自动重登；退出登录清空凭据。

### Phase 2：文件浏览与基础操作

文件列表、路径导航、下拉刷新、排序、图片缩略图、默认图标、`FileUiState`、请求取消、搜索防抖、新建文件夹、重命名、删除、多选模式、文件名校验。

验收：目录浏览稳定；快速切换不错乱；缩略图正常加载或回退；搜索可用；基础 CRUD 可用；删除有确认；错误有提示。

### Phase 3a：上传/下载核心（必交付）

TransferManager 骨架、Room 任务表、单文件上传/下载、进度回调、任务状态机、文件名提取 / sha1 命名、`filesDir/downloads/` + FileProvider。

验收：上传下载可用；任务列表持久化；并发限制生效；取消/失败/中断/重试行为正确；下载成功可打开/分享。

### Phase 3b：复制/移动 + 传输增强（可延后）

复制/移动 API、DirectoryBrowser 复用、MoveCopyTargetPickerScreen、多文件串行、通知栏 + 通知合并、Interrupted 处理。

> 3a 完成即有最小可用传输；3b 是体验增强，时间不够可砍。

### Phase 4：预览、分享、设置完善

图片预览、文本预览、文本大小限制、音视频打开策略、其他文件外部打开、系统分享、链接分享、临时预览文件、清理临时文件、设置页完善、本地 crash log 脱敏、NetworkMonitor、细节文案打磨。

### Phase 5：集成测试与打包准备

真实 Alist v3 服务器联调、弱网/断网测试、错误账号测试、证书错误测试、大文件测试、进程被杀恢复测试、通知权限拒绝测试、多 Android 版本测试、monkey 测试、README 使用说明、Debug APK 构建说明。

---

## 12. README 已知限制模板

```markdown
## 已知限制

本应用为 MVP 第一版，以下功能明确不支持：

### 文件管理
- ❌ 后台可靠传输（切后台系统可能挂起）
- ❌ 断点续传（中断后需重传）
- ❌ 多账号切换
- ❌ 管理员面板
- ❌ 外部网盘挂载管理
- ❌ 离线目录缓存
- ❌ 回收站/撤销删除
- ❌ 复杂分享规则（有效期、密码、权限）

### 系统兼容
- ❌ 横屏/平板布局（仅竖屏手机）
- ❌ 自签 HTTPS 证书支持
- ❌ Alist v2 兼容
- ❌ 从其他 App 接收分享文件

### 性能边界
- 大文件 > 500MB 测试覆盖，> 1GB 未测试
- 同时并发：上传 2，下载 3，超出排队
- 缩略图缓存：内存 15-20% 堆，磁盘独立目录

### 安全边界
- HTTP 明文连接：允许但有提示
- HTTPS 证书错误：不允许绕过
- 凭据存储：EncryptedSharedPreferences + Keystore，已 root 设备无法保证安全
```

---

## 13. 关键决策汇总

| 问题 | 决策 |
|------|------|
| Interrupted vs Failed | 不合并 |
| 退出登录 downloads / preview | downloads 保留；preview 清空 |
| 上传中 401 | abort + 整段重传，不做自动重试 |
| 退出登录 Room 任务 | 全部删除 |
| 自动重登触发 | debug 入口强制过期 token |
| Phase 3 拆分 | 3a 上传/下载核心 + 3b 复制/移动增强 |
| 大文件测试 | 500MB + SHA256 |
| 文件选择器 | `ActivityResultContracts.GetContent()` |
| HTTP 警告位置 | 输入框下方实时显示 |
| 上传同名冲突 | 交给 Alist，客户端不重命名 |
| AppError 文案 | `ErrorMessageMapper` 统一 |
| SSL 错误 | 拆 `SSLError` + `CertificateUntrusted`，不提供跳过证书校验 |
| 复制/移动多文件 | 串行 + 任一失败中止 |
| POST_NOTIFICATIONS | 仅 API 33+ 申请，拒绝不阻塞 |
| minSdk / targetSdk | 26 / 34 |
| 并发 | 上传 2 / 下载 3，Semaphore 限流 |
| 下载位置 | `filesDir/downloads/{sha1(path)}.ext` + FileProvider |
| 进程被杀测试 | am kill / 强制停止 / 不保留后台 |
| Interrupted 重试按钮 | “重新传输” |
| Failed 重试按钮 | “重试” |
