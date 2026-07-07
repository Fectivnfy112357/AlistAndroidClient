# Admin: 存储管理 + 站点设置集成

> 日期: 2026-07-07
> 状态: 待用户审阅

## 背景与目标

`SettingsScreen` 当前只承载"服务器信息 / 清理缓存 / 退出登录"，且底部横幅声明"多账号、管理员、外部网盘管理不在 MVP 范围内"。现需要把"管理当前 Alist 服务器"的能力纳入设置页：

1. **存储管理**：可查看、编辑（启用/停用 + 全部字段），**不可新增/不可删除**。
2. **Alist 设置**：在设置页集成 — 站点信息类的常用项直接可编辑，更完整/低频的设置项放到二级页。

设计原则：
- **复用优先**：Admin API 调用统一走 `AdminRepository`（从 `HomeRepository` 抽出 `runAdmin` + `refreshAndRetry` 模式），不再各 ViewModel 重复实现鉴权刷新。
- **动态渲染**：driver 与 settings 都按 Alist v3 后端返回的 `form_items` 动态生成表单，7 个基础字段类型 + 1 个降级（无 form_items 的项不展示）。
- **可逆风险**：编辑走二级页时显式"取消"按钮；行内启/停走 Switch 立即提交 + 失败回滚。

## 范围

### In scope
- 设置页"存储"区：列表 + 行内启/停 Switch
- 二级页 `StorageEditScreen`：单个存储的编辑（启/停 + driver 全部字段）
- 设置页"站点设置"区：4 个常用项（通过 setting key 过滤）直接编辑保存。**4 个常用项的 setting key**（v3 后端）：
  - `site_title` — 站点标题
  - `logo` — Logo URL
  - `login_background` — 登录页背景图
  - `announcement` — 登录页公告
  
  实施时若 v3 实际 key 名称不同（例如 `site_title` vs `title`），按**实际后端响应**调整过滤逻辑（以最终 listSettings 返回的 key 列表为准）。
- 二级页 `AdminSiteSettingsScreen`：所有带 `form_items` 的设置项（按 group 分组）
- 抽 `AdminRepository` 共享 admin API 模式
- 动态表单渲染器 `DynamicFormField`（driver/settings 共用）
- 单元测试

### Out of scope
- 新增/删除存储（按需求明确排除）
- 用户/角色/会话/任务管理（不纳入本次）
- 后端不返回 `form_items` 的设置项（**过滤不展示**）
- Alist v3 `setting` 中无 UI 定义的"原始字符串"项（如 aria2 各类 token、索引器 URL 等需手工输入大量字段的）
- 多账号切换、登录页深度定制

## 入口结构

```
SettingsScreen（垂直堆叠）
├─ 卡片 1：当前服务器
├─ 卡片 2：清理临时预览文件
├─ 卡片 3：退出登录
├─ 卡片 4：存储（新增）
│   ├─ 行：mount_path / driver / Switch(启停) / 点击进入 StorageEditScreen
│   └─ …
├─ 卡片 5：站点设置（新增）
│   ├─ 行：站点标题（点击行 → 编辑对话框/底部表单 → 保存）
│   ├─ 行：Logo URL（点击行 → 编辑）
│   ├─ 行：登录页背景图（点击行 → 编辑）
│   ├─ 行：登录页公告（点击行 → 编辑）
│   └─ 行：完整设置 → AdminSiteSettingsScreen
└─ 现有底部 Info 横幅移除（"不在 MVP 范围"已不再准确）
```

二级页路由：

| Route | 用途 |
|---|---|
| `admin/storage_edit/{id}` | 单个存储编辑 |
| `admin/site_settings` | 完整设置页 |

## 数据流

### AdminRepository（新建）

从 `HomeRepository` 抽出 `runAdmin`、`refreshAndRetry`、`safeCall`、`AdminResult` 四个内部 API 到 `AdminRepository`。`HomeRepository` 改为调用 `AdminRepository` 的公开方法。

公开 API：
- `suspend fun <T> runAdmin(base: String, call: suspend () -> AlistResponse<T>): AdminResult<T>`
- 后续存储 / 设置 ViewModel 复用

### StorageRepository（新建）

```
interface StorageRepository {
  suspend fun list(base: String): AdminResult<StorageList>
  suspend fun update(base: String, id: Long, patch: StoragePatch): AdminResult<Unit>
  suspend fun drivers(base: String): AdminResult<List<DriverInfo>>
}
```

`StoragePatch` 包含所有可写字段（id 必填；其余按 driver 的 form_items 携带）。提交策略：**全量提交** — 拉取原对象后合并改动，整体作为 body 提交给 `/api/admin/storage/update`。这样避免后端按字段增减带来的语义歧义。

### SettingsRepository（新建）

```
interface SettingsRepository {
  suspend fun listSettings(base: String): AdminResult<List<SettingGroup>>
  suspend fun saveSettings(base: String, patches: List<SettingPatch>): AdminResult<Unit>
}
```

Alist v3 `/api/admin/setting/list` 返回结构（**实施时需对照 v3 源码确认**——v3 不同小版本可能是 `List` 或 `Map`，本设计优先按 **List<SettingItem>** 实现，Map 情况由 Adapter 转换为 List 后处理）：
```
{ code, data: [{ key, value, type, group, form_items?, options? }, ...] }
```
按 `group` 聚合为 `SettingGroup`，`form_items` 为 null 的项过滤掉。

### DriverInfo / FormItem schema

```kotlin
sealed class FormItem {
  data class Text(val name: String, val label: String, val default: String?, val required: Boolean) : FormItem()
  data class Bool(val name: String, val label: String, val default: Boolean) : FormItem()
  data class Number(val name: String, val label: String, val default: Double?) : FormItem()
  data class Select(val name: String, val label: String, val options: List<Pair<String,String>>, val default: String?) : FormItem()
  data class MultiSelect(val name: String, val label: String, val options: List<Pair<String,String>>, val default: List<String>) : FormItem()
  data class TextArea(val name: String, val label: String, val default: String?) : FormItem()
  data class Url(val name: String, val label: String, val default: String?) : FormItem() // 渲染同 Text，标记为 URL 校验
}
```

所有类型共享 `name`（key）、`label`（显示名）、`default`。实施时按 v3 实际字段类型扩展枚举值；未识别的类型降级为 `Text`。

### AlistApi 扩展

新增端点：
- `POST /api/admin/storage/update` → `suspend fun updateStorage(@Url url, @Body body): AlistResponse<Unit>`
- `GET /api/admin/driver/list` → `suspend fun listDrivers(@Url url): AlistResponse<List<DriverInfo>>`
- `GET /api/admin/setting/list` → `suspend fun listSettings(@Url url): AlistResponse<SettingsList>`
- `POST /api/admin/setting/save` → `suspend fun saveSettings(@Url url, @Body body): AlistResponse<Unit>`

（具体 body schema 待 v3 后端确认后填入 DTO）

## 组件

### `DynamicFormField`（新增）
- 接收 `FormItem` + 当前值 + `onValueChange: (Any?) -> Unit`
- 内部 `when` 渲染对应 Composable
- 字段类型 → 渲染器映射：
  - `Text` / `Url` → `OutlinedTextField`
  - `Bool` → `Switch`
  - `Number` → `OutlinedTextField(keyboardOptions = Decimal)`
  - `Select` → `ExposedDropdownMenuBox`
  - `MultiSelect` → 多选 chips
  - `TextArea` → `OutlinedTextField(minLines = 3)`
  - 未知类型 → 降级为 `Text`，日志 warn

### `StorageRowItem`（新增）
- `CloudListItem` 变体
- 左侧：mount_path + driver 小字
- 右侧：`Switch`（启/停），Switch 自身消费 onClick 不触发父项

### `StorageEditScreen`（新增）
- 进入：拉 driver 表单定义 + 当前存储对象 → 渲染表单
- 顶部：`CloudTopBar(title = "编辑存储", back)`
- 主体：滚动 Column
  - 卡片 1：基础字段（mount_path 只读、driver 只读、备注/排序等可改字段）
  - 卡片 2：driver 参数（按 `driverInfo.form_items` 渲染 `DynamicFormField`）
  - 卡片 3：启/停（Switch + 状态文本）
- 底部：取消 / 保存 按钮（保存时调 `StorageRepository.update`，失败显示 CloudStatusBanner）

### `AdminSiteSettingsScreen`（新增）
- 顶部：`CloudTopBar(title = "完整设置", back)`
- 主体：按 `SettingGroup` 顺序渲染
  - 每个 group 一个 `CloudCard`，组内是 `DynamicFormField` 列表
- 底部：保存按钮（一次提交所有 group 的 patch）

### SettingsScreen 改造
- 移除底部"多账号、管理员…不在 MVP 范围内"横幅
- 注入 `StorageRepository`（列表/启停）+ `SettingsRepository`（常用项）+ `SessionManager`（拿 baseURL）
- 新增卡片 4（存储） + 卡片 5（站点设置）
- 常用 4 项的编辑交互：点击行 → 弹出 `AlertDialog`（标题=label，副输入框，底部"取消/保存"），保存后关闭对话框并刷新显示值

## ViewModel

| ViewModel | 责任 |
|---|---|
| `SettingsViewModel`（改） | 现有 + 加载 storage list / 加载常用 4 项 / 启停 / 保存常用项 |
| `StorageEditViewModel`（新） | 加载 driver + storage → 表单状态 → 保存 |
| `AdminSiteSettingsViewModel`（新） | 加载 settings → 状态 → 保存 |

## 错误处理

- **网络错误**：`CloudStatusBanner(kind = Error)` + 列表重试按钮
- **401/403 自动 refresh + 重试一次**（由 `AdminRepository.runAdmin` 处理）
- **保存失败**：表单保留已填值，顶部 banner 显示原因
- **启/停失败**：Switch 回滚到切换前状态 + 行内短暂 toast/banner
- **驱动/设置定义字段类型未知**：降级为 TextField 并日志 warn（**不静默**）

## 验证策略

### 单元测试
- `AdminRepositoryTest`：401 自动 refresh + 二次重试成功 / 第二次也失败则返回 `Unauthorized`
- `StorageRepositoryTest`：mock `AlistApi.updateStorage`，验证 patch 构造
- `SettingsRepositoryTest`：mock `listSettings` 解析 group 聚合
- `DynamicFormFieldTest`（Compose UI Test）：7 种类型各一个用例 + 未知类型降级
- `SettingsViewModelTest`（改）：加载存储列表 / 启停成功 + 失败回滚 / 4 个常用项保存
- `StorageEditViewModelTest`：加载表单 / 保存
- `AdminSiteSettingsViewModelTest`：加载 / 保存

### 端到端验证（人工）
- 登录管理员账号 → 设置页应可见存储列表和 4 个常用设置项
- 启/停单个存储 → 列表 Switch 状态变化，二次进入仍正确
- 进入编辑页 → 修改 driver 字段（如本地存储根路径）→ 保存 → 回到列表正常显示
- 站点标题修改 → 保存 → 退出登录 → 重新登录 → 看到新标题（实际验证生效）
- 故意断网 → 启/停应失败回滚、保存按钮给错误提示

## 风险与待确认

1. **`/api/admin/storage/update` body 形态**：默认按 **全量提交**（先 list 拿原对象 → 合并改动字段 → 整对象 POST）。这样不依赖后端是否做 partial 合并。如实测发现 v3 要求仅传变更字段，再回退到 patch。
2. **`/api/admin/setting/list` 返回结构**：v3 实际返回可能 `data` 是 `Map<String, SettingItem>` 而非 `List<SettingItem>`，需实施时对照 v3 源码确认。
3. **`form_items` 字段类型枚举**：v3 实际可能有 8+ 种（如 `datetime`/`color`/`image`），不在 7 类型内的降级为 Text。
4. **driver 多语言 label**：`form_items` 内可能有 `help`/`label_i18n` 字段，本期直接用 `label` 字符串，不做 i18n。
5. **保存并发**：常用项的 4 行内编辑如果在快速点击保存按钮时可能重复提交 — UI 层加 `isSubmitting` 守卫。
6. **测试覆盖**：动态表单的 7 种类型枚举 + 未知降级是新增测试面；目标覆盖率与现有模块持平。

## 实施计划

待 spec 审阅通过后，调用 `superpowers:writing-plans` 拆分任务。预计分 4-5 个子任务：

1. 抽 `AdminRepository` + 改 `HomeRepository` 调用方式
2. 扩展 `AlistApi` + DTO（drivers/settings/forms）+ `FormItem` schema
3. `DynamicFormField` Composable + 测试
4. 设置页改造（卡片 4/5 + ViewModel）+ `StorageEditScreen` + ViewModel
5. `AdminSiteSettingsScreen` + ViewModel + 测试

## 范围外（明确不做）

- 任何形式的"添加存储"/"删除存储" UI/路由
- 用户/角色/会话/任务管理 UI
- 多语言 i18n
- 设置项的"重置默认值"
- 历史/审计日志
- 主题/外观设置
