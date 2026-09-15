# AGENTS.md

## 项目与会话

- 使用中文与用户沟通。
- 这是单模块 `:app` 的原生 Android 客户端（Kotlin、Jetpack Compose、Material 3），包名为 `com.textvision.alistclient`；主代码位于 `app/src/main/java/com/textvision/alistclient/`。
- 测试服务地址为 `http://textvision.top:5244`。认证信息不写入仓库文档；通过已配置的安全测试环境或向用户获取。
- 工作区可能已有用户或其他 Agent 的未提交修改。先检查范围，只修改本任务文件，保留无关改动。

## 开发环境与验证

每次运行 Gradle、ADB 或 Android SDK 命令前，先执行：

```bash
source tools/dev-env.sh
```

这会将 JDK 17 放到本会话 PATH 前端；系统环境中的 Java 8 会导致 AGP/Gradle 的 class-file 错误。不要把 `JAVA_HOME` 写入全局 shell 配置。

常用命令（仓库根目录）：

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

- Debug APK：`app/build/outputs/apk/debug/app-debug.apk`。
- Android Lint 是本项目唯一的静态检查；单元测试使用 JUnit 4、MockK、Turbine、MockWebServer 与 Coroutines Test。
- 完成实现前，运行与改动风险匹配的验证；涉及 UI/性能时至少构建、Lint、单元测试，并在真机或模拟器做实际交互回归。
- 不要给 `app/build.gradle.kts` 加项目级仓库；依赖和版本只在 `gradle/libs.versions.toml` 集中管理。

## 架构入口

- `AlistClientApp.kt`：Hilt 应用与崩溃处理。
- `MainActivity.kt`：唯一 Activity、`AppNavHost` 与 `TransferManager` 初始化。
- `navigation/AppNavHost.kt`、`AppRoute.kt`：路由和底部导航。
- `di/AppModule.kt`：Hilt 绑定与 `@IoDispatcher`。
- `common/result/ApiResult.kt`：统一网络结果模型。
- `transfer/TransferManager.kt`：上传下载协调器。

## Compose 与滚动性能

性能问题先建立与用户手势等价的复现，再修改。持续按住的反向拖动与“滑动、松手、立即反向再落指”的 fling 接管是不同路径，不能互相替代。

- 首页、音乐概览、设置页属于短内容的卡片型页面。快速交替 fling 容易反复触及上下边界；这些页面在各自 `LazyColumn` 范围内使用 `CompositionLocalProvider(LocalOverscrollConfiguration provides null)`，避免 stretch overscroll 先消费反向拖动。保留这个作用域：不要全局禁用，也不要把它扩展到歌曲、专辑、艺人或文件列表，除非有相同的真机证据。
- 出现“松手后立刻反向不跟手”时，优先判断是否为边界行为，而不是直接归因于图片、阴影或帧率。真机 Perfetto 应同时记录 `input`、`view`、`gfx`、`sched` 和应用 atrace；`adb input motionevent` 的输入延迟统计可能是注入时间戳伪影，不能单独作为根因。
- 列表热路径避免逐帧分配、字节数组内容哈希和同步图片解码。`ArtworkBitmapCache` 以对象身份和尺寸 bucket 作为 O(1) 键，并使用有界强引用 LRU，服务于快速反向滚动时的封面复用。
- `SectionCard` 的默认阴影为 `4.dp`。卡片密集但不需要浮起阴影的页面可显式传入 `shadowElevation = 0.dp`；不要修改默认值来影响全局视觉。
- 改动 Compose 状态、回调或列表项时，检查稳定性与重组边界；用 `rememberUpdatedState`/`remember` 固定必要的父级回调，避免无关状态使可见行重组。

## 编辑与提交前

- 非平凡功能先检查 `docs/superpowers/specs/` 与 `docs/superpowers/plans/` 中已有的规格和计划。
- 修改符号前运行项目可用的 GitNexus 影响分析；提交前运行变更检测。若当前环境未提供 GitNexus 工具，在交付中说明该边界。
- 使用可恢复、窄范围的 Git 操作；不要覆盖或清理未知的工作区改动。
