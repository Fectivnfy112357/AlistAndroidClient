# 传输页分组与永久删除设计

日期：2026-06-29

## 背景

当前传输页将上传和下载记录混合显示。`TransferEntity.type` 已经区分 `Upload` 与 `Download`，但 UI 没有按类型分开。传输记录目前只能通过全量清理路径删除，缺少单条永久删除能力。

## 目标

- 传输页顶部使用两个 Tab：`上传` 与 `下载`。
- 每个 Tab 一次只展示对应类型的传输记录。
- 每条传输记录提供明确的 `删除` 操作。
- 删除是数据库层面的永久删除，不是仅从页面隐藏。
- 删除进行中的任务时，先取消底层传输，再永久删除记录。

## 非目标

- 不做批量删除。
- 不做长按删除。
- 不改变上传、下载、重试、取消的既有语义。
- 不删除已下载到本地的文件，也不删除远端文件；只删除传输记录。

## UI 设计

`TransferScreen` 在标题/摘要下方增加两个 Tab：

- `上传`
- `下载`

默认选中 `上传`。每个 Tab 根据 `TransferEntity.type` 过滤当前列表。页面整体摘要继续统计全部传输记录，例如：`2 个进行中 · 1 个失败 · 1 个完成`。

空状态按当前 Tab 显示：

- 上传 Tab：`暂无上传任务`
- 下载 Tab：`暂无下载任务`

每条记录保留现有主操作：

- 进行中：`取消`
- 可重试：`重试` 或 `重新传输`
- 成功：显示 `完成`

同时新增次要操作 `删除`。点击删除时显示确认弹窗：

- 进行中任务：`删除后会取消当前传输，并永久删除这条记录。`
- 非进行中任务：`将永久删除这条传输记录。`

确认按钮执行永久删除；取消按钮关闭弹窗，不改变记录。

## 数据与业务逻辑设计

`TransferDao` 增加单条删除接口：

```kotlin
@Query("DELETE FROM transfer_tasks WHERE id = :id")
suspend fun deleteById(id: String)
```

`TransferManager` 增加：

```kotlin
fun delete(id: String)
```

删除语义：

1. 标记本地取消缓存，避免进行中的进度/状态回写继续生效。
2. 从 `activeCalls` 移除并取消网络请求。
3. 从 `activeJobs` 移除并取消协程任务。
4. 在 IO scope 中调用 `dao.deleteById(id)` 永久删除记录。

该方法不同于 `cancel(id)`：

- `cancel(id)` 将记录保留为 `Cancelled`。
- `delete(id)` 将记录从数据库删除。

`TransferViewModel` 增加：

```kotlin
fun delete(id: String) = manager.delete(id)
```

`TransferScreenContent` 接收 `onDelete` 回调，记录行负责弹确认弹窗并调用回调。

## 测试设计

新增或更新单元测试覆盖：

1. `TransferListUiState` 或等价过滤逻辑能按 `Upload/Download` 分开记录。
2. 上传 Tab 和下载 Tab 的空状态文案不同。
3. `TransferManager.delete(id)` 删除已结束记录时，数据库中记录消失。
4. `TransferManager.delete(id)` 删除进行中记录时，会取消 active call/job 并删除数据库记录。
5. 既有摘要统计仍基于全部记录，而不是当前 Tab。

## 风险与约束

- 进行中删除需要防止异步上传/下载任务在删除后再次写入状态或进度。通过取消 call/job 与 `cancelNotified` 缓存降低回写风险。
- Room Flow 会在删除后自动刷新 UI；UI 不维护额外隐藏列表，避免“页面删除但数据库仍存在”的假删除。
- 当前传输页文件较集中，本次只做与 Tab 和删除入口直接相关的局部调整。
