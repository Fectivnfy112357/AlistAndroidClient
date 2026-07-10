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
| **AppScaffold** | `foundation/Scaffold.kt` | 包装 M3 Scaffold，提供 `topBar` / `bottomBar` slot | 屏幕背景渐变（SkyBlue 165°） |
| **AppTopBar** | `foundation/AppBars.kt` | `title, subtitle?, onBack?, actions` | 半透明白底圆按钮、subtle 副标题 + 在线点 |
| **AppBottomBar** | `foundation/AppBars.kt` | `currentRoute, onNavigate` | 5 Tab（首页/文件/**音乐**/传输/设置），激活态圆角胶囊背景 |
| **SectionCard** | `components/SectionCard.kt` | `Modifier, content` | 玻璃白 `surface.copy(0.78)` + backdrop-blur(14) + 22dp 圆角 + 4dp shadow |
| **ListItemRow** | `components/ListItemRow.kt` | `leading, title, subtitle?, trailing, onClick?` | 高度 48-64dp、14dp 圆角、ripple |
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
| **MusicHeroCard** | `components/music/MusicHeroCard.kt` | 大渐变横幅（含波形 + 大歌名 + 操作按钮）— 占位用 |
| **AlbumCard** | `components/music/AlbumCard.kt` | 横滑方形专辑卡（105×105 + 名字 + 艺人） |
| **ArtistCard** | `components/music/ArtistCard.kt` | 圆形艺人卡（84×84 + 名字 + 歌曲数） |
| **SongRow** | `components/music/SongRow.kt` | 紧凑歌曲行（封面首字 + 歌名 + 艺人 + 时长） |
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

布局（按原型 Screen02_Home）：
- 顶栏：标题 "早上好 ✨" + 副标题 "已连接 · 我的 Alist" + refresh/search icon-btn
- Hero 卡（18dp padding，相对位置有装饰圆）：42×42 蓝渐变方形 + "我的云端小屋" Fredoka 16 + "当前服务器 · v3.41.0" + 右侧 mint 绿点 "在线" chip
- 3 指标行（grid 3 列）：用户/角色/在线，每张 16dp 圆角，icon 28×28 + label + numeral
- 后台任务卡：进行中数字 + 4 个 chip（上传/解压/复制/离线下载/对象迁移）
- 存储源列表：4 张 `storage-card`（阿里云/夸克/百度/本地），每张 44×44 渐变方 + 名称 + 路径 + 右侧 chevron 或 chip

#### 5.2.3 屏 03 文件浏览（重写）

布局（按原型 Screen03_Files）：
- 顶栏：back 圆按钮 + 标题 "文件" + 副标题 "当前离线"（offline 点）+ refresh/upload
- 离线状态横幅：`StatusBanner(kind = WARNING)`
- 搜索框（chip 圆角）
- 多选提示条：蓝渐变背景 + check icon + "已选 X 项"
- 多选模式行：22×22 check 圆 + 选中态背景 `primaryContainer`
- 多选 actionbar：底部 4 按钮（下载/分享/移动/删除 danger）
- 单选模式行：FileTypeIcon + 文件名 + 子标题 + more 按钮

#### 5.2.4 屏 04 文件预览（改外观）

复用 `PreviewViewModel` / `PreviewRouter` / `PreviewImage` / `PreviewText` / `PreviewAudio` / `PreviewFallback` 现有实现，**不重写预览主体**。仅替换外观：
- 顶部 `TopBar` 替换为新 `AppTopBar`（back 圆按钮 + 标题 + more icon-btn）
- `FileTypeIcon` 走新映射
- 主题色自动随 token 改变
- 错误态/加载态走新 `ErrorState` / `LoadingState`

#### 5.2.5 屏 05 传输（重写）

布局（按原型 Screen05_Transfer）：
- 顶栏：标题 "传输" + 副标题 "下载 · 上传"
- segmented 切换器：全部/进行中/已完成
- 任务卡：每张 `task-row` 16dp 圆角 + 32×32 icon（上传粉/下载薄荷） + 名称 + 状态 + 进度条（5dp 高度，粉/蓝/薄荷渐变）+ 文件大小/速度/时间
- 失败态：进度条变红

#### 5.2.6 屏 06 设置（重写）

布局（按原型 Screen06_Settings）：
- 顶栏：标题 "设置" + 副标题当前服务器
- 主题选择器 3 张卡：sun（浅蓝白 swatch）/ moon（深蓝 swatch）/ auto（渐变 swatch），激活态主色描边
- 设置行：36×36 渐变方（蓝/薄荷/粉/紫罗兰/黄） + 名称 + 描述 + chevron
- 关于/退出登录：底部

#### 5.2.7 屏 07 存储编辑（重写）

布局（按原型 Screen07_StorageEdit）：
- 顶栏：back + 标题 "存储源" + 副标题
- Cookie 字段卡（蓝渐变背景 + 圆点 + 标签 + "获取" 按钮）
- 表单字段：14dp 圆角输入
- 底部 "保存" 主按钮

#### 5.2.8 屏 08 完整设置（重写）

按原型 index.html 现有设置结构 + 主题改写

#### 5.2.9 屏 09 移动/复制选择目标（重写）

布局（按原型 Screen09_PickTarget）：
- 顶栏：back + 标题 "选择目标" + "确认" 按钮
- 文件行（FileTypeIcon + 文件名）
- 当前路径面包屑
- 空文件夹空态

#### 5.2.10 屏 10 状态合集（demo）

按原型 Screen10_States：EmptyState / ErrorState / LoadingState 各演示

#### 5.2.11 屏 11 音乐预览（占位空态）

- 顶栏：back + 标题 "音乐预览"
- 大封面区（占位 SVG 大图标 + 渐变背景）
- "音乐功能即将推出" 文案
- 不接 ViewModel，固定内容

#### 5.2.12 屏 12 音乐库（占位 5 section）

布局（按原型 Screen12_MusicLibrary，但内容是占位）：
- 顶栏：标题 "音乐库" + 副标题
- **Section 1**: 5 chips（推荐/最近/艺人/专辑/我的）
- **Section 2**: MusicHeroCard（占位 "敬请期待" 渐变横幅）
- **Section 3**: 最近添加（5 个 SongRow 占位）
- **Section 4**: 艺人（3 个 ArtistCard 占位）
- **Section 5**: 专辑（3 个 AlbumCard 占位）
- **Section 6**: 全部歌曲（10 个 SongRow 占位）
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

- [ ] 登录页云朵装饰 + 糖果圆点
- [ ] 首页 Hero 卡 + 3 指标行 + 存储列表
- [ ] 文件页多选 actionbar + 离线横幅
- [ ] 传输页 segmented 切换 + 任务卡
- [ ] 设置页主题 3 卡选择
- [ ] 音乐 Tab 点击进入音乐库（5 section + 空态文案）
- [ ] 长按进入多选（移动端习惯）
- [ ] 下拉刷新支持（保留现有实现）
- [ ] 行内菜单（more）支持

---

## 11. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v3.0 | 2026-07-10 | 按 prototype/alist-android/DESIGN_HANDOFF.md 全面替换 M3 Expressive |

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
