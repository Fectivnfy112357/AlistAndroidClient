#!/usr/bin/env bash
# Capture an Android System Trace (atrace) around the home-scroll scenario.
#
# Perfetto config was rejected on this device per
# docs/testing/ui-performance-baseline-2026-07-15.md, so we use
# `atrace` directly. `--async_dump` proved flaky here, so we use
# the simpler sync `atrace -t N -o file` mode and run the host-
# side scenario in parallel.
#
# Categories skip kernel internals (workq etc.) that need root.
#
# Usage:
#   ./Capture-SystemTrace.sh -Scenario cold -Round 1 -Phase baseline
#                            [-Serial <adb-id>] [-WindowSec N]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_home-scroll-lib.sh
. "$SCRIPT_DIR/_home-scroll-lib.sh"

SCENARIO=""
ROUND=""
PHASE="baseline"
PACKAGE="com.textvision.alistclient"
WINDOW_SEC=""  # auto-derive from scenario if unset
EXTRA_DELAY_BEFORE=0.4

usage() {
  cat <<EOF
Usage: $0 -Scenario {cold|warm} -Round <N> [-Phase <name>] [-Serial <adb-id>] [-WindowSec N]
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    -Scenario)    SCENARIO="$2"; shift 2 ;;
    -Round)       ROUND="$2"; shift 2 ;;
    -Phase)       PHASE="$2"; shift 2 ;;
    -Serial)      SERIAL="$2"; shift 2 ;;
    -WindowSec)   WINDOW_SEC="$2"; shift 2 ;;
    -h|--help)    usage; exit 0 ;;
    *) echo "unknown arg: $1" >&2; usage; exit 2 ;;
  esac
done

if [ -z "$SCENARIO" ] || [ -z "$ROUND" ]; then
  echo "ERROR: -Scenario and -Round are required" >&2
  usage
  exit 2
fi
perf_scenario_for "$SCENARIO" >/dev/null

SERIAL="$(perf_resolve_serial)"
OUTDIR="$SCRIPT_DIR/../../build/perf/$PHASE"
mkdir -p "$OUTDIR"

# Clean up any leftover async session from a previous run.
perf_adb "$SERIAL" shell atrace --async_stop >/dev/null 2>&1 || true

# Pick a window long enough to cover the scenario + buffer.
if [ -z "$WINDOW_SEC" ]; then
  case "$SCENARIO" in
    cold) WINDOW_SEC=12 ;;
    warm) WINDOW_SEC=18 ;;
  esac
fi

OUTFILE="$OUTDIR/$SCENARIO-$ROUND.trace"
TRACE_REMOTE="/data/local/tmp/trace-$SCENARIO-$ROUND.trace"

echo "[$(date +%H:%M:%S)] atrace window=${WINDOW_SEC}s serial=$SERIAL scenario=$SCENARIO round=$ROUND"

# Start atrace on device in background. The remote shell stays alive
# for `WINDOW_SEC` and writes the trace to TRACE_REMOTE.
CATEGORIES="sched gfx view input freq idle res am"
adb -s "$SERIAL" shell atrace \
  -t "$WINDOW_SEC" -b 16384 -a "$PACKAGE" $CATEGORIES \
  -o "$TRACE_REMOTE" >/dev/null 2>&1 &
ATRACE_PID=$!

# Give the device-side recorder a moment to settle.
sleep "$EXTRA_DELAY_BEFORE"

case "$SCENARIO" in
  cold) perf_home_cold_swipes "$SERIAL" ;;
  warm) perf_home_warm_swipes "$SERIAL" ;;
esac

# Wait for the remote recorder to finish.
wait "$ATRACE_PID" 2>/dev/null || true

# Sometimes the recorder exits a hair before pull is allowed.
sleep 0.2

perf_adb "$SERIAL" pull "$TRACE_REMOTE" "$OUTFILE" >/dev/null 2>&1 || {
  echo "ERROR: failed to pull $TRACE_REMOTE" >&2
  exit 1
}

perf_adb "$SERIAL" shell rm -f "$TRACE_REMOTE" >/dev/null 2>&1 || true

if [ ! -f "$OUTFILE" ]; then
  echo "ERROR: trace file not produced at $OUTFILE" >&2
  exit 1
fi

size=$(stat -c%s "$OUTFILE" 2>/dev/null || echo 0)
echo "  done -> $OUTFILE ($size bytes)"
if [ "$size" -lt 1024 ]; then
  echo "WARN: trace file < 1 KB; recorder likely failed to capture"
  exit 1
fi
