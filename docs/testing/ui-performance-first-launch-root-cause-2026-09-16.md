# 首次启动/首次切 Tab 卡顿根因 — 2026-09-16

## TL;DR

主线程上有一段 **1343 ms 的 `Extract dex file`**，外加约 320 ms 的 `Verify dex file`，
发生在**每次安装/更新后第一次冷启动**。它此前三～四轮排查全部没看到，原因是
所有历史 trace 的 atrace tag 集里都没有 `dalvik` —— ART 的这两个 marker 只在
`dalvik` tag 下产生，而前几轮的 tag 是 `sched gfx view input freq idle res am`。

因果已用对照实验坐实：

| # | 时刻 | 设备/dex 状态 | `Extract dex`+`Verify dex` | 最大单段 |
|---|---|---|---:|---:|
| T1 | 14:24:15 | 改动后首次安装（dex 被 deflate 压缩） | 40 段 / **1652.4 ms** | **1343.2 ms** |
| T2 | 14:25:50 | `cmd package compile -m speed -f` 之后 | **0 段** | 0 |
| T3 | 14:27:57 | `cmd package compile --reset` 之后 | **0 段** | 0 |
| T4 | 14:30:11 | `packaging.dex.useLegacyPackaging = false` 首装 | **0 段** | 0 |
| T5 | 14:34:18 | 未压缩 dex 重装（源码变动触发重新 dexopt） | 20 段 / 93.1 ms（**全部是 `Verify dex`，`Extract dex` = 0**） | 44.6 ms |

T3 说明这份开销是**一次性、持久化在 dalvik-cache 里的**（reset 不会让它回来），
不是每次冷启动都付。T4/T5 说明把 dex 以 **Stored（未压缩）** 打进 APK 即可消除
解压：两次安装的 `Extract dex` 都是 0；剩余校验要么为 0，要么降到 93 ms
（相对 T1 的 1652 ms 是 17.8× 下降，最坏单段 1343 → 44.6 ms）。

## 结论

- **不是** Compose 组成、Room 首查、Hilt 首次实例化、MediaSession IPC —— 这些都有数据排除（见下）。
- 真正吃掉时间的是 **ART 的 dex 解压 + 校验**：APK 里 20 个 dex 全部是 `Defl:N`
  （74 MB dex → 19 MB），压缩过的 dex 无法 mmap，ART 只能在类首次被加载时
  **在主线程上解压出来**。首帧之前主线程被按住 1.3 秒以上，这就是"顿死感"。
- 前几轮 v1–v3（slide 动画 / warmCache 预热 / SplashGate）动的是 Compose 与启动编排，
  碰不到这段 ART 工作，所以"没有效果"是必然的。

## 为什么前几轮看不见（证据）

- 上一会话的 trace `/tmp/trace.html`：`Extract dex` 0 次、`Verify dex` 0 次、
  `dalvik` 0 次。那段 "727 ms 无任何 trace 标记" 的结论，是 tag 集缺失造成的假象，
  不是"阻塞在没有插桩的路径上"。
- session A 的 `build/perf/after2-trace/cold-1.trace`（182 MB，
  tag 集 `sched gfx view input freq idle res am`）同样 0 次 ART marker。
- 顺带发现：该 trace 里 `C(HomeScreen)` 之类 **Compose 函数级 marker 也是 0 个**
  —— 因为 session A 新加的 `ComposeCompositionTracer` 一遇到 `atrace -a` 就崩（见下），
  它存在的唯一目的（函数级组成数据）从未产出过。

## 修掉的真 bug：ComposeCompositionTracer 崩溃

```
java.lang.IllegalArgumentException: sectionName is too long
    at android.os.Trace.beginSection(Trace.java:514)
    at ComposeCompositionTracer$AtraceCompositionTracer.traceEventStart(ComposeCompositionTracer.kt:76)
```

`Trace.beginSection` 拒绝 >127 字符的名字，**且这个校验只在 app trace tag 打开时才执行**。
Compose 的 `info` 对嵌套 lambda composable（`Scaffold` 里的 `subcompose`）轻易超过 127 字符，
于是"只要开始采集就崩溃"——插桩恰好破坏了它要服务的场景。

两处改动（`debug/ComposeCompositionTracer.kt`）：

1. `sectionName(info)`：空串走 `(anonymous)`，超长按 120 字符截断（保留类名头部）。
2. `isTraceInProgress()` 由**无条件 true** 改为 `Trace.isEnabled()`（50 ms TTL 记忆化）。
   原来无条件 true 会让 runtime 对每个 composable 调用都走一次 begin/end ——
   在按帧计时的那条路径上白白引入开销。

回归测试：`app/src/test/java/com/textvision/alistclient/debug/ComposeCompositionTracerTest.kt`（4 例）。
修复后 T1–T4 四轮采集（全部带 `-a`）无 FATAL EXCEPTION。

## 排除清单（T1 实测值）

| 嫌疑 | 实测 | 结论 |
|---|---:|---|
| Room 首次 query | `alist:room:songCount` 6.1 ms | 排除 |
| MediaSession / Service 首次 IPC | `playback:warmUp` 17.7 ms（同步发起）+ `playback:connected` 0.01 ms | 排除 |
| Hilt 首次实例化 VM + 首屏组成 | `nav:Home` 2.5 ms / `nav:Files` 5.4 ms / `nav:Music` 4.9 ms | 排除 |
| 预热与首屏抢主线程 | 6 步全在 IO：HOME 140 ms、FILES 135 ms、MUSIC_INDEX 6 ms、PLAYBACK 18 ms、MUSIC_CACHE 1 ms、TRANSFER 0.2 ms | 非主线程 |
| 单个 Composable 热点 | 函数级 self-time 榜首 `SplashGate` 仅 42 ms / 21 次；`NavHost` 19 ms、`AnimatedContent` 19 ms | 不存在单点热点 |

## SplashGate 实际耗时（回答"要不要留"）

- T1：`alist:splash:await` 152.7 ms，`app:onCreate` 177.7 ms → 冷启动到进入 App ≈ 490 ms。
- T4：await 136.4 ms / `app:onCreate` 112.9 ms → ≈ 350 ms。
- T5：await 151.1 ms / `app:onCreate` 107.7 ms；`app:appRoot` 落在 +358.5 ms，
  首个目的地 `nav:Files` +392.6 ms → 冷启动到首个内容约 400 ms。
- `SPLASH_MAX_MS = 5_000` **从未命中**，超时兜底分支实际是浪费但无害。

## 修复后残留的平台侧开销（T5，约 17 s 窗口，主线程）

| self_ms | 次数 | section |
|---:|---:|---|
| 367.3 | 22 | `sendAccessibilitySemanticsStructureChangeEvents` |
| 227.2 | 26 | `AndroidOwner:measureAndLayout` |
| 105.7 | 40 | `Compose:applyChanges` |
| 70.3 | 1 | `performCreate:MainActivity` |
| 62.9 | 13 | `Record View#draw()` |
| 44.6 | 1 | `Verify dex file ...base.apk` |
| 40.2 | 6 | `AndroidOwner:onTouch` |
| 21.4 | 10 | `com.textvision.alistclient.ui.foundation.SplashGate` |
| 17.8 | 11 | `Recomposer:recompose` |
| 14.8 | 8 | `androidx.compose.animation.AnimatedContent` |
| 13.5 | 4 | `com.textvision.alistclient.ui.foundation.AppBottomBar` |

**注意环境因素**：该测试机开启了无障碍服务
（`com.xiaomi.aicr/...SelectToSpeakService`，`accessibility_enabled=1`），
会放大 semantics 树的遍历与事件派发开销（T4/T5 分别为 268/367 ms，全窗口最大单项）。
关掉无障碍后这一项应显著下降 —— **未验证**。

## 复现方式

```bash
source tools/dev-env.sh
bash tools/jank-loop.sh                 # 冷启动 → Music → Files → Home，含 crash 检查
# 产物: /tmp/alist-jank/{report,trace,gfx,crash}-<ts>.*
```

`tools/jank-loop.sh` 强制带 `-a com.textvision.alistclient`（否则我们自己的 marker
完全不可见）并开 `dalvik`（否则 ART 段不可见）；`tools/jank-parse.py` 按线程栈配对
B/E、按 cookie 配对 S/F，并计算 self-time。

## 决策记录（2026-09-16）

| 项 | 决定 | 依据 |
|---|---|---|
| `packaging.dex.useLegacyPackaging = false` | **保留（全局，debug + release 都生效）** | `Extract dex` 两次安装均为 0；接受 APK 26.7 → 72.6 MB |
| `alist:` 诊断 marker | **保留** | 不采集时只多一次 `Trace.isEnabled()`；后续性能排查可直接复用 |

按 variant 单独设置 dex 打包在 AGP 8.7.2 不可行：公开 variant API 的 `Packaging`
只暴露 `jniLibs` / `resources`，`TestedApkPackaging` 也没有 `dex`。所以这个开关只能全局，
或者用 Gradle 属性做条件开关。

## 未验证 / 待定

1. **release/真机用户是否也付这 1.3 s？** 未验证。AOSP 文档说安装时 dexopt 用
   `verify` filter，而 `verify` 本身就包含 extraction
   （[ART Service](https://android.googlesource.com/platform/art/+/android17-release/libartservice/service/README.md)），
   照此推断安装期就该解压完；我们却在运行时付出解压，说明本次 adb 安装没有跑安装期 dexopt
   （与 debuggable 包的行为一致）。**这条链路没有在本机验证**，需要一份签名 release 包对比。
   若成立，则未压缩 dex 的收益对 release 用户是"预防性"的，而包体代价是实打实的。
2. 未压缩 dex 并没有消掉惰性校验：T5 仍有 93 ms `Verify dex`（最大 44.6 ms），
   相对 T1 的 1652 ms 已可接受，但要归零仍需安装期 dexopt。
3. release 目前 `isMinifyEnabled = false`，dex 有 74 MB 未压缩体积；开 R8 能同时减少
   解压量与包体，但未评估 keep 规则风险。**这是目前最大的未开采杠杆。**
4. `alist:` marker 的移除方式（如需）：`grep -rn "alist:" app/src/main`。

## 验证记录（本次交付）

```
./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
BUILD SUCCESSFUL
单元测试 288 例 / 0 失败 / 0 错误（其中 ComposeCompositionTracerTest 4 例为本次新增回归）
Lint 0 error（71 warning，无一指向本次改动文件）
真机回归（T5）：三轮 tap 全部命中，无 FATAL EXCEPTION
```

GitNexus 影响分析在本环境不可用（`which gitnexus/gnx` 无结果），因此改动符号的影响面
通过 Gradle 编译 + Lint + 288 例单测 + 真机回归覆盖，未做图级别的影响分析。
