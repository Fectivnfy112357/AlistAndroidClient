#!/usr/bin/env bash
# 冷启动 → 首次 Tab 切换的卡顿诊断回路。
#
# 用法:
#   source tools/dev-env.sh
#   bash tools/jank-loop.sh                                  # 默认 Music → Files → Home
#   JANK_TAPS="600:2440:music" bash tools/jank-loop.sh       # 只测切 Music
#
# 产物落在 $JANK_OUT (默认 /tmp/alist-jank):
#   gfx-<ts>-cold.txt / gfx-<ts>-<label>.txt  每个窗口独立 reset，指标互不污染
#   trace-<ts>.html / report-<ts>.txt
#
# 关键点: atrace 必须带 `-a <pkg>`，否则我们 app 自己的 Trace.beginSection marker
# 不会被写进 trace —— 这正是上一轮"UI 线程 727ms 无标记"最可疑的解释。
set -uo pipefail

PKG=${PKG:-com.textvision.alistclient}
ACT=${ACT:-$PKG/.MainActivity}
OUT=${JANK_OUT:-/tmp/alist-jank}
TAPS=${JANK_TAPS:-"600:2460:music 365:2460:files 132:2460:home"}
SPLASH_WAIT=${JANK_SPLASH_WAIT:-7}
TAP_WAIT=${JANK_TAP_WAIT:-3}
BUF=${JANK_BUF:-65536}
TAGS=${JANK_TAGS:-"sched freq idle am wm gfx view binder_driver input dalvik sm res aidl ss"}
REPO=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)

TS=$(date +%m%d-%H%M%S)
mkdir -p "$OUT"
TRACE="$OUT/trace-$TS.html"
REPORT="$OUT/report-$TS.txt"
GFX_RE='Total frames rendered|Janky frames|Number Missed Vsync|Number Slow UI thread|Number Slow bitmap uploads|Number Slow issue draw commands|50th percentile|90th percentile|95th percentile|99th percentile'

dump() { # $1=label
  adb shell "dumpsys gfxinfo $PKG" > "$OUT/gfx-$TS-$1.txt" 2>&1
}

echo "== [$TS] cold start -> taps: $TAPS"

adb shell am force-stop "$PKG"
sleep 1

echo "== atrace --async_start (app-level on, buffer ${BUF}KB)"
adb shell "atrace --async_start -b $BUF -c -a $PKG $TAGS" || {
  echo "!! atrace 启动失败"; exit 1; }

adb shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1
adb shell "logcat -c" 2>/dev/null
CRASH_LOG="$OUT/crash-$TS.txt"

echo "== am start -W"
adb shell am start -W -n "$ACT" 2>&1 | sed 's/^/   /'

sleep "$SPLASH_WAIT"
APP_PID=$(adb shell pidof "$PKG" | tr -d '\r')
echo "== app pid = ${APP_PID:-<none>}"
dump cold

for spec in $TAPS; do
  x=${spec%%:*}; rest=${spec#*:}; y=${rest%%:*}; label=${rest#*:}
  focus=$(adb shell "dumpsys window" 2>/dev/null | grep -m1 mCurrentFocus | tr -d '\r')
  echo "== tap $label ($x,$y)  focus=$focus"
  adb shell dumpsys gfxinfo "$PKG" reset >/dev/null 2>&1
  adb shell input tap "$x" "$y"
  sleep "$TAP_WAIT"
  dump "$label"
done

echo "== atrace --async_stop -> $TRACE"
adb shell "atrace --async_stop > /data/local/tmp/jank-$TS.html" || echo "!! async_stop 失败"
adb pull "/data/local/tmp/jank-$TS.html" "$TRACE" >/dev/null 2>&1
adb shell "rm -f /data/local/tmp/jank-$TS.html"

{
  echo "=== gfxinfo per window ==="
  for f in "$OUT"/gfx-"$TS"-*.txt; do
    [ -s "$f" ] || continue
    echo "--- $(basename "$f" .txt | sed "s/gfx-$TS-//")"
    grep -E "$GFX_RE" "$f" | sed 's/^/    /'
  done
  echo
  echo "=== crash check (tracer 回归信号) ==="
  adb shell "logcat -d -b crash -v epoch" 2>/dev/null > "$CRASH_LOG"
  if grep -q "FATAL EXCEPTION" "$CRASH_LOG"; then
    echo "!! 采集期间发生崩溃:"
    grep -E "FATAL EXCEPTION|Process:|AndroidRuntime: .*(Exception|Error)" "$CRASH_LOG" | head -8 | sed 's/^/    /'
  else
    echo "OK: 无 FATAL EXCEPTION"
  fi
  echo
  echo "=== trace ==="
  if [ -s "$TRACE" ]; then
    python3 "$REPO/tools/jank-parse.py" "$TRACE" "${APP_PID:-}"
  else
    echo "!! trace 为空"
  fi
} | tee "$REPORT"

echo
echo "== report: $REPORT"
echo "== trace : $TRACE"
