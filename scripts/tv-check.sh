#!/usr/bin/env bash
# Read-only report on an Android TV over ADB. Changes nothing unless you pass
# --test-sideload <apk>, which only runs `adb install` on that APK.
#
# Usage:
#   ./tv-check.sh <TV_IP>                          # device info + package proposal
#   ./tv-check.sh <TV_IP> --test-sideload app.apk  # also test that sideloading works
#
# Output is printed and saved to tv-report.txt. Paste it back for review.

set -uo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"
source "$DIR/protected.sh"

TV_IP="${1:?Usage: ./tv-check.sh <TV_IP> [--test-sideload app.apk]}"
APK=""
[[ "${2:-}" == "--test-sideload" ]] && APK="${3:?--test-sideload needs an APK path}"
TARGET="$TV_IP"; [[ "$TARGET" == *:* ]] || TARGET="$TARGET:5555"

command -v adb >/dev/null || { echo "adb not found. Install Android platform-tools." >&2; exit 1; }
adb connect "$TARGET" >/dev/null
adb -s "$TARGET" get-state >/dev/null 2>&1 || { echo "Could not connect to $TARGET. Accept the prompt on the TV and retry." >&2; exit 1; }
A() { adb -s "$TARGET" shell "$@" </dev/null | tr -d '\r'; }

exec > >(tee "$DIR/tv-report.txt") 2>&1

echo "== Device"
echo "Manufacturer : $(A getprop ro.product.manufacturer)"
echo "Model        : $(A getprop ro.product.model)  (device: $(A getprop ro.product.device))"
echo "Android      : $(A getprop ro.build.version.release) (SDK $(A getprop ro.build.version.sdk))"
echo "Build        : $(A getprop ro.build.fingerprint)"
echo "RAM          : $(A cat /proc/meminfo | awk '/MemTotal/{printf "%.2f GB total", $2/1048576} /MemAvailable/{printf ", %.2f GB available", $2/1048576}')"
echo "Launcher     : $(A cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -n 1)"
echo "All home apps:"
A cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME | grep / | sed 's/^/  /'

echo
echo "== Sideload readiness"
R="$(A dumpsys user | grep -oE 'no_install_apps|no_install_unknown_sources[a-z_]*|no_debugging_features' | sort -u)"
echo "User restrictions blocking installs: ${R:-none}"
DO="$(A dumpsys device_policy | grep -iE 'Device Owner|admin=' | head -3)"
echo "Device policy admin: ${DO:-none}"
if [[ -n "$APK" ]]; then
  echo "Test install of $APK:"
  OUT="$(adb -s "$TARGET" install -r "$APK" 2>&1 | tr -d '\r' | tail -n 2)"
  echo "  $OUT"
  if grep -q Success <<<"$OUT"; then
    echo "SIDELOAD: OK"
  else
    echo "SIDELOAD: BLOCKED -> do cleanup only, skip the custom launcher"
  fi
else
  echo "SIDELOAD: not tested (re-run with --test-sideload <apk>)"
fi

echo
echo "== Packages (enabled)"
ALL="$(A pm list packages -e | sed 's/^package://' | sort)"
SYS="$(A pm list packages -s | sed 's/^package://')"
echo "Total enabled: $(wc -l <<<"$ALL")"

CAND_RE='(^|[._])ads?([._]|$)|advert|promo|recommend|sponsor|analytic|telemetry|tracker|tracking|statistic|datacollect|bigdata|report|feedback|logger|crash|survey|marketing|push|notify|appstore|market|store\.|game|music|video|movie|news|shop|ott|partner|preload|demo|retail|showcase|screensaver'

echo
echo "-- PROPOSED to disable (review each; nothing is disabled yet)"
while read -r p; do
  [[ -z "$p" ]] && continue
  is_protected "$p" && continue
  if grep -Eiq "$CAND_RE" <<<"$p"; then
    label=$(grep -qx "$p" <<<"$SYS" && echo system || echo user)
    echo "  $p   [$label]"
  fi
done <<<"$ALL"

echo
echo "-- PROTECTED (never touched)"
while read -r p; do is_protected "$p" && echo "  $p"; done <<<"$ALL"

echo
echo "-- OTHER (not proposed; listed so you can spot vendor bloat)"
while read -r p; do
  [[ -z "$p" ]] && continue
  is_protected "$p" && continue
  grep -Eiq "$CAND_RE" <<<"$p" && continue
  echo "  $p"
done <<<"$ALL"

echo
echo "Saved to $DIR/tv-report.txt"
