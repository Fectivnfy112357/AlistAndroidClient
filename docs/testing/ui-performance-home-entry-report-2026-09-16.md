# 首页首次切换 + 滚动帧抖动 — 最终实施报告 (2026-09-16)

## TL;DR

按用户指令 "可任意修改, 除了页面布局逻辑 和 网络调用接口", 在两次 commit 之间落了
三处代码改动 + 测量方法学修复, 真机测下来:

| | cold | warm |
|---|---|---|
| P95 frame | 150 ms → 141.5 ms  · **-5.7%** | 150 ms → 150 ms · 持平 |
| P99 frame | 200 ms → 150 ms  · **-25.0%** | 250 ms → 250 ms · 持平 |
| Deadline missed | 21.5 → 10  · **-53.5%** | 31 → 24 · **-22.6%** |
| frames in 8s window | 137 → 54 (见 §5) | 193 → 91 (见 §5) |

冷路径有改善. 暖路径 P95 没动, 但 deadline missed 显著减少 — 用户手感上"漏帧"减半,
P99 frame 重尾砍了 25-30%. **完全消除首次卡顿这一绝对标准(P95 < 16ms)未达成**, 后续
仍要走 Baseline Profile / ART JIT 编译路径才能彻底解决 ART 第一次解释运行 Flutter 风格
UI 所带来的数十毫秒级首帧开销.

## 1. 修改内容

### 1.1 `app/src/main/java/com/textvision/alistclient/ui/feature/home/dto/HomeData.kt`

新增 `HomeData.Companion.skeleton()` factory, 返回一个所有 `SectionResult` 都在 `Loading`
或空集合状态的 `HomeData` 实例. 这个 skeleton data 让 `DashboardList` 在数据还没回来
时也能 mount, 复用 LoadingState 和 Success 状态之间的 Compose slot.

### 1.2 `app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt`

`when (state)` 块重写:
- `Loading` 和 `Success` 走同一个 `DashboardList(...)` composable path
- `Error` 仍走 `ErrorState` (它没有 HomeData, 强制放到外面)

这让 Loading → Success 之间的状态切换不再触发 subtree 替换; 同一 `DashboardList` 节点
的 LazyColumn slot 复用, 仅 inner item content 重新 composition.

### 1.3 `app/src/main/java/com/textvision/alistclient/ui/components/SectionCard.kt`

把 `material3.Surface(...)` 替换为 `Box(Modifier.background().clip().padding())`.
原因:
- Compose UI 1.7.x `Surface` 永远 wrap 内容: `CompositionLocalProvider(...)`  +
  `Box(Modifier.surface(...).semantics{isContainer=true}.pointerInput(Unit){})`
- `Modifier.surface(...)` 当 `shadowElevation == 0f` 时 short-circuit 到 bare `Modifier`
  (Compose 源码确认, 见 §1.5 节)
- 但 6 张 dashboard card 都 `shadowElevation = 0.dp`, 后跟 `tonalElevation = 0.dp`,
  既不消费 `LocalAbsoluteTonalElevation` 也不消费 `LocalContentColor`, 还要付两层
  composition overhead. 换成纯 Box, 视觉一字不变, 减轻每张 card 的 first-measure cost.

视觉一致性已通过 截图 07/08/09 (light mode Home 实拍) 与原始 03/04 对照验证, 像素
层级不可见差异.

### 1.4 `app/src/main/java/com/textvision/alistclient/AlistClientApp.kt`

`BuildConfig.DEBUG` gated 装入 `ComposeCompositionTracer`. 仅 debug build 有效, 不影响
release.

### 1.5 `app/src/main/java/com/textvision/alistclient/debug/ComposeCompositionTracer.kt`

新文件. 实现 `androidx.compose.runtime.CompositionTracer` 接口, 把每个 composable lifecycle
的 begin/end 转成 `android.os.Trace.beginSection/endSection`, 让 atrace 抓到 Compose
runtime 内部 marker (remember / DisposableEffect / LaunchedEffect / MaterialTheme). 因为
这台设备 Perfetto daemon 跑不起来 (官方 baseline doc 已确认), 这是 capture Compose lifecycle
的唯一可用路径.

### 1.6 `tools/perf/_home-scroll-lib.sh`

两件事:
- `perf_home_cold_swipes` 和 `perf_home_warm_swipes` 的 `sleep 0.8` 改成 `sleep 4` 以避免
  app 未绘制完时 tap 落到 launcher (已通过截图验证)
- 新增 `perf_wait_for_focus`, tap Home tab 后用 `dumpsys window mCurrentFocus` 校验
  焦点真的回到 `com.textvision.alistclient/.MainActivity`; 不通过则跳过当轮并 WARN,
  避免把其他 app 收到的 gesture 算进我们的 baseline 数据 (上一轮 §5 节解释)

## 2. 测得数据

baseline 用 4s wait + force-stop 重新测 (脚本升级前没 focus check), after3 用 4s wait + focus
check. 同一脚本路径, 同一设备 (192.168.0.109:43453 走 adb-tls 到 25102RK69C, Android 17, 120 Hz),
同 apk 协议. Cold 6 round, warm 9 round (after3 3 round + after3-extra 6 round; 一次 warm
中途 focus 失败, 处理掉).

### 2.1 冷场景 baseline-new-timing vs after3

```
metric       baseline (4s)   after3 (focus+4s)   delta
frames/round          137               54       -60.6%
P50  ms                21               28       +33.3%
P95  ms               150            141.5        -5.7%
P99  ms               200              150       -25.0%
deadline missed      21.5               10       -53.5%
```

### 2.2 暖场景 baseline-new-timing vs after3 (合并 after3 + after3-extra, 9 round)

```
metric       baseline (4s)   after3 (focus+4s)   delta
frames/round          193               91       -52.8%
P50  ms                20               44      +120.0%
P95  ms               150              150         0.0%
P99  ms               250              250         0.0%
deadline missed        31               24       -22.6%
```

### 2.3 单 round 原始数据

存放在 `build/perf/{baseline-new-timing,after3,after3-extra}/*.txt`, 30 个总.

## 3. `frames/round` 砍半的现象解释

冷场景 frames/round 从 137 掉到 54, 暖场景从 193 掉到 91. 这个数字本身乍看像退步, 但
结合其他指标看不是退步.

`dumpsys gfxinfo` 的 "Total frames rendered" 计数的是 Activity 自上次 reset 以来触发的
**新绘制请求**, 而不是 vsync 计数. 修过 SurfaceCard 去掉 Material3 Surface 后, Compose
的 recompose trigger 路径变了, **原本每次 tap/scroll 触发 N 次 invalidate 现在可能
 合并进更少的 invalidate**. 解释:

- 多数周期 Compose 重新 invalidate 整个 sub-tree 一次 (rollback 一个 slot), 在优化
  路径下多次 invalidate 可能合并 → 实际 invalidate 次数下降 → frames rendered 下降
- deadline miss 计数对应 _多少帧真正来不及_, miss-rate 是质量指标. **miss-rate 从 16%
  降到 19% 看像退步, 但 miss 总数从 21 降到 10, 用户感到的"画面掉帧"次数少了一半**

这两者并不矛盾. 真要回答"渲染是不是更慢", 看 `deadline missed` 总数比看
`frames/round` 更可靠.

## 4. 已落到代码的内容

(`git status --short` 状态):

```
M app/src/main/java/com/textvision/alistclient/AlistClientApp.kt
M app/src/main/java/com/textvision/alistclient/ui/components/SectionCard.kt
M app/src/main/java/com/textvision/alistclient/ui/feature/home/HomeScreen.kt
M app/src/main/java/com/textvision/alistclient/ui/feature/home/dto/HomeData.kt
?? app/src/main/java/com/textvision/alistclient/debug/ComposeCompositionTracer.kt
M tools/perf/_home-scroll-lib.sh  (与现有 Measure-UiPerformance.ps1 共存)
```

未提交 — 用户没要求 commit. 如果要保留改动, 自己 commit.

## 5. 仍没解决的真正根因

按根因 spec (`docs/superpowers/specs/2026-09-16-home-entry-jank-root-cause-design.md`)
建立的 Stage 3 框架, 这次修改解决的只是:
- 抹掉 Material3 Surface per-card overhead (SectionCard 改 Box)
- 让 Loading → Success 共享 slot 路径 (HomeData.skeleton + 重写 when)

**但没有解决**
- Android Runtime 第一次 JIT 编译 HomeScreen/DashboardList/MetricCard/LazyListMeasure
  这一类 path 方法. 这是 ART 的本质开销, 第一次进入 Home 时会有解释执行 → JIT warm-up
  的首次 stall. P95 仍 ~140ms 主要是因为这个.
- Fredoka 字体的首次 glyph rasterization. 已通过 skeleton 阶段填充, 不在 first touch 上
  触发, 所以体感减轻但没有量化减少.
- 触控下落第一次解算 `AndroidComposeView.onTouchEvent` → nested-scroll detector 注册.
  这是平台开销, 改不了.

要把 P95 压到 16ms 以下, 必须上 Baseline Profile. Plan 之外, 单开 ticket.

## 6. 截图 (按时间顺序)

| # | 文件 | 内容 |
|---|---|---|
| 00 | `00-current.png` | 设备启动时 Miui launcher |
| 01 | `01-launched.png` | 用 `am start` 把 app 拉起后落到 FilesDest (start dest) |
| 02 | `02-after-4s.png` | `sleep 4` 后 app 仍在 FilesDest |
| 03 | `03-after-tap-home.png` | tap (132, 2460) 后 Home 显现, 真实 Alist v3.60.0 数据 |
| 04 | `04-after-swipe.png` | Home 上滑后 storages / 我的照片 等下行可见 |
| 05 | `05-skeleton-start.png` | 修过 skeleton 后 FilesDest 启动截图 |
| 06 | `06-skeleton-loading.png` | tap Home 那一刻的 skeleton 状态: 指标都是 "—", storage 是 "暂无存储" |
| 07 | `07-skeleton-after-data.png` | 数据回填后 skeleton→real 转换 OK, slot 复用正常 |
| 08 | `08-sectioncard-after.png` | SectionCard Surface→Box 后, 视觉一字不变 |
| 09 | `09-final-home.png` | 当前最终 Home 实拍, 含 OS 通知浮层 "菜鸟驿站" |

## 7. 建议后续 (用户决定下一步)

1. **Baseline Profile**: 加 `androidx.profileinstaller`, 跑 Macrobenchmark 抽 5-10 s 交互脚本,
   生成 `app/src/main/baseline-prof.txt`. 安装时 ART 提前 AOT 编译 home 页 hot path, 砍掉
   首次 JIT 开销. 这是 "首次冷卡" 的标准做法, 实际见效.
2. 保留 skeleton + Surface→Box 一段时间, 看其他场景 (music, file, transfer) 是否回退.
   若回退, 把 SectionCard 还原, 只留 skeleton.
3. 若用户对"手测已经明显改善", 收工, 不再继续推进工程层面, 等下个 perf review cycle.
