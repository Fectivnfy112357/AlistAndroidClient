# Alist Android · UI 原型重构设计（v3 · Sky Blue + 糖果）

- **日期**: 2026-07-10
- **作者**: Hermes（设计交付）+ 贾晓源（落地）
- **状态**: 设计已确认，待用户复核
- **前置**:
  - `prototype/alist-android/DESIGN_HANDOFF.md` — 设计师交付
  - `prototype/alist-android/index.html` — 13 屏 HTML 原型
  - `2026-07-08-ui-expressive-redesign-design.md` — 上一轮 M3 Expressive 设计（本 spec 替换它）
  - `2026-07-09-followups-fixes-and-deviations-design.md` — 修复与偏差

---

## 1. 目标与定位

### 1.1 解决的问题

| # | 现状 | 影响 |
|---|---|---|
| 1 | 上一版 M3 Expressive 用 Indigo Blue 主色、Indigo 命名 | 与设计师交付的 Sky Blue + 糖果方案不一致，视觉割裂 |
| 2 | 字体仅系统默认（Roboto），无 Fredoka | 标题缺乏"弹弹"质感，达不到原型优雅可爱的目标 |
| 3 | 圆角 4/8/12/16/28 与原型 10/14/18/22/28 不一致 | 卡片/按钮/输入框观感偏差 |
| 4 | 共享组件（AppTopBar / AppBottomBar / ActionButton / StatusBanner / FileTypeIcon）与原型视觉脱节 | 即使改 token，组件仍是 M3 Expressive 风格 |
| 5 | 5 个主屏（Home / File / Transfer / Settings / Login）布局未对照原型 | 视觉与原型的"卡片列表 + 玻璃白 + 留白"语言不符 |
| 6 | 二级屏（StorageEdit / AdminSiteSettings / MoveCopyPicker / Preview）布局未对照原型 | 同上 |
| 7 | 底部导航 4 Tab（首页/文件/传输/设置），原型要求 5 Tab（+ 音乐） | 不符原型 |
| 8 | 无占位封面/CoverLetter 组件 | 即使未来做音乐，也无视觉基础 |
| 9 | 无 SVG 资源 | 装饰图标/封面占位无法落地 |

### 1.2 不做（YAGNI）

- **音乐功能实现**：原型屏 11（音乐预览）/ 屏 12（音乐库）的实际播放/列表逻辑均不实现，仅保留 Tab 占位与"即将推出"空态
- **大屏双栏 / WindowSizeClass / 折叠屏**
- **国际化文案整理**（仅做 token 准备，文案留后续）
- **Live Edit / Studio Bot**
- **Swipe 手势 / 拖拽排序**
- **背景图 / 真实图片加载**（所有封面/头像用 CoverLetter + 渐变占位）
- **动态取色 / Material You**（原型要求颜色严格可控）
- **音乐库的音频引擎**（MediaSession / ExoPlayer 集成）
- **封面/专辑真实图片从 Alist API 加载**（用 CoverLetter 占位）

### 1.3 成功标准

1. 13 屏（11 屏实际 + 2 屏占位）视觉与原型 index.html 一致（结构 / 间距 / 颜色 / 字体 / 圆角 / 阴影）
2. Fredoka 标题 + Noto Sans SC 正文在所有屏生效
3. 三主题（浅色 / 深色 / 跟随系统）完整可用且持久化
4. 单文件 ≤ 400 行（与上一版一致）
5. `collectAsStateWithLifecycle()` 全面替代 `collectAsState()`（继承上一版约束）
6. 浅蓝白主调 + 糖果点缀全部走 `MaterialTheme.colorScheme.*` token，**零硬编码颜色/字体/圆角**
7. 每个 Screen 至少 3 个 `@Preview`（Light / Dark / 大字）
8. lint 零 error
9. 关键流程 Compose UI Test 不掉
10. 底部 Tab 5 个（首页/文件/**音乐**/传输/设置），音乐 Tab 点击进入空态页

---

## 2. 架构（保留单模块 MVVM）

### 2.1 包结构

```
app/src/main/java/com/textvision/alistclient/
├── ui/
│   ├── theme/
│   │   ├── Color.kt          # 重写：Brand/Candy/Neutral/State + 3 主题
│   │   ├── Type.kt           # 重写：Fredoka + Noto Sans SC
│   │   ├── Shape.kt          # 重写：10/14/18/22/28
│   │   ├── Motion.kt         # 保留 + 增 SpringFast/Medium/Slow
│   │   ├── Icons.kt          # 新增：原型 SVG → ImageVector
│   │   └── Theme.kt          # 关闭 dynamicColor
│   ├── foundation/
│   │   ├── Scaffold.kt       # 保留
│   │   ├── AppBars.kt        # 重写 TopBar + 5 Tab BottomBar
│   │   └── Backgrounds.kt    # 重写 SkyBlue 165° 渐变
│   ├── components/           # 13 个核心组件 + music 子包
│   │   ├── SectionCard.kt    # 玻璃白卡片
│   │   ├── ListItemRow.kt
│   │   ├── FileTypeIcon.kt   # 重新映射 MIME → Icon + Container
│   │   ├── StatusBanner.kt
│   │   ├── SearchField.kt
│   │   ├── ActionButton.kt   # 4 变体
│   │   ├── EmptyState.kt
│   │   ├── ErrorState.kt
│   │   ├── LoadingState.kt
│   │   ├── AppAlertDialog.kt
│   │   ├── ComponentPreviews.kt
│   │   └── music/            # 音乐专用
│   │       ├── CoverLetter.kt
│   │       ├── WaveIndicator.kt
│   │       ├── MusicHeroCard.kt
│   │       ├── AlbumCard.kt
│   │       ├── ArtistCard.kt
│   │       ├── SongRow.kt
│   │       └── MiniPlayer.kt
│   ├── common/
│   │   └── AppError.kt
│   └── feature/              # 13 屏
│       ├── auth/LoginScreen.kt
│       ├── home/HomeScreen.kt + HomeSections/HomeStorageSection/HomeUiState/HomeViewModel/HomeRepository(不变)
│       ├── file/FileScreen.kt + FileListContent/FileMultiSelectBar/FileUiState/FileIntent/FileViewModel(不变)
│       ├── transfer/TransferScreen.kt + TransferListContent/TransferRow/TransferProgress/TransferViewModel(不变)
│       ├── settings/SettingsScreen.kt + SettingsViewModel(不变)
│       ├── preview/PreviewScreen.kt + PreviewImage/PreviewText/PreviewAudio/PreviewFallback/PreviewViewModel(改外观)
│       ├── picker/MoveCopyTargetPickerScreen.kt
│       ├── storage/StorageEditScreen.kt
│       ├── admin/AdminSiteSettingsScreen.kt
│       └── music/            # 占位
│           ├── MusicLibraryScreen.kt   # 5 section + 空态
│           └── MusicPreviewScreen.kt   # 沉浸式播放器空态
├── navigation/               # 增 MusicLibraryDest / MusicPreviewDest
│   ├── AppDestination.kt     # + 2 路由
│   ├── AppNavHost.kt         # 增分支
│   ├── BottomNavBar.kt       # 改为 5 Tab
│   └── AppNavTransitions.kt  # 保留
├── di/, data/, domain/, common/, network/, transfer/, file/, auth/, admin/, preview/, util/  # 不动
└── MainActivity.kt / AlistClientApp.kt  # 不动
```

### 2.2 不动的部分

- **所有数据层**（Repository / DAO / DataStore / API / Network）
- **所有 ViewModel**（LoginViewModel / HomeViewModel / FileViewModel / TransferViewModel / SettingsViewModel / PreviewViewModel / AdminViewModel / FileNameValidator / CopyMoveUseCase）
- **所有 UseCase / Mapper**
- **所有导航类型安全目标**（只增加 2 个新路由）
- **所有测试文件**（如果某个测试因为 token 重命名失败，保留 @Deprecated alias 兜底）
- **Gradle 依赖**（Material Icons Extended 已含；新增 `androidx.compose.ui:ui-text-google-fonts` 给 Fredoka 用）

---

## 3. Design Token

### 3.1 颜色

#### 3.1.1 原始调色板

**主蓝系列**（Sky Blue，替换 IndigoBlue）
```kotlin
val Brand50  = Color(0xFFF4F9FF)
val Brand100 = Color(0xFFE7F2FF)
val Brand200 = Color(0xFFD7E9FF)
val Brand300 = Color(0xFFCDE5FF)
val Brand400 = Color(0xFFBFE0FF)
val Brand500 = Color(0xFF6FB6FF)   // primary light
val Brand600 = Color(0xFF4A98E8)   // primary deep
val Brand700 = Color(0xFF2D7AD0)
val Brand800 = Color(0xFF9DC9FF)   // primary dark
val Brand900 = Color(0xFFD7E9FF)
```

**糖果点缀**
```kotlin
val CandyMint       = Color(0xFF9BE3C8)
val CandyMintBg     = Color(0xFFDAF6EC)
val CandyPink       = Color(0xFFFFC4D6)
val CandyPinkBg     = Color(0xFFFFE4ED)
val CandyLemon      = Color(0xFFFFE89B)
val CandyLemonBg    = Color(0xFFFFF4CC)
val CandyLilac      = Color(0xFFD8C7FF)
val CandyLilacBg    = Color(0xFFECE2FF)
```

**中性色**（Ink 系列，替换 Neutral）
```kotlin
val Ink         = Color(0xFF1F3A5F)   // 主文字
val InkSoft     = Color(0xFF6B8AB5)   // 次级
val InkMute     = Color(0xFFA6BBDB)   // 辅助
val Line        = Color(0xFF7EA7E0).copy(alpha = 0.18f)  // 分割线
val Surface     = Color(0xFFFFFFFF).copy(alpha = 0.78f)  // 玻璃白
val SurfaceSolid= Color(0xFFFFFFFF)
val BgStart     = Color(0xFFF4F9FF)
val BgEnd       = Color(0xFFE7F2FF)
```

**状态色**（去饱和）
```kotlin
val StateError      = Color(0xFFF49AA1)
val StateErrorBg    = Color(0xFFFFE5E8)
val StateWarn       = Color(0xFFF4C77A)
val StateWarnBg     = Color(0xFFFFF1D8)
val StateSuccess    = CandyMint
val StateSuccessBg  = CandyMintBg
```

#### 3.1.2 M3 Light ColorScheme

| Token | 值 | 用途 |
|---|---|---|
| `primary` | `Brand500` (`#6FB6FF`) | 主按钮、关键 icon、激活态 |
| `onPrimary` | `#FFFFFF` | 主按钮文字 |
| `primaryContainer` | `Brand300` (`#CDE5FF`) | 选中态背景、tag 底 |
| `onPrimaryContainer` | `Ink` (`#1F3A5F`) | 选中态文字 |
| `secondary` | `CandyMint` | 次要强调（成功） |
| `secondaryContainer` | `CandyMintBg` | 成功 tag 底 |
| `tertiary` | `CandyPink` | 第三强调（喜爱/装饰） |
| `tertiaryContainer` | `CandyPinkBg` | 装饰 tag 底 |
| `surface` | `#FFFFFF` | 不透明卡片 |
| `surfaceContainer` | `Surface` (alpha 0.78) | 玻璃白卡片 |
| `surfaceContainerHigh` | `Surface` (alpha 0.92) | 浮层、modal |
| `onSurface` | `Ink` | 主文字 |
| `onSurfaceVariant` | `InkSoft` | 次级文字 |
| `outline` | `Line` | 分割线 |
| `background` | `BgStart` | 屏幕底色 |
| `error` | `StateError` | 错误 |
| `onError` | `#FFFFFF` | 错误文字 |

#### 3.1.3 M3 Dark ColorScheme

按原型 §3.1.2 映射：

| Token | Dark 值 |
|---|---|
| `primary` | `Brand800` (`#9DC9FF`) |
| `onPrimary` | `Ink` |
| `primaryContainer` | `Color(0xFF2D4F7C)` |
| `onPrimaryContainer` | `Brand100` |
| `secondary` | `CandyMint` |
| `secondaryContainer` | `Color(0xFF1B5A45)` |
| `tertiary` | `CandyPink` |
| `tertiaryContainer` | `Color(0xFF7C2E48)` |
| `surface` | `Color(0xFF0F2444)` |
| `surfaceContainer` | `Color(0xFF1A2D52)` |
| `surfaceContainerHigh` | `Color(0xFF243E6A)` |
| `onSurface` | `Brand100` |
| `onSurfaceVariant` | `InkMute` |
| `outline` | `InkMute.copy(alpha = 0.18f)` |
| `background` | `Color(0xFF0F2444)` |
| `error` | `Color(0xFFF8B4B8)` |
| `onError` | `Color(0xFF5C1F23)` |

#### 3.1.4 @Deprecated Alias 兼容

保留旧名以减少连锁编译错误：
```kotlin
@Deprecated("Use Brand500", ReplaceWith("Brand500"))
val IndigoBlue40 = Brand500
@Deprecated("Use InkSoft", ReplaceWith("InkSoft"))
val Secondary40 = InkSoft
@Deprecated("Use InkMute", ReplaceWith("InkMute"))
val Neutral50 = InkMute
// ... 其他 Legacy aliases 保留
```

### 3.2 字体

#### 3.2.1 字体加载

**Fredoka**（标题）— 通过 Google Fonts Compose 集成：
```kotlin
val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val FredokaFamily = FontFamily(
    Font(GoogleFont("Fredoka"), googleFontProvider,
         weight = FontWeight.Normal),
    Font(GoogleFont("Fredoka"), googleFontProvider,
         weight = FontWeight.Medium),
    Font(GoogleFont("Fredoka"), googleFontProvider,
         weight = FontWeight.SemiBold),
    Font(GoogleFont("Fredoka"), googleFontProvider,
         weight = FontWeight.Bold),
)
```

**Noto Sans SC**（正文）— Android 12+ 系统自带，Android 7-11 fallback 到系统中文：
```kotlin
val NotoSansScFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(R.font.noto_sans_sc_medium, FontWeight.Medium),
    Font(R.font.noto_sans_sc_bold, FontWeight.Bold),
)
// fallback：FontFamily.SansSerif
```

为了避免引入字体资源包，Android 7-11 直接 fallback 到 `FontFamily.SansSerif`（系统中文）。Android 12+ 用 `GoogleFont("Noto Sans SC")`（已 Google Fonts 提供）— 但需决定。**采用简化方案**：标题走 Fredoka（Google Font），正文走系统默认（`FontFamily.Default`，Android 上是 Roboto + 中文系统字体）。这与原型 §3.2 "Noto Sans SC 中文走系统字体，不引入字体包" 一致。

**最终方案**：
- 标题：`FredokaFamily`（Google Fonts）
- 正文：`FontFamily.Default`（Android 系统中文，无需引入资源）

#### 3.2.2 Typography 文本角色（13 个）

按原型 §3.2 表：

| 角色 | 字体 | 字号 | 字重 | 行高 | 字距 |
|---|---|---|---|---|---|
| `displayLarge` | Fredoka | 32 | 600 | 1.2 | -0.01em |
| `displayMedium` | Fredoka | 24 | 600 | 1.2 | -0.01em |
| `headlineSmall` | Fredoka | 18 | 600 | 1.3 | 0 |
| `titleMedium` | Fredoka | 16 | 600 | 1.4 | 0 |
| `titleSmall` | Fredoka | 14 | 600 | 1.4 | 0 |
| `bodyLarge` | Default | 15 | 400 | 1.5 | 0 |
| `bodyMedium` | Default | 13 | 400 | 1.5 | 0 |
| `bodySmall` | Default | 12 | 400 | 1.5 | 0 |
| `labelLarge` | Default | 14 | 600 | 1.2 | 0 |
| `labelMedium` | Default | 12 | 600 | 1.2 | 0.04em |
| `labelSmall` | Default | 11 | 600 | 1.2 | 0.04em |
| `caption` | Default | 11 | 400 | 1.3 | 0 |
| `numeral` | Fredoka | 26 | 600 | 1.0 | -0.02em |

### 3.3 圆角

```kotlin
object Corner {
    val ExtraSmall = 10.dp   // 小元素、tag
    val Small      = 14.dp   // 输入框、小卡
    val Medium     = 18.dp   // 按钮
    val Large      = 22.dp   // 卡片
    val ExtraLarge = 28.dp   // modal、专辑封面
    val Circle     = 50%     // icon-btn
    val Chip       = 999.dp  // chip
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(Corner.ExtraSmall),
    small      = RoundedCornerShape(Corner.Small),
    medium     = RoundedCornerShape(Corner.Medium),
    large      = RoundedCornerShape(Corner.Large),
    extraLarge = RoundedCornerShape(Corner.ExtraLarge),
)
```

### 3.4 阴影

4 级阴影系统（替换 M3 默认 elevation）：

```kotlin
object AppElevation {
    val None = 0.dp
    val Soft = 6.dp   // 卡片、按钮
    val Pop  = 12.dp  // 浮层、modal
    val Hero = 24.dp  // 大封面
}
```

具体阴影值在组件内通过 `Modifier.shadow()` 注入（`color = Ink.copy(alpha = 0.06)` 等）。

### 3.5 间距

8 点网格（按原型 §3.5）：

```kotlin
object Spacing {
    val xxs = 2.dp
    val xs  = 4.dp
    val s   = 6.dp
    val m   = 8.dp
    val l   = 10.dp
    val xl  = 12.dp
    val xxl = 14.dp
    val s16 = 16.dp
    val s20 = 20.dp
    val s24 = 24.dp
    val s32 = 32.dp
}
```

**常用组合**：
- 屏幕左右 padding：`16dp`
- 卡片内 padding：`14-16dp`
- 区块上下间距：`16-24dp`
- 行间距：`8-10dp`
- icon 与文字间距：`6-8dp`

### 3.6 动效

```kotlin
object AppMotion {
    val SpringFast = spring<Float>(dampingRatio = 0.9f, stiffness = 1200f)
    val SpringMedium = spring<Float>(dampingRatio = 0.85f, stiffness = 600f)
    val SpringSlow = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)

    val TweenShort = tween<Float>(120, easing = FastOutSlowInEasing)
    val TweenMedium = tween<Float>(240, easing = FastOutSlowInEasing)
    val TweenLong = tween<Float>(400, easing = FastOutSlowInEasing)
}

fun isReducedMotion(): Boolean {
    return Settings.Global.getFloat(
        LocalContext.current.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
}
```

### 3.7 主题方案

```kotlin
enum class DarkMode { SYSTEM, LIGHT, DARK }

@Composable
fun AlistTheme(
    darkMode: DarkMode = DarkMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (darkMode) {
        DarkMode.SYSTEM -> systemDark
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    val colorScheme = if (isDark) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
```

**动态取色关闭**（原型要求颜色严格可控）。

---

## 4. 共享组件（13 个核心 + 7 个音乐专用）

### 4.1 通用组件

| 组件 | 文件 | API 关键点 | 视觉关键点 |
|---|---|---|---|
| **AppScaffold** | `foundation/AppScaffold.kt` | 标准 Scaffold（替代 Material3 Scaffold，自定义 TopBar + BottomBar slot） | 系统栏适配 + edge-to-edge |
| **AppTopBar** | `foundation/AppBars.kt` | `title, subtitle?, onBack?, actions` | 顶栏背景 `surface.copy(0.92f)`，back 与 action 是 36dp 圆形 tonal icon-btn，title Fredoka 18 600，subtitle 11 600 + 6×6 在线点（ink 灰） |
| **AppBottomBar** | `foundation/AppBars.kt` | `currentRoute, onNavigate` | 5 Tab（首页/文件/**音乐**/传输/设置），激活态圆角胶囊背景 + tinted icon；glass-white 背景 + 8dp 顶部 shadow |
| **DecoBadge** | `components/DecoBadge.kt` | `icon, position(tl/tr/br/center), size(sm/md/lg), color(pink/mint/lemon/lilac/blue/orange/mute)` | 白色 92% 背景 + 4dp blur + 糖果色 icon；用于 AlbumCard 右下装饰徽章 |
| **SectionCard** | `components/SectionCard.kt` | `Modifier, content` | 玻璃白 `surface.copy(0.78)` + 22dp 圆角 + 4dp shadow |
| **ListItemRow** | `components/ListItemRow.kt` | `leading, title, subtitle?, trailing, onClick?` | 高度 48-64dp、14dp 圆角、ripple |
| **KeyValueRow** | `components/KeyValueRow.kt` | `label: String, value: String` | 一行 label 左 / value 右；label `onSurfaceVariant` labelMedium，value `onSurface` bodyMedium；14dp 垂直间距；用于预览屏的"详细信息"网格 |
| **UserAvatarCard** | `components/UserAvatarCard.kt` | `name: String, role: String, serverName: String, status: String?` | 48dp 圆形渐变头像（CoverLetter 占位）+ 用户名（Fredoka titleMedium）+ 角色 + 服务器 + 右侧 mint chip；用于设置屏顶部 |
| **FileTypeIcon** | `components/FileTypeIcon.kt` | `mimeType, size=40.dp` | 按原型 §4.2.6 表重映射：folder→tertiaryContainer(CandyPinkBg)、image→secondaryContainer(CandyMintBg)、video→primaryContainer(Brand300)、audio→secondaryContainer、text→surfaceContainerHigh、pdf→errorContainer、archive→tertiaryContainer、code→surfaceContainerHigh、其他→surfaceContainerHigh |
| **StatusBanner** | `components/StatusBanner.kt` | `kind(INFO/WARN/ERROR/SUCCESS), message, actionLabel?, onAction?` | 4 种背景色按原型 §4.2.4，18dp 圆角 |
| **SearchField** | `components/SearchField.kt` | `value, onValueChange, placeholder?` | 40dp 高度、chip 圆角、半透明白底 |
| **ActionButton** | `components/ActionButton.kt` | `text, onClick, variant(FILLED/TONAL/OUTLINED/TEXT), enabled, isLoading, leadingIcon?` | 4 变体：FILLED=主蓝渐变 48dp、TONAL=primaryContainer 48dp、OUTLINED=透明+primaryContainer 描边 48dp、TEXT=透明 40dp |
| **EmptyState** | `components/EmptyState.kt` | `icon, title, description?, actionText?, onAction?` | 居中、96×96 SVG 插画、Fredoka 标题 |
| **ErrorState** | `components/ErrorState.kt` | `message, onRetry?` | 浅红圆 + alert icon、retry 按钮 |
| **LoadingState** | `components/LoadingState.kt` | `message?` | CircularProgressIndicator + 文字 |
| **AppAlertDialog** | `components/AppAlertDialog.kt` | 替代 M3 AlertDialog | 28dp 圆角 modal |

### 4.2 音乐专用组件（仅占位使用）

| 组件 | 文件 | 用途 |
|---|---|---|
| **CoverLetter** | `components/music/CoverLetter.kt` | `name, gradient, size=42.dp` — 居中首字封面（CSS 渐变 + Fredoka 首字符）；字号 = size × 0.52 |
| **WaveIndicator** | `components/music/WaveIndicator.kt` | `isPlaying, barCount=4, color` — 4 柱波形，scaleY 动画 1.2s 循环 |
| **MusicHeroCard** | `components/music/MusicHeroCard.kt` | `title, subtitle, isPlaying, onPlayPause, onFavorite, onQueue` | 大渐变横幅（180dp × 28dp 圆角，粉-紫-紫罗兰）+ 顶部 "刚刚播放" label + WaveIndicator + Fredoka 22 标题 + play/heartFill/queue 圆形按钮组 |
| **AlbumCard** | `components/music/AlbumCard.kt` | `name, artist, gradient, decoBadge?, size = Size.SMALL(105dp) / LARGE(140dp)` | 横滑/网格方形专辑卡（封面 CoverLetter + 右下 DecoBadge + 名字 + 艺人） |
| **ArtistCard** | `components/music/ArtistCard.kt` | `name, count, gradient` | 圆形艺人卡（84×84 圆形 CoverLetter + 名字 + 歌曲数） |
| **SongRow** | `components/music/SongRow.kt` | `name, artist, duration, isPlaying?, onClick?` | 紧凑歌曲行（42dp CoverLetter + 歌名 + 艺人 + 时长 + more）；`isPlaying=true` 时背景 `primaryContainer` + 主色名称 |
| **MiniPlayer** | `components/music/MiniPlayer.kt` | 全局底部迷你播放器（即使音乐 Tab 空态，也常驻于 5 主屏底部） |

### 4.3 SVG 资源

将原型 index.html 中 30+ 个 inline SVG 抽到 `res/drawable/`，转 `ImageVector` 用 `Icons.kt` 统一管理：

| 类别 | 图标 |
|---|---|
| 通用 | arrow / more / refresh / upload / download / share / link |
| 文件类型 | folder / image / video / audio / doc / archive / trash |
| 导航 | search / home / file / transfer / settings / user / shield / cloud / database / sparkle |
| 状态 | alert / offline / check / copy / ext |
| 媒体控制 | play / pause / skipPrev / skipNext / heart / heartFill / repeat / shuffle / queue / musicNote / filter / sort / mic |
| 主题 | sun / moon / auto / logout |
| 装饰 | decoNote / decoStar / decoHeart / decoDisc / decoFlame / decoWave / decoSpark / decoHead / cookie / server / broom / musicNote / cloudLogo |

**实现方式**：
1. 把每个 SVG path 数据抽到 `Icons.kt` 中作为 `ImageVector.Builder` 构造（运行时构建，零资源包）
2. 或在 `res/drawable/` 放 XML vector drawable（开发更快，但维护成本高）

**采用方案 1**（运行时构建）。`Icons.kt` 暴露 `object AppIcons`，每个 icon 是 `ImageVector`，由 SVG path 转换。

---

## 5. 13 屏映射

### 5.1 屏清单（按原型 §5.1）

| # | 屏 | 路由 | 主框架 | Tab | 改动 |
|---|---|---|---|---|---|
| 01 | 登录 | `LoginDest` | ❌ | - | **重写** |
| 02 | 首页 | `HomeDest` | ✅ | 首页 | **重写** |
| 03 | 文件浏览 | `FilesDest` | ✅ | 文件 | **重写** |
| 04 | 文件预览 | `PreviewDest` | ❌ | - | **改外观** |
| 05 | 传输 | `TransfersDest` | ✅ | 传输 | **重写** |
| 06 | 设置 | `SettingsDest` | ✅ | 设置 | **重写** |
| 07 | 存储编辑 | `StorageEditDest` | ❌ | - | **重写** |
| 08 | 完整设置 | `AdminSiteSettingsDest` | ❌ | - | **重写** |
| 09 | 移动/复制选择目标 | `MoveCopyPickerDest` | ❌ | - | **重写** |
| 10 | 状态合集 | demo | - | - | **重写** |
| **11** | **音乐预览** | `MusicPreviewDest` | ❌ | - | **占位（空态）** |
| 12 | 音乐库 | `MusicLibraryDest` | ✅ | **音乐** | **占位（5 section + 空态）** |
| 13 | 资源库 | demo | - | - | **重写**（开发自检） |

### 5.2 各屏改动详情

#### 5.2.1 屏 01 登录（重写）

布局（按原型 index.html Screen01_Login）：
- 屏幕背景：`linear-gradient(165deg, #F4F9FF 0%, #E0EEFF 55%, #D5E6FF 100%)`
- 顶部云朵装饰（`clouds()` 函数）
- 中部品牌：76×76 云朵 logo（SVG）+ "登录 Alist" 标题 + 副标题
- 三个输入框（服务器 / 用户名 / 密码），每行有 icon
- "登录 Alist" 主按钮（48dp 高度，蓝渐变 + 光泽）
- 底部："新用户？了解 Alist →" 链接
- 底部装饰：3 个糖果色小圆点

#### 5.2.2 屏 02 首页（重写）

布局（按 HTML Screen02_Home）：
- 顶栏：标题 "早上好 ✨" + 副标题 "已连接 · 我的 Alist" + refresh/search icon-btn
- **Hero 服务器卡**（18dp padding，相对位置有装饰圆）：42×42 蓝渐变方形 + "我的云端小屋" Fredoka 16 + "当前服务器 · v3.41.0" + 右侧 mint 绿点 "在线" chip
- 3 指标行（grid 3 列）：用户/角色/在线，每张 16dp 圆角，icon 28×28 + label + numeral
- 后台任务卡：进行中数字 + **5 chips**（上传 / 解压 / 复制 / 离线下载 / 对象迁移），前 2 个带 dot
- 存储源标题行：左侧 "存储源" 段落标题 + 右侧 "管理 →" 链接
- 存储源列表：4 张 `storage-card`（阿里云/夸克/百度/本地），每张 44×44 渐变方 + 名称 + 路径 + 右侧 chevron 或 "已禁用" chip

#### 5.2.3 屏 03 文件浏览（重写）

布局（按 HTML Screen03_Files）：
- 顶栏：back 圆按钮 + 标题 "文件" + 副标题 "当前离线 · 部分操作不可用"（offline 点）+ refresh/upload
- 离线状态横幅：`StatusBanner(kind = WARNING, message = "离线模式：仅可查看本地缓存")`
- 搜索框（chip 圆角）
- **多选提示条**：`primaryContainer` 蓝渐变背景 + check icon + "已选 N 项 · 点击 **移动** 选择目标目录"（"移动" 加粗）
- 多选模式行：22×22 check 圆 + 选中态背景 `primaryContainer` + `FileTypeIcon` + 名称 + 子标题 + more
- **多选 actionbar**（5 按钮）：`已选 N` + 全选（蓝圆形 28×28 背景） + 移动（primary） + 下载 + 删除（danger） + 取消（×）
- 单选模式行：22×22 check 圆（空）+ FileTypeIcon + 名称 + 子标题 + more

#### 5.2.4 屏 04 文件预览（改外观）

复用 `PreviewViewModel` / `PreviewRouter` / `PreviewImage` / `PreviewText` / `PreviewAudio` / `PreviewFallback` 现有实现。改造外观：
- 顶部 `TopBar` 替换为新 `AppTopBar`（back 圆按钮 + 标题 + 副标题 完整路径 + 分享 / 下载 icon-btn）
- `FileTypeIcon` 走新映射
- 主题色自动随 token 改变
- 错误态/加载态走新 `ErrorState` / `LoadingState`
- **图片预览主体**（仅图片类型）：占满 340dp 高度、18dp 圆角、内部 SVG 占位；底部 14dp 渐变 overlay 文字条（白色 11px 标题 + 10px 副标题）
- **预览主体下方**追加：
  - **3 outlined 按钮行**：分享 / 复制直链 / 其他应用 — `ActionButton(variant = OUTLINED)` + `AppIcons.share / link / external`，18dp 圆角，38dp 高度，flex 1
  - **详细信息卡**：`SectionCard`（solid 14dp padding）+ `card-title "详细信息"` + 4 行 `KeyValueRow`：类型 / 尺寸 / 修改时间 / 位置

#### 5.2.5 屏 05 传输（重写）

布局（按 HTML Screen05_Transfer）：
- 顶栏：标题 "传输" + 副标题 "N 进行中 · M 已完成"（运行时计数）
- segmented 切换器：**4 段** — 全部 / 上传·N（**badge 色 `candy-pink` / `tertiary`**） / 下载·N（**badge 色 `candy-mint` / `secondary`**） / 失败
- 任务卡（4 种状态）：
  - **上传中**：32×32 pink icon（`tertiaryContainer` bg） + 名称（truncate 单行省略） + "上传中"（primary 色 status） + 5dp 渐变进度条（`primary` → `candy-mint`）+ 进度数字 + "取消"链接
  - **下载中**：32×32 mint icon（`secondaryContainer` bg） + 名称 + "下载中"（secondary 色 status） + 进度条 + "取消"链接
  - **失败**：40×40 浅红圆 + alert icon + 名称 + "失败"（`state-error` 色） + 红色进度条 + 错误原因 + "重试 · 删除" 链接
  - **已完成**：卡片整体 `opacity = 0.75` + 32×32 brand icon（`primaryContainer` bg） + 名称 + "已完成"（`onSurfaceVariant` ink-soft 色） + 100% 进度条 + 大小 + 完成时间 + "查看" 链接

#### 5.2.6 屏 06 设置（重写）

布局（按 HTML Screen06_Settings）：
- 顶栏：标题 "设置" + 副标题 "管理你的小窝"（绿点）
- **用户卡**（顶部，SectionCard）：48×48 圆形渐变头像（`CoverLetter` 占位，绿-蓝渐变 `Brush.linearGradient(CandyMint, Brand500)`）+ 用户名 Fredoka 15 + 副标题 "我的云端小屋 · 在线" + 右侧 mint "VIP" chip
- **外观主题** 段落标题（`labelMedium` onSurfaceVariant）+ SectionCard 包含 3 卡主题选择器：
  - **sun 卡**（浅色，激活态）：swatch 三段色块（`BgStart / Surface / BgEnd`） + sun icon + "浅色" 文案
  - **moon 卡**（深色）：swatch 三段色块（`Ink / DarkPrimaryContainer / DarkBg`） + moon icon + "深色"
  - **auto 卡**（跟随）：swatch 三段（`BgStart / 渐变中间 / Ink`） + auto icon + "跟随"
  - 激活态：主色 2dp 描边 + `primaryContainer` 背景
- **存储源** 段落标题 + SectionCard 包含 3 行 `set-row`（36×36 渐变方 + 名称 + 描述 + chevron）：阿里云盘/夸克网盘/百度网盘
- **快速设置** 段落标题 + SectionCard 包含 2 行 set-row：站点公告（chevron）/ 隐私与密码（**Switch 开关**，on 状态）
- **维护** 段落标题 + SectionCard 包含 2 行 set-row：清理临时预览文件（chevron）/ 完整设置（chevron）
- **退出登录** outlined 按钮（红文字 + `state-error-bg` 描边 + 14dp 圆角，flex 1）
- **页脚**：居中 10px 文字 "Alist Client · v1.0.0 · made with 💙"

#### 5.2.7 屏 07 存储编辑（重写）

布局（按 HTML Screen07_StorageEdit）：
- 顶栏：back + 标题 "编辑存储" + 副标题 "存储名"
- **信息卡**（特殊 mint 渐变背景）：`Brush.linearGradient(CandyMintBg, Color(0xFFC2EFE0))`，无描边；内含 44×44 白色圆角方 + mint cloud icon + 存储名 Fredoka 15 (深色 `Color(0xFF1B5A45)`) + "挂载路径 · /xxx" + 右侧 mint "已启用" chip
- **驱动参数** 段落标题 + SectionCard（solid 14dp padding）包含 5 字段：
  - 备注（OutlinedTextField，14dp 圆角）
  - 挂载路径（OutlinedTextField）
  - Cookie：mint 浅色字段卡（`primaryContainer` 蓝渐变背景，圆点 + "已设置 · N 天前更新" + outlined "重新获取" 按钮 + cookie icon）
  - 根目录路径（OutlinedTextField）
  - 排序方式（OutlinedTextField + 右侧 chevron，**下拉选择感**）
  - **启用存储** row：左侧 "启用存储" 13 + "禁用后文件将不再显示" 11 + 右侧 Switch（on 状态，蓝渐变）
- 底部 sticky 48dp FILLED "保存修改" 主按钮
- 按钮下方居中 10px 副文字 "保存成功后将自动返回"

#### 5.2.8 屏 08 完整设置（重写）

布局（按 HTML Screen08_FullSettings）：
- 顶栏：back + 标题 "完整设置" + 副标题 "N 项 · M 组"
- **站点** 段落标题 + SectionCard 包含：
  - 站点标题 / 站点公告 / 站点图标 3 个 OutlinedTextField
  - "隐藏公告" row（左侧 13 + "登录后不可见" 11 + 右侧 Switch，**off**）
- **预览** 段落标题 + SectionCard 包含 3 row：
  - "启用预览"（Switch on）
  - "自动播放视频"（Switch off）
  - "强制代理"（Switch on）
- **安全** 段落标题 + SectionCard 包含：
  - "Token 有效期" OutlinedTextField（48 小时 + 右侧 chevron）
  - "签名直链" row（Switch on）
- 底部 sticky 48dp FILLED "保存全部" 主按钮

#### 5.2.9 屏 09 移动/复制选择目标（重写）

布局（按 HTML Screen09_PickTarget）：
- 顶栏：back + 标题 "选择目标" + 副标题 "移动 N 项到…"
- **面包屑 Row**（位于顶栏下方）：横向 chip 链（`AppIcons.home` + "根目录" + `/` + `AppIcons.folder` + "当前目录"） + 末尾 "+ 新建" pill 按钮（`primaryContainer` 背景，无描边，chip 圆角）
- **新建文件夹输入卡**（当新建激活时显示）：`Surface` with 1.5dp **dashed `primary` 描边** + `primaryContainer` 背景；内含 folder icon + OutlinedTextField（透明无描边）+ 右侧 "✓ 创建" 文字按钮
- **可移动到的位置** 段落标题
- 文件行（`FileTypeIcon` folder 38×38 + 名称 + 次级元数据 "N 项 · X.X MB"）+ 右侧 24×24 radio circle（未选 = 透明 + 2dp `outline` 描边；选中 = 蓝渐变实心 + check icon）
- 当前目录行：背景 `primaryContainer` + 名称副标题 "N 项 · 当前目录" + 选中态用 `check on`（与文件页一致）
- **底部 sticky 48dp FILLED "确认移动到 · {选中名}" 主按钮**（置于 `bottomBar` 之外，scrim 灰白背景）

#### 5.2.10 屏 10 状态合集（demo）

按 HTML Screen10_States：4 态合一展示
- 顶栏：back + 标题 "传输" + 副标题 "离线 · 任务已暂停"（offline 橙点）
- 离线状态横幅：`StatusBanner(kind = WARNING, message = "离线模式：传输操作已暂停", actionLabel = "重试")`
- **空态卡**：`EmptyState` icon (96×96 SVG 文件盒+糖果点缀) + title "这里空空如也 ✨" + desc "失败的传输任务会出现在这里 / 小窝正在安静等待你回来"
- **错误态卡**：SectionCard 包含 40×40 浅红圆 + alert icon + "加载失败" 13 + "网络异常，请检查后重试" 11 + 右侧 "↻ 重试" 按钮（红 chip 圆角）
- **加载骨架卡**：SectionCard 包含 3 行（40×40 圆角方 + 60%/40% 宽度 shimmer 行）
- **删除确认 modal 演示**（内嵌于 SectionCard 占位）：scrim + 底部 modal + drag handle + "确认删除 N 项？" + 描述 + "再想想 / 确认删除" 双按钮

#### 5.2.11 屏 11 音乐预览（占位空态）

- 屏幕背景：`linear-gradient(165deg, #E0EAFF 0%, #D7E9FF 55%, #CDE5FF 100%)` + `CloudDecor`
- 顶栏：back + 标题 "正在播放" + 副标题 "来自「音乐库」" + actions: heart icon-btn + more icon-btn
- 大封面区（300dp 高度，28dp 圆角，粉-紫渐变） + 几何 SVG 装饰（同心圆 + 音符）
- 歌曲信息：歌名 Fredoka 20 + "艺人 · 专辑" 12 + HIRES/FLAC chip 行
- 进度条（6dp 高度，蓝-薄荷渐变 + 14×14 白色手柄）+ 时长 1:24 / 3:42
- 控制按钮行：随机 + 上一首 + **大圆播放 64dp**（蓝渐变 + 6px 白光环）+ 下一首 + 循环
- 副操作行：队列 pill + 麦克风 + 下载
- 歌词预览卡：SectionCard 半透明 + "歌词" 标签 + "展开 ↓" 链接 + 3 行歌词（中间行加粗主色，其余 mute）
- **占位说明**：保留完整播放器视觉骨架，仅文字 + chips 改为 "音乐功能即将推出" 占位文案

#### 5.2.12 屏 12 音乐库（占位 5 section）

布局（按 HTML Screen12_MusicLibrary，结构完整 + 占位内容）：
- 顶栏：back + 标题 "音乐库" + 副标题 "N 首 · M 位艺人" + actions: sort icon-btn + search brand icon-btn
- **Section 1（筛选 chips）**：横向滚动 `LazyRow` 4 chips：全部 · N / 最近添加 · N / 最爱 · N / 下载 · N（首项 primaryContainer 激活；其余 `surfaceContainerHigh` 灰底）
- **Section 2（MusicHeroCard）**：180dp 高度 + 28dp 圆角 + 粉-紫-紫罗兰渐变；顶部 "刚刚播放" 小标 + WaveIndicator；底部歌名 Fredoka 22 + "艺人 · 专辑 · 时长" + 圆形 play + heartFill + queue 圆按钮组
- **Section 3（最近添加）**：section 标题 "最近添加"（4×14 渐变 accent 条 + Fredoka 14）+ "查看全部 →" 链接 + 横向 `LazyRow` 4 张 `AlbumCard`（105×105 + deco-badge 右下）
- **Section 4（艺人）**：section 标题 + "全部 N 位 →" 链接 + 横向 4 张 `ArtistCard`（84×84 圆形 + 名字 + 歌曲数）
- **Section 5（专辑）**：section 标题 + "更多 →" 链接 + **2 列网格**（grid 2 列 12dp gap）2 张 `AlbumCard`（140dp 高 + deco-badge 右下 lg）
- **Section 6（全部歌曲）**：section 标题 + 右侧 "随机播放" 按钮（shuffle icon + 文字）+ 10 个 `SongRow`（42dp CoverLetter + 名称 + 艺人 + 时长 + more）；首项 `playing` 状态（`primaryContainer` 背景 + 主色名称）
- 底部常驻 `MiniPlayer`（点击无操作）

#### 5.2.13 屏 13 资源库（demo）

按原型 Screen13_Assets：资源对照表（开发自检用）。可以用 LazyColumn 展示 token 颜色块 / 圆角 / 阴影示例。

---

## 6. 导航

### 6.1 路由

新增 2 个路由（@Serializable）：

```kotlin
@Serializable data object MusicLibraryDest : AppDestination
@Serializable data object MusicPreviewDest : AppDestination
```

### 6.2 BottomBar 5 Tab

```kotlin
val BottomTabs = listOf(
    BottomTab("home", HomeDest, R.string.tab_home, AppIcons.home),
    BottomTab("files", FilesDest, R.string.tab_files, AppIcons.file),
    BottomTab("music", MusicLibraryDest, R.string.tab_music, AppIcons.musicNote),  // 新增
    BottomTab("trans", TransfersDest, R.string.tab_transfer, AppIcons.transfer),
    BottomTab("set", SettingsDest, R.string.tab_settings, AppIcons.settings),
)
```

激活态视觉：圆角胶囊背景（`primaryContainer`）+ 主色 icon。

### 6.3 AppNavHost 新增分支

```kotlin
composable<MusicLibraryDest> { MusicLibraryScreen(onOpenPreview = { ... }) }
composable<MusicPreviewDest> { MusicPreviewScreen(onBack = { ... }) }
```

---

## 7. 状态管理（继承上一版）

强制 UDF：`StateFlow` + `Intent`，无变化。

新加部分：
- `MusicLibraryUiState` / `MusicLibraryIntent` / `MusicLibraryViewModel`（占位，只暴露空状态）
- `MusicPreviewUiState`（固定常量）

---

## 8. 测试

### 8.1 继承

- 单元测试不变
- Compose UI Test 不变（如果 token 改名导致预览失败，保留 @Deprecated alias 兜底）
- Roborazzi 视觉回归：现有快照**全部失效**，需要重新生成

### 8.2 新增

- 13 屏每个 Screen 至少 3 个 @Preview（Light / Dark / LargeFont）
- CoverLetter / WaveIndicator / MiniPlayer / AlbumCard / ArtistCard / SongRow / MusicHeroCard 各 3 @Preview
- 颜色 token 测试：`ColorTest.kt` 校验主蓝/糖果色/中性色 hex 值
- Fredoka 加载测试：`TypographyTest.kt` 校验 Fredoka Family 不为空

### 8.3 重新生成 Roborazzi 快照

13 屏 + 13 组件 + 7 音乐组件 = 33 屏/组件快照重新生成。

---

## 9. 风险与权衡

### 9.1 风险

| 风险 | 缓解 |
|---|---|
| Roborazzi 全部快照失效，需重新生成 | Plan 阶段产出 Roborazzi 任务清单；CI 跑时一次性接受新 baseline |
| 旧 M3 Expressive 颜色/字体引用散落 70+ 文件 | 用 @Deprecated alias 兜底，新代码走新 token；后续逐步删 alias |
| Fredoka 走 Google Fonts 需 Google Play Services | 中国大陆部分设备 GMS 缺失会回退到系统字体；可接受 |
| MusicLibrary / MusicPreview 占位代码可能与未来实现冲突 | 占位文件用 `// PLACEHOLDER` 注释；ViewModel 暴露 `EmptyState` 不接 API |
| MiniPlayer 在 5 主屏常驻，但点击无操作 | 点击事件 = 空 lambda + 提示 "音乐功能即将推出" |
| 改 5 主屏布局时可能漏改 Preview/Transfer/Storage 等次要屏 | Plan 阶段逐屏列出改动点；review 时按 13 屏表逐项勾选 |

### 9.2 权衡

- **保留 8 个旧共享组件** vs **全删按原型重建**：保留仅能省 ViewModel/导航重写，组件本身必须按原型重写（视觉与原型不一致）
- **Fredoka Google Font** vs **系统字体**：选 Google Font 以贴近原型
- **MiniPlayer 常驻 5 主屏** vs **仅音乐 Tab**：选 5 主屏（原型要求）

---

## 10. 验收清单

### 10.1 设计验收

- [ ] 13 屏视觉与原型 index.html 一致（结构 / 间距 / 颜色 / 字体 / 圆角 / 阴影）
- [ ] 浅色 / 深色 / 跟随系统三主题完整
- [ ] Fredoka 标题在所有屏生效
- [ ] 圆角 / 阴影 / 间距 与 token 严格一致
- [ ] 音乐 Tab 占位（5 section + 空态）
- [ ] MiniPlayer 在 5 主屏底部常驻

### 10.2 工程验收

- [ ] 零硬编码颜色、字体、圆角（grep `Color(0xFF` 仅在 theme/ 下）
- [ ] 单文件 ≤ 400 行
- [ ] `collectAsStateWithLifecycle()` 全面替代 `collectAsState()`
- [ ] lint 零 error
- [ ] 每个 Screen 至少 3 个 @Preview（Light / Dark / LargeFont）
- [ ] Roborazzi 全部快照重新生成并通过 0.1% 容差
- [ ] 13 屏 + 13 通用组件 + 7 音乐组件的 Preview 全覆盖

### 10.3 体验验收

- [ ] 登录页云朵装饰 + 糖果圆点 + 165° 渐变
- [ ] 首页 Hero 服务器卡 + 3 指标行 + 任务卡（5 chip）+ 存储列表 + "管理 →" 链接
- [ ] 文件页多选提示条（含 "移动" 加粗）+ 离线横幅 + 5 按钮 actionbar
- [ ] 文件预览页 3 outlined 按钮 + 图片底部 overlay 信息条 + 详细信息网格卡
- [ ] 传输页 4 段 segmented（上传·N 用 tertiary / 下载·N 用 secondary 着色）+ 4 种状态任务卡（含已完成 opacity 0.75）
- [ ] 设置页用户头像卡 + 主题 3 卡 + 4 段（外观/存储源/快速设置/维护）+ 退出登录 + 版本页脚
- [ ] 存储编辑页 mint 渐变信息卡 + 5 字段驱动参数 + cookie 字段卡 + 启用 toggle + 保存主按钮
- [ ] 完整设置页 3 段（站点/预览/安全）+ Token 有效期 + 保存全部主按钮
- [ ] 选择目标页 面包屑 chip 链（icon 而非 emoji）+ 新建 dashed 卡片 + radio 24×24 + 底部 sticky 确认按钮
- [ ] 音乐预览页完整播放器骨架（300dp 封面 + 进度 + 64dp 大圆 + 控制 + 队列 + 歌词卡），文字为"音乐功能即将推出"占位
- [ ] 音乐库页 4 chips 横滑 + MusicHeroCard + 最近添加 album-card 横滑 + 艺人横滑 + 专辑 2 列网格 + 随机播放按钮 + SongRow playing 状态
- [ ] 状态合集页 4 态（空态/错误/加载骨架/删除确认 modal）
- [ ] 音乐 Tab 点击进入音乐库（5 section + 占位文案）
- [ ] 长按进入多选（移动端习惯）
- [ ] 下拉刷新支持（保留现有实现）
- [ ] 行内菜单（more）支持

---

## 11. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v3.0 | 2026-07-10 | 按 prototype/alist-android/DESIGN_HANDOFF.md 全面替换 M3 Expressive |
| v3.1 | 2026-07-10 | 与 PNG 截图对齐（6 处视觉修正） |
| v3.2 | 2026-07-10 | 与 HTML 原型 + DESIGN_HANDOFF 全面对齐（12 处修正，含 Hero 卡恢复） |

---

## 附录 A：原型资源对照

- **设计交付**：`prototype/alist-android/DESIGN_HANDOFF.md`（860 行）
- **HTML 原型**：`prototype/alist-android/index.html`（13 屏可交互）
- **PNG 截图**：`prototype/alist-android/img.png` ~ `img_10.png`（11 张）
- **使用方式**：`python -m http.server 8765` → 浏览器打开 `index.html`

## 附录 B：替换前后的 token 对照

| M3 Expressive（旧） | Sky Blue v3（新） |
|---|---|
| `IndigoBlue40` (`#1F6FEB`) | `Brand500` (`#6FB6FF`) |
| `IndigoBlue80` | `Brand800` (`#9DC9FF`) |
| `IndigoBlue90` | `Brand300` (`#CDE5FF`) |
| `Secondary40` | `InkSoft` (`#6B8AB5`) |
| `Tertiary40` | `CandyLemon` (`#FFE89B`) |
| `Error40` | `StateError` (`#F49AA1`) |
| `FontFamily.Default` 全文 | Fredoka 标题 + Default 正文 |
| `Corner.Small = 8.dp` | `Corner.Small = 14.dp` |
| `Corner.Medium = 12.dp` | `Corner.Medium = 18.dp` |
| `Corner.Large = 16.dp` | `Corner.Large = 22.dp` |

## 附录 C：旧 token 的 @Deprecated alias

| 旧名 | 新名 |
|---|---|
| `AlistBlue` | `Brand500` |
| `IndigoBlue10/20/30/40/80/90/95/99` | `Brand100/200/300/500/800/900` 等 |
| `Secondary10/20/30/40/80/90/95/99` | `Ink` 系列 |
| `Tertiary10/20/30/40/80/90/95/99` | `CandyLemon` 系列 |
| `Error10/20/30/40/80/90/95/99` | `StateError` 系列 |
| `Neutral10/20/.../99` | `Ink/InkSoft/InkMute` |
| `SurfaceContainer*` | `MaterialTheme.colorScheme.surfaceContainer*` |
| `Outline/OutlineVariant` | `MaterialTheme.colorScheme.outline*` |
| `FolderTint/ImageTint/TextTint` | `MaterialTheme.colorScheme.tertiaryContainer/secondaryContainer/...` |

> 设计完成 · 2026-07-10 · 待用户复核后转入 writing-plans
