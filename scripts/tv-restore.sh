#!/usr/bin/env bash
# Undo everything tv-clean.sh changed, using tv-clean.state.
# Usage: ./tv-restore.sh <TV_IP>

set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
STATE="$DIR/tv-clean.state"
TV_IP="${1:?Usage: ./tv-restore.sh <TV_IP>}"
TARGET="$TV_IP"
[[ "$TARGET" == *:* ]] || TARGET="$TARGET:5555"

[[ -f "$STATE" ]] || { echo "No tv-clean.state found; nothing to restore."; exit 0; }

adb connect "$TARGET" >/dev/null
adb -s "$TARGET" wait-for-device
# </dev/null: stop adb from eating the stdin of surrounding read loops.
A() { adb -s "$TARGET" shell "$@" </dev/null; }

while read -r action pkg; do
  case "$action" in
    disabled)    A pm enable --user 0 "$pkg" >/dev/null && echo "enabled   $pkg" ;;
    uninstalled) A cmd package install-existing --user 0 "$pkg" >/dev/null && echo "restored  $pkg" ;;
    home)
      # pkg holds the home activity that was active before tv-clean.sh ran.
      if [[ "$pkg" == */* && "$pkg" != android/* ]]; then
        A cmd package set-home-activity "$pkg" >/dev/null
        echo "default launcher restored to $pkg"
      else
        echo "previous launcher unknown; press Home and pick your stock launcher"
      fi ;;
  esac
done <"$STATE"

rm -f "$STATE"
echo "Restore complete."
