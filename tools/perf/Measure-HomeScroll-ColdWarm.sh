#!/usr/bin/env bash
# Measure home-scroll P50/P90/P95/P99 + deadline miss under a real device.
#
# Two scenarios, replicating the original Measure-UiPerformance.ps1
# 'home-scroll' input sequence while splitting it by cold vs warm tab
# path:
#
#   cold: force-stop -> start MainActivity -> tap Home -> scroll
#   warm: force-stop -> start MainActivity -> visit each tab to fill
#         back stack -> return to Home -> scroll
#
# Outputs: build/perf/<Phase>/home-<scenario>-scroll-<N>.txt
#          (one file per round per scenario)
#
# Usage:
#   ./Measure-HomeScroll-ColdWarm.sh -Scenario cold -Rounds 3 \
#       -Phase baseline [-Serial <adb-serial>]
#
# Notes:
#   - This script is intentionally bash (Linux-friendly). It does NOT
#     depend on Measure-UiPerformance.ps1's hard-coded Windows adb path.
#   - It outputs only what `dumpsys gfxinfo com.textvision.alistclient`
#     provides; trace data is captured separately by Capture-SystemTrace.sh.
#   - Cross-device comparison: the baseline was taken on
#     192.168.0.109:43453 (25102RK69C, Android 16). If the serial used
#     here differs, document that fact in the report.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=_home-scroll-lib.sh
. "$SCRIPT_DIR/_home-scroll-lib.sh"

SCENARIO=""
ROUNDS=3
PHASE="baseline"
PACKAGE="com.textvision.alistclient"

usage() {
  cat <<EOF
Usage: $0 -Scenario {cold|warm} -Rounds <N> [-Phase <name>] [-Serial <adb-id>]

  -Scenario  cold  (force-stop + start + tap Home, then scroll)
              warm  (visit each tab before returning to Home, then scroll)
  -Rounds    number of measurement rounds (default: 3)
  -Phase     output sub-directory, e.g. baseline / after / after-v2 (default: baseline)
  -Serial    adb serial (default: first 'device' line of 'adb devices')
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    -Scenario) SCENARIO="$2"; shift 2 ;;
    -Rounds)   ROUNDS="$2"; shift 2 ;;
    -Phase)    PHASE="$2"; shift 2 ;;
    -Serial)   SERIAL="$2"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "unknown arg: $1" >&2; usage; exit 2 ;;
  esac
done

if [ -z "$SCENARIO" ]; then
  echo "ERROR: -Scenario {cold|warm} is required" >&2
  usage
  exit 2
fi

perf_scenario_for "$SCENARIO" >/dev/null

SERIAL="$(perf_resolve_serial)"
OUTDIR="$SCRIPT_DIR/../../build/perf/$PHASE"
mkdir -p "$OUTDIR"

echo "[$(date +%H:%M:%S)] serial=$SERIAL scenario=$SCENARIO rounds=$ROUNDS phase=$PHASE"

for i in $(seq 1 "$ROUNDS"); do
  # Reset gfxinfo BEFORE the scenario so the dump covers only the
  # measurement window. A failure here would corrupt the round, so we
  # abort on non-zero.
  perf_adb "$SERIAL" shell dumpsys gfxinfo "$PACKAGE" reset >/dev/null

  case "$SCENARIO" in
    cold) perf_home_cold_swipes "$SERIAL" ;;
    warm) perf_home_warm_swipes "$SERIAL" ;;
  esac

  out="$OUTDIR/home-$SCENARIO-scroll-$i.txt"
  perf_adb "$SERIAL" shell dumpsys gfxinfo "$PACKAGE" >"$out"
  echo "  round $i -> $out"
done

echo "done: scenario=$SCENARIO phase=$PHASE serial=$SERIAL"
