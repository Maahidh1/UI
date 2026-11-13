# Sourced by tv-clean.sh and tv-check.sh. Packages that must never be disabled:
# keyboard/input, Play Store, Play services, Bluetooth, Wi-Fi, remote control,
# settings, updates, and core system components.
PROTECTED_RE='^android$|^com\.android\.(systemui|settings|tv\.settings|providers\.|bluetooth|phone|shell|packageinstaller|permissioncontroller|externalstorage|networkstack|wifi|inputdevices|keychain|location|server\.telecom|vending)|^com\.google\.android\.(gms|gsf|gsf\.login|vending|packageinstaller|permissioncontroller|tv\.remote|tvrecommendations\.core|ext\.services|ext\.shared|modulemetadata|networkstack|tungsten|apps\.tv\.launcherx|tvlauncher|leanbacklauncher$|katniss|configupdater|webview|tv\.bugreportsender)|tvrecommendations|keyboard|inputmethod|ime$|\.ime\.|latin|gboard|input|remote|rcu|bluetooth|\.bt\.|wifi|wlan|ota|update|upgrade|settings|setupwizard|setupwraith|\.setup$|provision|webview|launcher'

is_protected() { grep -Eiq "$PROTECTED_RE" <<<"$1"; }
