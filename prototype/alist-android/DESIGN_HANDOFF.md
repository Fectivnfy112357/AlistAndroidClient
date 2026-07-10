# Alist 安卓客户端 · 设计交付文档

> 设计师：Hermes · 交付日期：2026-07-10
> 配套原型：`prototype/alist-android/index.html`（13 屏可交互）
> 配套资源库：屏 13（设计交付清单）
> 适用版本：Alist Client v1.0.0 · Android · Material 3

---

## 0. 文档使用指南

| 角色 | 重点章节 |
|---|---|
| **前端开发** | §3 Token → §4 组件 → §9 落地指引 → 屏 13 资源库 |
| **设计师** | §2 品牌 → §3 Token → §6 屏清单 → §7 资源清单 |
| **产品 / PM** | §1 背景 → §6 屏清单 → §4 组件 状态/反馈 |
| **QA** | §3.7 验收清单 → §10 兼容性 |

---

## 1. 项目背景 & 设计目标

### 1.1 产品定位
连接 **Alist 私有网盘服务** 的安卓客户端。用户登录后可在手机上浏览、管理、传输云端文件，并对服务器（存储源、站点设置）进行管理员级别的维护。新增音乐预览与音乐库模块，让云盘里的音乐也能像专业播放器一样被消费。

### 1.2 设计目标
- **优雅可爱**：浅蓝白主调 + 糖果点缀，避免浓妆艳抹的廉价感
- **轻盈不轻浮**：柔和阴影 + 玻璃质感 + 适度圆角，精致而非幼稚
- **功能优先**：信息密度合理，重要操作（登录、上传、保存）一眼可见
- **统一可复用**：13 屏共用一套 design token，组件 100% 复用，不允许样式漂移
- **跨主题一致**：浅色 / 深色 / 跟随系统三套主题，token 自动切换

### 1.3 目标用户
- Alist 自部署用户（25-40 岁，技术爱好者）
- 习惯用网盘管理大量多媒体文件（照片 / 视频 / 音乐）
- 重视隐私和审美，对 UI 品质有要求

---

## 2. 品牌定位 & 设计语言

### 2.1 一句话
**"你的私人云端小屋"** —— 安静、精致、有温度的私有云盘伴侣。

### 2.2 关键词
```
云朵 · 玻璃 · 糖果 · 弹弹 · 优雅 · 极简 · 蓝白
```

### 2.3 设计原则
| 原则 | 含义 | 反例 |
|---|---|---|
| **克制的糖果** | 糖果色仅用于点缀（icon 高光、tag、状态），不进入大面积 | 不要把整张卡片涂成粉色 |
| **留白的优雅** | 信息密度低，重要元素周边留 16-24px 空白 | 不要塞满每个像素 |
| **一致的层级** | 标题用 Fredoka、正文用 Noto Sans SC，永远不混用 | 不要正文用 Fredoka |
| **去饱和的状态** | 错误用浅粉红、警告用奶黄，不用大红大黄 | 不要用 #FF0000 表示错误 |
| **真实的圆角** | 卡片 22 / 按钮 18 / 小元素 12，比例协调 | 不要全用 4px 圆角或全用 50px |
| **有呼吸的渐变** | 渐变角度 135°，起止色相差不超过 30% | 不要彩虹色或对比刺眼的渐变 |

---

## 3. 设计 Token（Design Tokens）

所有 token 必须通过 Material 3 `ColorScheme` / `Typography` / `Shapes` 暴露，**禁止硬编码**。

### 3.1 色彩 · Color Tokens

#### 3.1.1 基础调色板（Raw Palette · 颜色源）

**主蓝色系**（品牌色，天空蓝 → 深海蓝）
```
--brand-50:   #F4F9FF    页面背景起始
--brand-100:  #E7F2FF    页面背景结束
--brand-200:  #D7E9FF    主按钮 hover、卡片背景
--brand-300:  #CDE5FF    选中态背景
--brand-400:  #BFE0FF    强调描边
--brand-500:  #6FB6FF    ⭐ 主色（Primary）
--brand-600:  #4A98E8    ⭐ 主色深（Primary Container / Pressed）
--brand-700:  #2D7AD0    主色更深（少见）
```

**糖果点缀色**（克制使用）
```
candy-mint:    #9BE3C8    薄荷糖  · 成功/激活
candy-mint-bg: #DAF6EC    薄荷浅底
candy-pink:    #FFC4D6    樱花粉  · 喜爱/女性化
candy-pink-bg: #FFE4ED    樱花浅底
candy-lemon:   #FFE89B    奶黄糖  · 警告
candy-lemon-bg:#FFF4CC    奶黄浅底
candy-lilac:   #D8C7FF    紫罗兰  · 特殊/高亮
candy-lilac-bg:#ECE2FF    紫罗兰浅底
```

**中性色**（文字层级）
```
ink:           #1F3A5F    主文字（深海军蓝）
ink-soft:      #6B8AB5    次级文字
ink-mute:      #A6BBDB    辅助文字、placeholder
line:          rgba(126, 167, 224, 0.18)  分割线
surface:       rgba(255, 255, 255, 0.78)  卡片背景（玻璃白）
surface-solid: #FFFFFF                     不透明卡片
bg-start:      #F4F9FF    屏幕背景渐变起始
bg-end:        #E7F2FF    屏幕背景渐变结束
```

**语义状态色**（去饱和版，不用纯红/纯黄）
```
state-error:    #F49AA1    错误文字
state-error-bg: #FFE5E8    错误横幅底
state-warn:     #F4C77A    警告文字
state-warn-bg:  #FFF1D8    警告横幅底
state-success:  #9BE3C8    成功（=candy-mint）
state-success-bg:#DAF6EC   成功横幅底
```

#### 3.1.2 Material 3 映射表

| M3 Token | Light | Dark | 用途 |
|---|---|---|---|
| `primary` | `#6FB6FF` | `#9DC9FF` | 主按钮、关键 icon、激活态 |
| `onPrimary` | `#FFFFFF` | `#1F3A5F` | 主按钮文字 |
| `primaryContainer` | `#CDE5FF` | `#2D4F7C` | 选中态背景、tag 底 |
| `onPrimaryContainer` | `#1F3A5F` | `#D7E9FF` | 选中态文字 |
| `secondary` | `#9BE3C8` | `#9BE3C8` | 次要强调（成功状态） |
| `secondaryContainer` | `#DAF6EC` | `#1B5A45` | 成功 tag 底 |
| `tertiary` | `#FFC4D6` | `#FFC4D6` | 第三强调（喜爱/装饰） |
| `tertiaryContainer` | `#FFE4ED` | `#7C2E48` | 装饰 tag 底 |
| `surface` | `#FFFFFF` | `#0F2444` | 卡片背景 |
| `surfaceContainer` | `rgba(255,255,255,0.78)` | `#1A2D52` | 玻璃白卡片 |
| `surfaceContainerHigh` | `rgba(255,255,255,0.92)` | `#243E6A` | 浮层、modal |
| `onSurface` | `#1F3A5F` | `#E7F2FF` | 主文字 |
| `onSurfaceVariant` | `#6B8AB5` | `#A6BBDB` | 次级文字 |
| `outline` | `rgba(126,167,224,0.18)` | `rgba(166,187,219,0.18)` | 分割线 |
| `background` | `#F4F9FF` | `#0F2444` | 屏幕底色 |
| `error` | `#F49AA1` | `#F8B4B8` | 错误 |
| `onError` | `#FFFFFF` | `#5C1F23` | 错误文字 |

### 3.2 字体 · Typography

| 用途 | 字体 | 字号 | 字重 | 行高 | 字距 |
|---|---|---|---|---|---|
| **Display L**（大标题） | Fredoka | 32 | 600 | 1.2 | -0.01em |
| **Display M**（屏标题） | Fredoka | 24 | 600 | 1.2 | -0.01em |
| **Headline S**（卡片标题） | Fredoka | 18 | 600 | 1.3 | 0 |
| **Title M**（区块标题） | Fredoka | 16 | 600 | 1.4 | 0 |
| **Title S**（行内强调） | Fredoka | 14 | 600 | 1.4 | 0 |
| **Body L**（正文） | Noto Sans SC | 15 | 400 | 1.5 | 0 |
| **Body M**（默认正文） | Noto Sans SC | 13 | 400 | 1.5 | 0 |
| **Body S**（辅助文字） | Noto Sans SC | 12 | 400 | 1.5 | 0 |
| **Label L**（按钮文字） | Noto Sans SC | 14 | 600 | 1.2 | 0 |
| **Label M**（标签） | Noto Sans SC | 12 | 600 | 1.2 | 0.04em |
| **Label S**（chip / 微标签） | Noto Sans SC | 11 | 600 | 1.2 | 0.04em |
| **Caption**（时长/数字） | Noto Sans SC | 11 | 400 | 1.3 | 0 |
| **Numeral**（统计数字） | Fredoka | 26 | 600 | 1.0 | -0.02em |

**字体加载**：
- Fredoka 通过 Google Fonts CDN（Compose 用 `GoogleFont.Provider`）
- Noto Sans SC 中文走系统字体，不引入字体包（节省包体）
- 加载策略：`FontVariation.weight(700).toFontFamily()` 动态字重

### 3.3 圆角 · Shape

```kotlin
// Shapes.kt
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),  // 小元素、tag
    small      = RoundedCornerShape(14.dp),  // 输入框、小卡
    medium     = RoundedCornerShape(18.dp),  // 按钮
    large      = RoundedCornerShape(22.dp),  // 卡片
    extraLarge = RoundedCornerShape(28.dp),  // modal、专辑封面
)
```

**应用规则**：
| 元素 | 圆角 | Shape token |
|---|---|---|
| 主按钮 | 18 | `medium` |
| 文件行 | 14 | `small` |
| 卡片 | 22 | `large` |
| 专辑封面 | 28 | `extraLarge` |
| Mini Player | 22 | `large` |
| Modal / Bottom Sheet | 28 顶部 | `extraLarge`（顶部） |
| icon-btn 圆形 | 50% | `CircleShape` |
| chip | 999 | `CircleShape`（50% 即可） |
| 输入框 | 14 | `small` |

### 3.4 阴影 · Elevation

**4 级阴影系统**（不用 Material 默认 elevation，定制柔和版）
```kotlin
val ShadowNone = 0.dp
val ShadowSoft = 6.dp   // 卡片、按钮
val ShadowPop  = 12.dp  // 浮层、modal
val ShadowHero = 24.dp  // 大封面、音乐预览专辑
```

| 元素 | Elevation | 实现 |
|---|---|---|
| 主按钮 | 6dp | `box-shadow: 0 6px 18px rgba(74,152,232,0.35)` |
| 卡片 | 4dp | `0 4px 18px rgba(31,58,95,0.06)` |
| Modal / Bottom Sheet | 12dp | `0 8px 32px rgba(31,58,95,0.12)` |
| 大圆播放按钮 | 24dp + 光环 | `0 8px 24px rgba(74,152,232,0.45), 0 0 0 6px rgba(255,255,255,0.5)` |
| Mini Player | 12dp | `0 -2px 12px rgba(31,58,95,0.08)`（向上阴影） |

### 3.5 间距 · Spacing

**8 点网格系统**
```
2  · 极小（icon 内边距）
4  · 小元素间距
6  · 默认行内间距
8  · 标准间距
10 · 卡片内 padding（紧凑）
12 · 卡片内 padding（默认）
14 · 行间距
16 · 区块内 padding（宽松）
20 · 区块间距
24 · 大区块间距
32 · 屏幕边缘 padding
```

**常用组合**：
- 屏幕左右 padding：`16dp`
- 卡片内 padding：`14-16dp`
- 区块上下间距：`16-24dp`
- 行间距：`8-10dp`
- icon 与文字间距：`6-8dp`

### 3.6 动效 · Motion

**Spring Tokens**
```kotlin
object AppMotion {
    val SpringFast = spring<Float>(dampingRatio = 0.9f, stiffness = 1200f)  // 按钮反馈
    val SpringMedium = spring<Float>(dampingRatio = 0.85f, stiffness = 600f) // Tab 切换
    val SpringSlow = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)    // 页面转场
}
```

**Tween Tokens**
```kotlin
val TweenShort = tween<Float>(120, easing = FastOutSlowInEasing)   // 微交互
val TweenMedium = tween<Float>(240, easing = FastOutSlowInEasing)  // 状态切换
val TweenLong = tween<Float>(400, easing = FastOutSlowInEasing)    // 页面进入
```

**应用规则**：
| 场景 | 动效 | 时长 |
|---|---|---|
| 按钮按压 | `SpringFast` + scale 0.98 | ~100ms |
| Tab 切换 | `SpringMedium` + 颜色渐变 | ~240ms |
| 页面进入 | `SpringSlow` + slide+fade | ~400ms |
| Modal 弹出 | `SpringMedium` + slide up | ~240ms |
| 列表 item 出现 | `SpringFast` stagger 50ms | - |
| 进度条更新 | `TweenMedium` | 240ms |
| 波形动画 | 1.2s `infinite` | - |

**Reduced Motion**：
```kotlin
val effectiveSpring = if (isReducedMotion()) SpringFast else SpringMedium
```
检测 `Settings.Global.ANIMATOR_DURATION_SCALE`，scale = 0 时关闭弹性。

### 3.7 验收清单（开发必查）

- [ ] 所有颜色通过 `MaterialTheme.colorScheme.xxx` 访问，零硬编码
- [ ] 所有圆角通过 `MaterialTheme.shapes.xxx` 访问
- [ ] 所有阴影使用 4 级 elevation token
- [ ] 所有动效用 `AppMotion` 命名常量
- [ ] 字体用 `MaterialTheme.typography.xxx`
- [ ] 浅色 / 深色主题完整测试
- [ ] 单文件不超过 400 行
- [ ] lint 零 error

---

## 4. 组件库（Component Library）

### 4.1 组件清单（13 个核心组件）

| 组件 | 文件 | 用途 |
|---|---|---|
| **AppScaffold** | `foundation/AppScaffold.kt` | 标准 Scaffold（替代 Material3 Scaffold，自定义 TopBar + BottomBar） |
| **AppTopBar** | `foundation/AppBars.kt` | 顶栏（标题 + 副标题 + 返回/操作按钮） |
| **AppBottomBar** | `foundation/AppBars.kt` | 底栏（5 个 Tab，含音乐 Tab） |
| **SectionCard** | `components/SectionCard.kt` | 玻璃白卡片容器 |
| **ListItemRow** | `components/ListItemRow.kt` | 统一列表行（leading/title/subtitle/trailing/onClick） |
| **FileTypeIcon** | `components/FileTypeIcon.kt` | 按 MIME 返回 M3 Icon + Container 颜色 |
| **StatusBanner** | `components/StatusBanner.kt` | 顶部状态横幅（info/warn/error/success） |
| **SearchField** | `components/SearchField.kt` | M3 SearchBar 简化版 |
| **ActionButton** | `components/ActionButton.kt` | 4 变体按钮（FILLED/TONAL/OUTLINED/TEXT） |
| **EmptyState** | `components/EmptyState.kt` | 空态（icon + title + desc + action） |
| **ErrorState** | `components/ErrorState.kt` | 错误态（icon + message + retry） |
| **LoadingState** | `components/LoadingState.kt` | 加载态（CircularProgressIndicator + 文字） |
| **AppAlertDialog** | `components/AppAlertDialog.kt` | 确认弹窗（替代 Material3 AlertDialog） |

### 4.2 关键组件 API 规范

#### 4.2.1 AppTopBar
```kotlin
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
)
```
- 标题用 `Typography.headlineSmall` (Fredoka 18 600)
- 副标题用 `Typography.labelMedium` (11)，可选
- 副标题左侧 6×6 圆点表示在线/离线（`primary` / `warning` 颜色）
- 返回按钮 32×32 圆形，半透明白底
- 右侧 actions 区域支持多个 icon-btn

#### 4.2.2 AppBottomBar
```kotlin
@Composable
fun AppBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
)
```
- 5 个 Tab：首页 / 文件 / **音乐** / 传输 / 设置
- 激活态：圆角胶囊背景 + 主色 icon
- Tab 高度 56dp（含 safe area）
- 玻璃白背景 + backdrop-blur(14)

#### 4.2.3 ActionButton（4 变体）
```kotlin
enum class ButtonVariant { FILLED, TONAL, OUTLINED, TEXT }

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.FILLED,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
)
```

| 变体 | 背景 | 文字 | 描边 | 高度 |
|---|---|---|---|---|
| FILLED | `primary` | `onPrimary` | 无 | 48 |
| TONAL | `primaryContainer` | `onPrimaryContainer` | 无 | 48 |
| OUTLINED | 透明 | `primary` | `primaryContainer` 1.5dp | 48 |
| TEXT | 透明 | `primary` | 无 | 40 |

按钮内部：loading 时显示 Spinner（14×14），leadingIcon 16×16，gap 6dp。

#### 4.2.4 StatusBanner
```kotlin
enum class BannerKind { INFO, WARNING, ERROR, SUCCESS }

@Composable
fun StatusBanner(
    kind: BannerKind,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
)
```

| Kind | 背景 | 文字 | icon |
|---|---|---|---|
| INFO | `primaryContainer` | `onPrimaryContainer` | info |
| WARNING | 浅黄 `#FFF1D8` | 棕 `#8B6A2A` | alert |
| ERROR | 浅粉 `#FFE5E8` | 红 `#B8505C` | alert |
| SUCCESS | `secondaryContainer` | `secondaryContainer` 深色 | check |

圆角 18（用 `shapes.medium`），paddding 10/14，icon 16×16，可选右侧 action button（"重试"/"查看"）。

#### 4.2.5 ListItemRow
```kotlin
@Composable
fun ListItemRow(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
)
```
- 高度：48-64dp（视 subtitle 是否存在）
- leading / trailing slot 自由组合
- 选中态：背景 `primaryContainer`（透明度 0.3）
- ripple 用 `Modifier.clickable`

#### 4.2.6 FileTypeIcon
```kotlin
@Composable
fun FileTypeIcon(
    mimeType: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
)
```

**MIME → Icon + Container 映射**：

| MIME 前缀 | Icon | Container Color |
|---|---|---|
| `folder/` | `Icons.Outlined.Folder` | `tertiaryContainer` |
| `image/*` | `Icons.Outlined.Image` | `secondaryContainer` |
| `video/*` | `Icons.Outlined.Movie` | `primaryContainer` |
| `audio/*` | `Icons.Outlined.MusicNote` | `secondaryContainer` |
| `text/*` | `Icons.Outlined.Description` | `surfaceContainerHigh` |
| `application/pdf` | `Icons.Outlined.PictureAsPdf` | `errorContainer` |
| `application/zip\|rar\|7z` | `Icons.Outlined.Archive` | `tertiaryContainer` |
| `text/x-\|code/*` | `Icons.Outlined.Code` | `surfaceContainerHigh` |
| 其他 | `Icons.Outlined.InsertDriveFile` | `surfaceContainerHigh` |

#### 4.2.7 EmptyState / ErrorState / LoadingState

**统一封装**（避免重复 Column + padding）：
```kotlin
@Composable
fun StateColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
)

@Composable
fun EmptyState(
    icon: ImageVector = Icons.Outlined.Inbox,
    title: String,
    description: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
)

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
)

@Composable
fun LoadingState(message: String = "加载中…")
```

### 4.3 音乐专用组件

| 组件 | 文件 | 用途 |
|---|---|---|
| `CoverLetter` | `components/CoverLetter.kt` | 居中首字封面（CSS 渐变 + Fredoka 首字符） |
| `WaveIndicator` | `components/WaveIndicator.kt` | 4 柱波形动画（播放/暂停态） |
| `MusicHeroCard` | `components/MusicHeroCard.kt` | 大渐变横幅（含波形 + 大歌名 + 操作按钮） |
| `AlbumCard` | `components/AlbumCard.kt` | 横滑方形专辑卡（封面 + 名字 + 艺人） |
| `ArtistCard` | `components/ArtistCard.kt` | 圆形艺人卡 |
| `SongRow` | `components/SongRow.kt` | 紧凑歌曲行（封面首字 + 歌名 + 艺人 + 时长） |
| `MiniPlayer` | `components/MiniPlayer.kt` | 全局底部迷你播放器 |

#### CoverLetter
```kotlin
@Composable
fun CoverLetter(
    name: String,
    gradient: Brush,
    size: Dp = 42.dp,
    modifier: Modifier = Modifier,
)
```
- 取 `name` 第一个 Unicode 字母/数字字符
- 英文 → uppercase
- 中文 → 单字
- 空白则 fallback `♪`
- 字号：`size.value * 0.52f`（42dp → 22px，匹配原型）

#### WaveIndicator
```kotlin
@Composable
fun WaveIndicator(
    isPlaying: Boolean = true,
    barCount: Int = 4,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
)
```
- 4 根柱子，错峰 scaleY 动画
- 暂停时静止 + opacity 0.4

---

## 5. 屏清单 & 导航架构

### 5.1 屏幕清单（13 屏）

| # | 屏 | 路由 | 主框架 | Tab |
|---|---|---|---|---|
| 01 | 登录 | `/login` | ❌ | - |
| 02 | 首页 | `/home` | ✅ | 首页 |
| 03 | 文件浏览 | `/files` | ✅ | 文件 |
| 04 | 文件预览（图片/文本/音频） | `/preview` | ❌ | - |
| 11 | 音乐预览（沉浸式播放器） | `/music/preview` | ❌ | - |
| 05 | 传输 | `/transfer` | ✅ | 传输 |
| 06 | 设置 | `/settings` | ✅ | 设置 |
| 07 | 存储编辑 | `/storage/edit` | ❌ | - |
| 08 | 完整设置 | `/settings/full` | ❌ | - |
| 09 | 移动/复制选择目标 | `/files/pick-target` | ❌ | - |
| 10 | 状态合集（空态/加载/错误） | `*` demo | - | - |
| 12 | 音乐库 | `/music/library` | ✅ | **音乐** |
| 13 | 资源库（设计交付） | `*` demo | - | - |

**主框架 4 + 1 = 5 个 Tab**：首页 / 文件 / **音乐** / 传输 / 设置
**二级页**：1、4、7、8、9、11（无 Tab，有返回按钮）

### 5.2 导航架构

```
Splash
  └─ Login (/login)
       └─ Main (Scaffold + BottomBar 5 Tabs)
            ├─ Home (/home)
            ├─ Files (/files)
            │    └─ Preview (/preview)
            │    └─ PickTarget (/files/pick-target)
            ├─ Music (/music/library)              ← 新增 Tab
            │    └─ MusicPreview (/music/preview)
            ├─ Transfer (/transfer)
            └─ Settings (/settings)
                 └─ StorageEdit (/storage/edit)
                 └─ FullSettings (/settings/full)
```

### 5.3 各屏关键交互（开发必看）

#### 屏 01 · 登录
- 居中卡片式布局
- 三个输入框（服务器 / 用户名 / 密码）
- 主按钮 "登录 Alist" 带 loading
- 错误时顶部红色状态横幅（`StatusBanner kind=ERROR`）

#### 屏 03 · 文件页
- 多选模式：长按进入，行内显示 22dp check 圆圈，底部出现 `actionbar`
- 离线时顶部黄色 `StatusBanner kind=WARNING`
- 行内菜单：preview / share / copy-link / download
- 多选操作：delete（弹确认框）/ batch download

#### 屏 11 · 音乐预览
- 大封面 300dp 高度 + 几何 SVG 装饰
- 控制按钮层级：
  - 大圆播放 64dp
  - 上一首/下一首 48dp
  - 随机/循环 40dp
  - 队列 pill 32dp 高度
- 进度条 + 拖动手柄（柔光环）
- 歌词卡可展开全屏歌词视图

#### 屏 12 · 音乐库
- 5 个 section：chips / hero / 最近添加 / 艺人 / 专辑 / 全部歌曲
- 全部歌曲封面用 `CoverLetter`（居中首字）
- 专辑封面用 `AlbumCard`（SVG 几何 + 右下 deco 徽章）
- 底部常驻 `MiniPlayer`（横跨所有 Tab）

---

## 6. 资源清单（占位实现 → 生产实现）

详细对照见屏 13 资源库。这里给开发一份**精简速查表**：

### 6.1 占位资源 → 实现方式

| 类型 | 元素 | 占位实现 | 生产实现建议 |
|---|---|---|---|
| **专辑封面** | 横滑方形 | CSS 渐变 + SVG 几何 | 后端返回 `cover_url`，用 Coil/Glide 加载缩略图 |
| **专辑封面** | 网格方形 | 同上 | 同上，更大尺寸 |
| **歌曲封面** | 紧凑 42×42 | 渐变 + 居中首字 | 同上，加 placeholder（首字封面）作为加载占位 |
| **Hero 横幅** | 300px | 渐变 + 大装饰 | 当前播放歌曲的大封面图 |
| **Mini Player** | 38×38 | 渐变 + 居中首字 | 同上 |
| **空态插画** | 96×96 | 蓝渐变 + 文件盒 SVG | 复用此 SVG，或换 Lottie |
| **错误插画** | 40×40 icon | 浅红圆 + alert icon | 同上 |
| **樱花山预览** | 340×340 | SVG 山水 | 真实图片缩略图 |
| **登录云朵 Logo** | 76×76 | SVG 渐变云朵 | Alist 官方 logo |
| **用户头像** | 48×48 | 渐变 + 居中首字 | Gravatar / 用户上传 |
| **艺人头像** | 84×84 | 同上 | 艺人真实头像 |

### 6.2 图标策略

**30+ 个 inline SVG icon** 已抽出至 `const I = {...}` 对象（HTML 内）或 `IconKey.kt`（Kotlin）。

**生产建议**：
- 优先使用 **Material Icons Extended**（已含在依赖中）
- 找不到对应 icon 时，使用 Lucide / Phosphor 同语义 icon
- deco 装饰图标（音符/星星/唱片/火焰）保留为项目专属 SVG，不参与主题切换

**图标尺寸规范**：
| 场景 | 尺寸 |
|---|---|
| Tab 底部 icon | 22dp |
| 顶栏 icon-btn | 16dp |
| 卡片 leading icon | 18-22dp |
| 列表行 trailing icon | 16-18dp |
| 大按钮 icon | 20-24dp |
| 装饰徽章 | 14-18dp |

---

## 7. 主题方案

### 7.1 三主题策略

```kotlin
enum class DarkMode { SYSTEM, LIGHT, DARK }

@Composable
fun AlistTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    // Android 12+ 动态取色，否则静态
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        isDark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
```

### 7.2 主题色板实现

详见 §3.1.2 M3 映射表，对应 `Color.kt` 中的 `LightColors` / `DarkColors`。

### 7.3 主题切换持久化

`ThemeRepository.darkMode: Flow<DarkMode>` → DataStore 持久化
`SettingsViewModel.setDarkMode(DarkMode)` → 调用 repo → 触发 `darkMode` Flow → UI 自动重组

### 7.4 主题选择器 UI

设置页 → 外观主题 → 3 张主题卡：
- 浅色（sun icon + 浅蓝白 swatch）
- 深色（moon icon + 深蓝 swatch）
- 跟随系统（clock icon + 渐变 swatch）

激活态：主色描边 + 主色背景。

---

## 8. 无障碍 & 国际化

### 8.1 无障碍（A11Y）

- **最小可点击区域**：44×44dp（Material 标准）
- **文本对比度**：所有文字 vs 背景 ≥ 4.5:1（WCAG AA）
- **Touch Target Spacing**：相邻可点击元素间距 ≥ 8dp
- **屏幕阅读器**：所有 icon-btn 必须有 `contentDescription`
- **动态字体**：所有字号使用 `sp`，最大支持 200%
- **减少动效**：检测 `Settings.Global.ANIMATOR_DURATION_SCALE`，scale=0 时禁用 Spring

### 8.2 国际化（i18n）

- **支持语言**：简体中文（默认）、英文
- **文案资源**：`res/values/strings.xml` + `res/values-en/strings.xml`
- **不要硬编码**中文/英文字符串，所有 UI 文案走 string resource
- **图标语义**：icon 必须有 `contentDescription`，跟随系统语言切换

---

## 9. 开发落地指引

### 9.1 包结构

```
com.textvision.alistclient/
├── ui/
│   ├── foundation/         # AppScaffold, AppBars, Backgrounds
│   ├── components/         # 13 个核心组件
│   ├── theme/             # Color, Type, Shape, Motion, Theme
│   └── feature/
│       ├── auth/          # 登录屏
│       ├── home/          # 首页
│       ├── file/          # 文件浏览、预览、选择目标
│       ├── music/         # 音乐库、音乐预览
│       ├── transfer/      # 传输
│       └── settings/      # 设置、存储编辑、完整设置
├── navigation/            # AppNavHost, AppRoute
├── data/                  # Repository, DataStore
└── domain/                # Model, UseCase
```

### 9.2 单屏文件结构

```kotlin
// FileScreen.kt (< 400 行)
@Composable
fun FileScreen(
    viewModel: FileViewModel = hiltViewModel(),
    onNavigate: (FileRoute) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppScaffold(
        topBar = {
            AppTopBar(
                title = "文件",
                subtitle = state.currentPath,
                onBack = { onNavigate(FileRoute.Back) },
            )
        },
        bottomBar = { AppBottomBar("files", onNavigate) },
    ) { padding ->
        FileContent(state, viewModel::onIntent, Modifier.padding(padding))
    }
}
```

### 9.3 命名规范

| 类型 | 规范 | 示例 |
|---|---|---|
| Composable | `PascalCase` | `AppTopBar`, `FileTypeIcon` |
| Composable 内部 helper | `PascalCase` | `FileContent` |
| 函数（非 Composable） | `camelCase` | `coverChar`, `formatSize` |
| 状态 sealed interface | `XxxUiState` | `FileUiState` |
| 意图 sealed interface | `XxxIntent` | `FileIntent` |
| ViewModel | `XxxViewModel` | `FileViewModel` |
| 路由 | `XxxRoute` | `FileRoute.PickTarget` |
| 资源 token | `kebab-case` | `color-brand-500` |
| 资源 token（Kotlin） | `camelCase` | `brandPrimary500` |

### 9.4 状态管理

```kotlin
// 强制 UDF：StateFlow + Intent
data class FileUiState(
    val files: List<FileItem> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val currentPath: String = "/",
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface FileIntent {
    data class LoadFiles(val path: String) : FileIntent
    data class ToggleSelect(val id: String) : FileIntent
    data class Delete(val ids: List<String>) : FileIntent
    data class Open(val file: FileItem) : FileIntent
}

@HiltViewModel
class FileViewModel @Inject constructor(
    private val repo: FileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FileUiState())
    val state: StateFlow<FileUiState> = _state.asStateFlow()

    fun onIntent(intent: FileIntent) { /* handle */ }
}
```

### 9.5 性能 & 重组稳定性

- **State 收集**：强制用 `collectAsStateWithLifecycle()`，不用 `collectAsState()`
- **lambda 稳定**：不要在 Composable 内 inline lambda 引用重组不稳定的变量
- **key 用法**：`LazyColumn` 的 item 必须有稳定 key（用文件 ID）
- **remember**：复杂计算用 `remember` / `remember(key)` 缓存
- **derivedStateOf**：依赖其他 state 派生时用 `derivedStateOf`

### 9.6 测试覆盖

每个 Screen 至少包含：
- `LightFileScreenPreview`（亮色预览）
- `DarkFileScreenPreview`（深色预览）
- `LargeFontFileScreenPreview`（大字预览，1.5x 缩放）
- 1 个 `composeTestRule` UI 测试（覆盖关键交互）

---

## 10. 兼容性 & 验收

### 10.1 兼容性

| 项 | 要求 |
|---|---|
| minSdk | 24 (Android 7.0) |
| targetSdk | 35 (Android 15) |
| 动态取色 | Android 12+ 启用，低版本回退静态 token |
| 屏幕方向 | 仅竖屏 |
| 字体 | Android 12+ 系统支持中文 Noto，Android 7-11 用系统默认 |

### 10.2 验收清单

#### 设计验收
- [ ] 所有屏与原型 HTML 一致（结构 / 间距 / 颜色 / 字体）
- [ ] 浅色 / 深色 / 跟随系统三主题完整
- [ ] 圆角 / 阴影 / 间距 与 token 严格一致
- [ ] 字体使用正确（Fredoka 标题、Noto Sans SC 正文）
- [ ] 动画流畅（Spring / Tween 应用到位）
- [ ] 音乐预览/音乐库 5 个 Tab 完整可用

#### 工程验收
- [ ] 零硬编码颜色、字体、圆角
- [ ] 单文件 ≤ 400 行
- [ ] `collectAsStateWithLifecycle()` 全面替代 `collectAsState()`
- [ ] lint 零 error（warning 允许但需 review）
- [ ] 每个 Screen 至少 3 个 @Preview（Light/Dark/LargeFont）
- [ ] Compose UI Test 覆盖关键流程（空态、多选、错误重试）
- [ ] Roborazzi 截图回归测试通过（0.1% 容差）

#### 体验验收
- [ ] 离线态有明确横幅提示
- [ ] 错误态有「重试」按钮
- [ ] 空态有引导文案（不是冷冰冰的"暂无数据"）
- [ ] 长按进入多选（移动端习惯）
- [ ] 下拉刷新支持
- [ ] 行内菜单（more）支持

---

## 11. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | 2026-07-10 | 初版交付（13 屏 + 资源库） |

---

## 附录 A：原型使用指引

打开 `prototype/alist-android/index.html`（推荐 Chrome）：
- 顶部 nav chip 切换 13 个屏
- 直接对应实现参考
- 屏 13（资源库）含所有占位资源的视觉对照表

打开方式：
```bash
cd prototype/alist-android
python -m http.server 8765
# 访问 http://127.0.0.1:8765/index.html
```

## 附录 B：设计文件清单

| 文件 | 路径 | 说明 |
|---|---|---|
| 原型 | `prototype/alist-android/index.html` | 13 屏可交互 HTML |
| 本文档 | `prototype/alist-android/DESIGN_HANDOFF.md` | 设计交付文档 |
| 产品说明 | `.hermes/desktop-attachments/产品与原型说明-4.md` | 原始产品定义 |

---

> **设计交付完成 · 2026-07-10 · Hermes**
> 任何问题找设计师对回这份文档，所有 token、组件、屏都有据可查。