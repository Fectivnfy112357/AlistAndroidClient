# 文件列表更多菜单与删除设计

日期：2026-06-29

## 背景

“我的文件”列表当前文件行右侧外露分享和下载按钮，文件夹行只有进入箭头。底层 `FileRepository.delete(paths)` 已支持调用 AList 删除接口，但 UI 和 `FileViewModel` 没有提供单项删除入口。

## 目标

- 文件行右侧保留外露 `下载` 按钮。
- 文件行新增 `更多` 菜单，包含：`分享链接`、`复制直链`、`删除`。
- `复制直链` 只对文件显示，且仅当 AList 返回的 `downloadUrl` 非空时显示。
- 文件夹行右侧显示 `更多` 菜单，包含：`删除`。
- 删除前弹确认框：`确定删除「xxx」吗？此操作不可恢复。`
- 删除成功后刷新当前目录。
- 删除失败不假删除，保留当前列表并显示错误。

## 非目标

- 不做批量删除。
- 不做长按菜单。
- 不做回收站。
- 不为缺失 `downloadUrl` 的文件自行拼接直链。

## 交互设计

文件行：

- 点击行主体：沿用当前预览行为。
- 点击下载：沿用当前下载行为。
- 点击更多：打开下拉菜单。
- 菜单项：
  - 分享链接
  - 复制直链（仅 `downloadUrl` 非空时显示）
  - 删除

文件夹行：

- 点击行主体：进入文件夹。
- 点击更多：打开下拉菜单。
- 菜单项：
  - 删除

删除确认：

- 标题：`删除文件` 或 `删除文件夹`
- 正文：`确定删除「xxx」吗？此操作不可恢复。`
- 确认按钮：`删除`
- 取消按钮：`取消`

复制直链：

- 使用 Android Clipboard 将 `item.downloadUrl` 写入剪贴板。
- 成功后 Toast：`直链已复制`。

## 实现设计

- 扩展 `FileRepositoryContract`，增加 `delete(paths: List<String>): ApiResult<Unit>`。
- `FileRepository` 已有 `delete(paths)`，改为实现 contract。
- `FileViewModel` 增加 `delete(item: FileItem)`：调用 repository 删除单项路径；成功后刷新当前目录；失败时将 UI 切到 Error 或保留当前列表并提供错误提示，优先复用现有 `ErrorMapper`。
- `FileScreen` 为 `FileRow` 增加 `onDelete`、`onShare`、`onCopyDirectLink` 回调。
- `FileRow` 用 Material `DropdownMenu` / `DropdownMenuItem` 实现更多菜单，用 `AlertDialog` 实现删除确认。

## 测试设计

- `FileScreenSourceTest` 验证文件行使用更多菜单、包含删除确认、复制直链只依赖 `downloadUrl`。
- `FileViewModelTest` 验证删除成功后调用 repository 并刷新列表。
- `FileViewModelTest` 验证删除失败时不会从当前列表假删除。
