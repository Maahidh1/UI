#!/usr/bin/env bash
# Declutter an Android TV / Google TV home screen over ADB.
#
# Only ever uses `pm disable-user --user 0` (never uninstall). Each package
# touched is recorded in tv-clean.state; tv-restore.sh re-enables them all.
# Packages matching protected.sh (keyboard, Play Store/services, Bluetooth,
# Wi-Fi, remote/input, settings, updates) are refused outright.
#
# Usage:
#   ./tv-clean.sh <TV_IP>                       # dry run: show what would change
#   ./tv-clean.sh <TV_IP> --apply               # disable clutter packages
#   ./tv-clean.sh <TV_IP> --apply --set-home    # also make Clean Home the default launcher
#
# Prereqs: on the TV, Settings > Device Preferences > About > tap "Build" 7x,
# then Developer options > enable "Network debugging" (or "USB debugging").

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
LIST="$DIR/packages.txt"
STATE="$DIR/tv-clean.state"
HOME_ACTIVITY="com.cleanhome.tv/.MainActivity"

TV_IP="${1:-}"
shift || true
APPLY=0
SET_HOME=0
for arg in "$@"; do
  case "$arg" in
    --apply) APPLY=1 ;;
    --set-home) SET_HOME=1 ;;
    *) echo "Unknown option: $arg" >&2; exit 1 ;;
  esac
done

if [[ -z "$TV_IP" ]]; then
  sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
fi

command -v adb >/dev/null || { echo "adb not found. Install Android platform-tools." >&2; exit 1; }

TARGET="$TV_IP"
[[ "$TARGET" == *:* ]] || TARGET="$TARGET:5555"

echo "Connecting to $TARGET (accept the prompt on your TV if one appears)..."
adb connect "$TARGET" >/dev/null
adb -s "$TARGET" wait-for-device
# </dev/null: stop adb from eating the stdin of surrounding read loops.
A() { adb -s "$TARGET" shell "$@" </dev/null; }

source "$DIR/protected.sh"

INSTALLED="$(A pm list packages | sed 's/^package://' | tr -d '\r')"

# Read package list, skipping comments/blank lines (bash 3.2 compatible).
TO_DISABLE=()
while read -r pkg _; do
  [[ -z "$pkg" || "$pkg" == \#* ]] && continue
  if is_protected "$pkg"; then
    echo "REFUSING protected package: $pkg (remove it from packages.txt)" >&2
    exit 1
  fi
  if grep -qx "$pkg" <<<"$INSTALLED"; then
    TO_DISABLE+=("$pkg")
  fi
done <"$LIST"

if [[ ${#TO_DISABLE[@]} -eq 0 ]]; then
  echo "None of the packages in packages.txt are installed. Nothing to do."
else
  echo "Packages that will be disabled:"
  printf '  - %s\n' "${TO_DISABLE[@]}"
fi

if [[ $SET_HOME -eq 1 ]]; then
  if grep -qx "com.cleanhome.tv" <<<"$INSTALLED"; then
    echo "Clean Home will be set as the default launcher."
  else
    echo "ERROR: Clean Home (com.cleanhome.tv) is not installed. Install the launcher APK first:" >&2
    echo "  adb -s $TARGET install launcher/app/build/outputs/apk/release/app-release.apk" >&2
    exit 1
  fi
fi

if [[ $APPLY -eq 0 ]]; then
  echo
  echo "Dry run only. Re-run with --apply to make these changes."
  exit 0
fi

touch "$STATE"
for pkg in ${TO_DISABLE[@]+"${TO_DISABLE[@]}"}; do
  if A pm disable-user --user 0 "$pkg" 2>&1 | grep -q "new state: disabled"; then
    echo "disabled  $pkg"
    echo "disabled $pkg" >>"$STATE"
  else
    echo "skipped   $pkg (device refused; not uninstalling)"
  fi
done

if [[ $SET_HOME -eq 1 ]]; then
  PREV_HOME="$(A cmd package resolve-activity --brief -a android.intent.action.MAIN \
    -c android.intent.category.HOME | tr -d '\r' | tail -n 1)"
  A cmd package set-home-activity "$HOME_ACTIVITY" >/dev/null
  echo "default launcher set to $HOME_ACTIVITY"
  # Don't overwrite the real original launcher if this is a re-run.
  if [[ "$PREV_HOME" != com.cleanhome.tv/* ]] && ! grep -q '^home ' "$STATE"; then
    echo "home $PREV_HOME" >>"$STATE"
  fi
fi

sort -u "$STATE" -o "$STATE"
echo
echo "Done. Press Home on your remote. To undo everything: ./tv-restore.sh $TV_IP"
