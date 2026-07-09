# UI Expressive 计划后续修复与关键偏离回填

> 状态: 设计阶段 — 等待用户 review
> 计划来源: `2026-07-08-ui-expressive-followups.md` 末尾"已知偏离 + pre-existing flaky"
> 关联进度账本: `.superpowers/sdd/progress.md` (行 162-170)

## 1. 背景

2026-07-08 完成 UI Expressive 重做 + followups 计划(32+18=50 个 task 全部 APPROVED)后,`.superpowers/sdd/progress.md` 末尾遗留两类未消项:

### 1.1 关键偏离 (实施中决策, 4 项)

| ID | 偏离 | 当前状态 |
|----|------|---------|
| P1-5 | 用 SessionEventBus 替代 brief 中 AuthRepository 持 flow, 修复首版 401 抢跑 emit | **代码已修, 文档化不足** |
| P1-6 | 仅删 9 组件文件 + 18 颜色 val (非 brief 的 11+5); CloudShapes/CloudMotion/CloudTypography 保留 | **生产引用未替换为新 token** |
| P2-18 | 非过时 `@RequiresApi` 注解, 是 `MediaStore.Downloads` API 29+ 静默失败; 加 SDK<Q 守卫 | **代码已修, 跟脚说明缺失** |
| Plan 签名 | Plan 多次猜测签名被 implementer 纠正 (HyperOsMotion→CloudMotion, onMoreClick→trailing slot, FileType→FileCategory, Preview mime→fileTypeName) | **无偏差记录模板, 易重蹈** |

### 1.2 Pre-existing flaky (2 项)

| 测试 | 失败模式 | 根因假设 |
|------|---------|---------|
| `TransferManagerTest.deleteCancelsActiveTransferAndRemovesRecord` | 全套跑 order-dependent, 隔离跑通过 | `TransferManager.scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` 无法注入, 测试用 `runBlocking` 启动后台 IO 协程与 Robolectric 主线程/其他测试静态状态竞争 |
| `HomeRepositoryTest.retrySectionRefetchesOnlyThatSection` | 全套跑 order-dependent, 隔离跑通过 | 用 `server.enqueue` 显式入队 13 个响应 + `server.requestCount` 断言; `runTest` 默认 StandardTestDispatcher 与 AdminRepository 内部 `withContext(IoDispatcher)` 交错时机不稳定 |

## 2. 目标

1. 消除 2 项 pre-existing flaky (100 次连跑 0 失败)
2. 把 P1-6 中 4 个生产引用文件改为新 token, 让 CloudShapes/Motion/Typography 退化为纯演示别名
3. 把 P1-5 + P2-18 的偏离补全到 progress.md, 加 KDoc 防回归
4. 引入 `deviation-template.md`, 回填 4 项已知纠正记录, 防止未来 plan/implementer 脱节

## 3. 范围 (In Scope)

### 3.1 测试基础设施 (Task 1-4)

- **Task 1**: TransferManager 抽 `interface TransferExecutor` + `RealTransferExecutor`(生产, 当前 IO 行为) + `FakeTransferExecutor`(测试, 同 TestScope)
- **Task 1 衍生**: TransferManager.scope 改为构造注入 `CoroutineScope` (默认 `@Singleton` 用 `SupervisorJob + Dispatchers.IO`, 测试用 `TestScope`)
- **Task 2**: `TransferManagerTest.deleteCancelsActive...` 改用 `FakeExecutor` + `TestScope`, 不再依赖 Robolectric IO 线程或 `BlockingOkHttpClient.execute()` 死循环
- **Task 3**: `HomeRepositoryTest.retrySectionRefetchesOnlyThatSection` 改用 `MockWebServer.takeRequest(timeout)` 路径断言, 显式 `runTest(UnconfinedTestDispatcher())`, `requestCount` 改为路径精确匹配
- **Task 4**: 全套 `:app:testDebugUnitTest` + `:app:lintDebug` 跑通; flaky 测试手动连跑 100 次 0 失败

### 3.2 关键偏离代码层清理 (Task 5-6)

#### Task 5 — P1-6 进一步: CloudShapes / CloudMotion 替换为新 token

**当前生产引用清单** (grep 验证):
- `app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt:77,148,195,202` — 4 处 `CloudShapes.Control`
- `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt:17,19,21` — 3 处 `CloudMotion.{DurationMediumMillis, FloatTween, OffsetTween}`
- `app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt:74,76` — 2 处 `CloudMotion.{PressedScale, RestScale, DurationShortMillis}` (Motion.kt 内部自引用, 改用字面量或提取私有 const)
- `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferProgress.kt:20` — 1 处 `CloudMotion.SpringFast`

**目标引用清单** (替换为):
- `AppShapes.Control` (Task 5 已建, 走 `MaterialTheme.shapes` 或显式 import)
- `AppMotion` 新建 `app/src/main/java/com/textvision/alistclient/ui/theme/Motion.kt` 同文件中新增 `object AppMotion`, 把所有常量从 CloudMotion 迁移
- 保留 `CloudMotion` 与 `CloudShapes` 作为 `@Deprecated` 转发对象 (无 CloudTypography 引用, 直接删)

**最终状态**:
- `CloudShapes` → `@Deprecated("Use AppShapes via MaterialTheme.shapes") object CloudShapes : AppShapes` 或纯 typealias
- `CloudMotion` → `@Deprecated("Use AppMotion") object CloudMotion : AppMotion` (Kotlin 无 object 继承, 改为 `typealias`)
- `CloudTypography` → 删除 (0 引用, 仅 Type.kt 自留 alias)

#### Task 6 — P2-18 跟脚

- `TransferManager.runDownload` (line 219-260) KDoc 顶部补:
  ```
  SDK<Q 守卫存在的原因: MediaStore.Downloads.EXTERNAL_CONTENT_URI + IS_PENDING 模式
  在 API 26-28 上 insert() 返回 null 或静默失败, 与 @RequiresApi 注解无关.
  删守卫前请先确认 minSdk 提升到 29 或实现非 MediaStore 下载路径.
  ```
- `TransferManager.createDownloadUri` (line 333-343) 与 `publishDownloadUri` (line 345-349) 的 `@RequiresApi(Q)` 注释保留, 加 "守卫见 runDownload"
- `app/lint-baseline.xml` 确认无 NewApi 残留 (Task 21 已删, 复检)

### 3.3 文档化补全 (Task 7-8)

#### Task 7 — progress.md P1-5 文档化补全

把 progress.md 行 130 末尾 P1-5 记录扩写:
```
Task 9+10+11 (P1-5): complete (commits 1319de7..36c0f9b, 4 commit 含 1 fix, review APPROVED after fix, 0 critical/0 important)
- **fix commit**: 36c0f9b (文件名带 fix 后缀)
- **首版 bug**: AuthInterceptor 任意 401 即 emit SessionEvent.Unauthorized, 导致 AdminRepository.refreshAndRetry 在 refresh 成功但 retry 仍 403 时抢跑 — 把"权限不足"误判为"会话失效"
- **修法**: AdminRepository.runAdmin 仅在 refreshAndRetry 自身失败时 emit; FileRepository.runAlistWithRefresh 因 first.code!=401 守卫 403 短路, 无需改
- **测试**: HomeRepositoryTest.admin401LeavesSectionsFailedAndPublicOk (正例: admin 401 应 Failed 但不触发 logout) + X (反例: 普通用户 401 应触发 SessionEvent.Unauthorized)
- **回归测试位置**: app/src/test/java/.../HomeRepositoryTest.kt:111 `admin401LeavesSectionsFailedAndPublicOk` (验证 admin 401 → 所有 admin section Failed 但 publicSection 仍 Ok, 且不触发 SessionGate.logout)
```

#### Task 8 — deviation-template.md + 回填

- 新建 `docs/superpowers/plans/_deviation-template.md`:
  ```markdown
  # Plan ↔ Implementer Deviation Log

  > 每次 task review 发现 plan 签名/常量名/参数与实现脱节时, 在此追加一条记录.

  ## 格式
  | task_id | plan 原写法 | implementer 实际写法 | 原因 |
  ```

- 在 `_deviation-template.md` 末尾追加 4 项已知纠正记录:
  1. Task 19 (P2-15): `PreviewDestArgs.mime:String` → 实为 `mime:String` 承载 `FileType.name` (待 Task 25 清理为真实 MIME), 后续 P2-15 已清
  2. Task 8 (Navigation): plan 提 `AppBottomNavItem` 包含 `onMoreClick` → 实为 M3 `ListItemRow.trailing` slot, 删除多余参数
  3. Task 17 (Spring): plan 提 `HyperOsMotion.SpringFast` → 实为 `CloudMotion.SpringFast`, `HyperOsMotion` 不存在
  4. Task 13 (DirectoryBrowser): plan 提 `fileCategoryFromMime(null, name)` → 实为 `toFileCategory()`, 与 FileScreen 一致

- `progress.md` 行 172 末尾追加指向 `_deviation-template.md` 的链接

## 4. 非目标 (Out of Scope)

- 不修其他 flaky (例如 `LocalDownloadNamer` 测试顺序敏感度, 暂未观察到)
- 不动 `FileRepository.runAlistWithRefresh` 后续 P1-5 follow-up (行 173 "重要后续: FileRepository 同形隐患 fs/list 若服务端返 401 (非 403) 仍有同 bug") — 留待 P3 plan
- 不改 `TransferManager` 的 semaphore 数 / 并发模型
- 不动 `progress.md` 的"Critical findings" / "P0 立即" / "P1 本季度" / "P2 后续" 历史章节

## 5. 设计要点

### 5.1 TransferExecutor 抽象

**行为等价约束**: TransferExecutor 抽象必须保留 TransferManager 现有的全部副作用语义:
- `isActive(generation, id)` 检查仍由 TransferManager 持有 (executor 仅接收 `isActive: suspend (Long) -> Boolean` 回调)
- `cancelNotified` / `clearGeneration` 仍由 TransferManager 持有, executor 不可见
- `progress.flush()` / `notificationController` 调用仍由 TransferManager 在 transfer 结束后触发
- `updateStatusUnlessCancelled` 调用由 TransferManager 在 `onProgress` 回调中调用, executor 不直接写 dao

这样 executor 只承载 IO (HTTP + ContentResolver), TransferManager 仍是状态机中枢, 重构后 TransferManagerTest 的 11 个测试无需改动, 仅 `deleteCancelsActive` 改用 FakeExecutor.

```kotlin
// app/src/main/java/com/textvision/alistclient/transfer/TransferExecutor.kt
interface TransferExecutor {
    suspend fun runDownload(
        id: String,
        remotePath: String,
        displayName: String,
        generation: Long,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend (Long) -> Boolean,
    ): TransferOutcome

    suspend fun runUpload(
        id: String,
        uri: Uri,
        targetPath: String,
        fileName: String,
        generation: Long,
        onProgress: (bytesDone: Long, totalBytes: Long) -> Unit,
        isActive: suspend (Long) -> Boolean,
    ): TransferOutcome
}

sealed interface TransferOutcome {
    data object Success : TransferOutcome
    data class Failed(val reason: String) : TransferOutcome
    data object Cancelled : TransferOutcome
}

@Singleton
class RealTransferExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val okHttpClient: OkHttpClient,
) : TransferExecutor { /* 当前 runDownload/runUpload 逻辑迁移 */ }

@Singleton
class TransferManager @Inject constructor(
    private val dao: TransferDao,
    private val executor: TransferExecutor,
    @ApplicationScope private val scope: CoroutineScope,
    private val notificationController: TransferNotificationController,
) {
    private val uploadSemaphore = Semaphore(2)
    private val downloadSemaphore = Semaphore(3)
    // ...
}
```

测试中:
```kotlin
class FakeTransferExecutor : TransferExecutor {
    val downloads = mutableListOf<DownloadRequest>()
    val uploads = mutableListOf<UploadRequest>()
    var nextOutcome: TransferOutcome = TransferOutcome.Success
    override suspend fun runDownload(...): TransferOutcome {
        downloads += DownloadRequest(...)
        // 模拟 IO 等待直到 cancelled
        while (!cancelled.get()) delay(10)
        return TransferOutcome.Cancelled
    }
}
```

### 5.2 HomeRepositoryTest 路径断言

替换 `server.requestCount` 为:
```kotlin
val request = server.takeRequest(2_000, TimeUnit.MILLISECONDS)
    ?: fail("Expected retry request within 2s")
assertTrue(request.path?.contains("/api/admin/storage/list") == true)
assertEquals("GET", request.method)
```

显式 `runTest(UnconfinedTestDispatcher())` 保证 AdminRepository 的 `withContext(IoDispatcher)` 立即执行。

### 5.3 CloudShapes/Motion 替换顺序

1. 新建 `AppShapes` (已存在, `app/src/main/java/com/textvision/alistclient/ui/theme/Shape.kt:23` 已有 `object CloudShapes`, 在同一文件加 `object AppShapes` 同内容)
2. 新建 `AppMotion` (同 Motion.kt)
3. 替换 4 个生产文件引用
4. `CloudShapes` / `CloudMotion` 改为 `@Deprecated("Use AppShapes/AppMotion") object CloudShapes` 纯转发 object (每个属性/常量显式 `= AppShapes.X`, 编译期可走; 运行时相同实例)
5. `CloudTypography` 删除 (Type.kt:121)
6. ThemePreviews.kt 文本改为 "AppShapes / AppMotion" + 对应引用

## 6. 风险

| 风险 | 概率 | 影响 | 缓解 |
|------|------|------|------|
| TransferExecutor 抽象遗漏细节, runDownload/runUpload 行为漂移 | 中 | 高 | 复用现有 11 个 TransferManagerTest 全绿作为回归网; Fake 与 Real 并存, 真实 IO 路径不变 |
| CloudShapes → AppShapes 替换后 Roborazzi baseline 失配 | 低 | 中 | baseline 仅含 6 视觉主题 demo, 形状 token 不直接影响文件图标; 跑 Roborazzi compare 验证 |
| deviation-template.md 未被未来 plan 引用 | 中 | 低 | 在 `_deviation-template.md` 顶部加 README 链接; progress.md 末尾加链接 |

## 7. 验证

- [ ] `:app:testDebugUnitTest` 全绿 (含 transfer/admin/auth/file/home/ui 各模块)
- [ ] `:app:lintDebug` 全绿 (baseline ≤ 当前 615 行)
- [ ] flaky 测试连跑 100 次 0 失败 (脚本 `for i in {1..100}; do ./gradlew :app:testDebugUnitTest --tests "*TransferManagerTest.deleteCancelsActive*" --tests "*HomeRepositoryTest.retrySectionRefetchesOnlyThatSection*" || exit 1; done`)
- [ ] `grep -rn "CloudShapes\.\|CloudMotion\.\|CloudTypography\." app/src/main` 仅 ThemePreviews.kt 演示文本 (≤ 4 行)
- [ ] `grep -rn "@Deprecated.*Cloud[A-Z]" app/src/main` 命中 CloudShapes/Motion 转发
- [ ] progress.md 行 130 末段含 "fix commit: 36c0f9b" 字样
- [ ] `_deviation-template.md` 含 4 项历史纠正记录

## 8. 任务拆分

| # | Task | 估时 | commit |
|---|------|------|--------|
| 1 | TransferManager 抽 TransferExecutor + scope 注入 | 0.5d | (任务实施时确定) |
| 2 | TransferManagerTest.deleteCancelsActive 改用 FakeExecutor + TestScope | 0.5d | (任务实施时确定) |
| 3 | HomeRepositoryTest.retrySection 用 takeRequest 路径断言 | 0.25d | (任务实施时确定) |
| 4 | 全套测试 + lint + flaky 连跑 100 次 | 0.25d | (任务实施时确定) |
| 5 | CloudShapes/Motion → AppShapes/Motion, CloudTypography 删 | 0.5d | (任务实施时确定) |
| 6 | TransferManager.runDownload KDoc 补全 + lint baseline 复检 | 0.25d | (任务实施时确定) |
| 7 | progress.md P1-5 文档化补全 | 0.1d | (任务实施时确定) |
| 8 | _deviation-template.md 创建 + 4 项回填 + progress.md 链接 | 0.25d | (任务实施时确定) |

**总估时**: ~2.6 人天 (1 个 sprint)

## 9. 关联文件

- `.superpowers/sdd/progress.md` 行 162-170 (本 spec 修复目标)
- `.superpowers/sdd/progress.md` 行 130 (P1-5 偏离详细化)
- `app/src/main/java/com/textvision/alistclient/transfer/TransferManager.kt:43-149` (Task 1 重构目标)
- `app/src/test/java/com/textvision/alistclient/transfer/TransferManagerTest.kt:132-146` (Task 2 重构目标)
- `app/src/test/java/com/textvision/alistclient/ui/feature/home/HomeRepositoryTest.kt:150-166` (Task 3 重构目标)
- `app/src/main/java/com/textvision/alistclient/admin/form/DynamicFormField.kt:77-202` (Task 5 替换目标)
- `app/src/main/java/com/textvision/alistclient/navigation/AppNavTransitions.kt:17-21` (Task 5 替换目标)
- `app/src/main/java/com/textvision/alistclient/ui/feature/transfer/TransferProgress.kt:20` (Task 5 替换目标)
- `app/src/main/java/com/textvision/alistclient/ui/theme/Type.kt:121` (Task 5 CloudTypography 删除目标)