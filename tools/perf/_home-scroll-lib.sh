#!/usr/bin/env bash
# Shared helper for the home-scroll cold/warm scenarios.
#
# Sourced by tools/perf/Measure-HomeScroll-ColdWarm.sh and
# tools/perf/Capture-SystemTrace.sh to avoid duplicating the
# fixed swipe sequence. Coordinates come from
# docs/testing/ui-performance-baseline-2026-07-15.md and match
# the existing Measure-UiPerformance.ps1 home-scroll scenario.

# Resolve the ADB serial once. Preference:
#   $SERIAL env > first "device" line of `adb devices` > error.
#
# Example: SERIAL=adb-e331270f-wMfuVU._adb-tls-connect._tcp ./Measure-...
perf_resolve_serial() {
  if [ -n "${SERIAL:-}" ]; then
    echo "$SERIAL"
    return 0
  fi
  local first
  first="$(adb devices 2>/dev/null | awk 'NR>1 && $2=="device" {print $1; exit}')"
  if [ -z "$first" ]; then
    echo "ERROR: no ADB device attached and SERIAL is unset" >&2
    return 1
  fi
  echo "$first"
}

perf_adb() {
  # Wrapper that prefixes `-s $SERIAL` and aborts on non-zero exit.
  local serial="$1"; shift
  adb -s "$serial" "$@" || {
    echo "ERROR: adb $*" >&2
    return 1
  }
}

# Deterministic tab coordinates per baseline doc.
perf_tab_x_for() {
  case "$1" in
    home) echo 132 ;;
    files) echo 365 ;;
    music) echo 600 ;;
    transfers) echo 833 ;;
    settings) echo 1065 ;;
    *) echo "unknown tab: $1" >&2; return 1 ;;
  esac
}

perf_swipe() {
  local serial="$1" x1="$2" y1="$3" x2="$4" y2="$5" duration="${6:-250}"
  perf_adb "$serial" shell input swipe "$x1" "$y1" "$x2" "$y2" "$duration"
}

# Run the home scroll sequence: 5 up-swipes, 5 down-swipes, 250 ms apart.
# Args: serial rounds_remaining(int)
perf_home_scroll_swipes() {
  local serial="$1"
  local i
  for i in 1 2 3 4 5; do
    perf_swipe "$serial" 600 2050 600 500
    perf_swipe "$serial" 600 500 600 2050
  done
}

# Cold home-scroll: force-stop + start + tap Home tab + 5 up + 5 down.
# Mirrors Measure-UiPerformance.ps1 Invoke-Scenario 'home-scroll'.
# Args: serial
perf_home_cold_swipes() {
  local serial="$1"
  perf_adb "$serial" shell am force-stop com.textvision.alistclient
  perf_adb "$serial" shell am start -n com.textvision.alistclient/.MainActivity >/dev/null
  # Give the cold-start + first frame + navhost settle all time to land.
  # 4s typically suffices; the post-tap check (perf_wait_for_focus)
  # bails out cleanly if MainActivity hasn't risen.
  sleep 4
  perf_adb "$serial" shell input tap 132 2460
  if ! perf_wait_for_focus "$serial" 5; then
    echo "WARN: focus did not return to com.textvision.alistclient after Home tap; skipping round" >&2
    return 1
  fi
  sleep 0.6
  perf_home_scroll_swipes "$serial"
}

# Warm home-scroll: force-stop + start + visit each tab to populate
# back stack, return to Home, then 5 up + 5 down. Mirrors a user
# path of "Files -> Music -> Transfers -> Settings -> Home".
# Args: serial
perf_home_warm_swipes() {
  local serial="$1"
  perf_adb "$serial" shell am force-stop com.textvision.alistclient
  perf_adb "$serial" shell am start -n com.textvision.alistclient/.MainActivity >/dev/null
  # Same 4s startup wait, see `perf_home_cold_swipes` rationale.
  sleep 4
  # Visit each tab in baseline order before returning to Home.
  local tab x
  for tab in files music transfers settings; do
    x="$(perf_tab_x_for "$tab")"
    perf_adb "$serial" shell input tap "$x" 2460
    sleep 0.6
  done
  perf_adb "$serial" shell input tap 132 2460
  if ! perf_wait_for_focus "$serial" 5; then
    echo "WARN: focus did not return to com.textvision.alistclient after Home tap; skipping round" >&2
    return 1
  fi
  sleep 0.6
  perf_home_scroll_swipes "$serial"
}

perf_scenario_for() {
  case "$1" in
    cold) echo cold ;;
    warm) echo warm ;;
    *) echo "unknown scenario: $1" >&2; return 1 ;;
  esac
}

# Wait until mCurrentFocus shows our app or warn and return non-zero.
# Without this, when the previous app (e.g. MMS, launcher) had focus
# the tap+swipe landed on someone else and `dumpsys gfxinfo` collected
# frames from that other process instead of `com.textvision.alistclient`,
# which made the round look like "few frames captured, high P95".
perf_wait_for_focus() {
  local serial="$1"
  local timeout_s="${2:-5}"
  local pkg="com.textvision.alistclient"
  local i
  for i in $(seq 1 $((timeout_s * 4))); do
    if perf_adb "$serial" shell dumpsys window 2>/dev/null \
        | grep -q "mCurrentFocus=Window{[a-f0-9]* u0 $pkg/$pkg.MainActivity}"; then
      return 0
    fi
    sleep 0.25
  done
  return 1
}

# Wait until Home tab (1st nav item) is the active destination. Tapping
# (132, 2460) on a 1200x2608 device should land on the leftmost bottom-nav
# item; we treat success as "bottomNav reports HomeDest" via
# currentBackStackEntry on the NavController. Cheaper proxy: dump
# mFocusedApp and confirm it is still our pkg (mostly redundant with the
# focus check above but catches the case where focus drifted mid-tap).
perf_wait_for_home_tab() {
  local serial="$1"
  local timeout_s="${2:-5}"
  local i
  for i in $(seq 1 $((timeout_s * 4))); do
    # Source-aware sanity: just confirm our app is focused within timeout.
    if perf_adb "$serial" shell dumpsys window 2>/dev/null \
        | grep -q "mCurrentFocus=Window{[a-f0-9]* u0 com.textvision.alistclient/"; then
      return 0
    fi
    sleep 0.25
  done
  return 1
}
