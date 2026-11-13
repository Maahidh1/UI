# Clean Android TV Home Screen

Two tools for decluttering an Android TV / Google TV home screen:

| Folder | What it does |
| --- | --- |
| [`scripts/`](scripts) | ADB scripts that turn off recommendation rows and preinstalled clutter. Fully reversible. |
| [`launcher/`](launcher) | **Clean Home**, a minimal replacement home screen: a clock and a grid of your apps. No ads, no "sponsored" rows. |

You can use either one on its own. For the cleanest result, use both.

---

## 1. Turn on debugging on the TV (one time)

1. **Settings → Device Preferences → About** (or **System → About**), then select **Build** 7 times to unlock Developer options.
2. **Settings → Device Preferences → Developer options**, then turn on **USB debugging** and, if listed, **Network debugging**.
3. Find the TV's IP address under **Settings → Network & Internet → (your network)**.
4. On your computer, install [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) so that `adb` is on your PATH.

The first time you connect, the TV shows an "Allow USB debugging?" prompt. Accept it.

## 2. Build and install the Clean Home launcher (optional)

Open `launcher/` in Android Studio and select **Build → Build APK(s)**, or run:

```bash
cd launcher
./gradlew assembleRelease      # needs the Android SDK (Android Studio installs it)
adb connect <TV_IP>:5555
adb install app/build/outputs/apk/release/app-release.apk
```

**Using it:**
- **OK** opens an app.
- **Long-press OK** or the **Menu** key on an app gives you: *Move to front*, *Hide from home*, *App info*.
- A **Settings** tile opens system settings, so you never need the stock launcher.
- A **Show N hidden** tile appears when you have hidden apps. Select it to bring them all back.
- Pressing **Home** while already on the home screen jumps back to the first tile.

## 3. Run the cleanup script

```bash
cd scripts
./tv-clean.sh <TV_IP>                        # dry run: lists what would change
./tv-clean.sh <TV_IP> --apply                # disable the clutter
./tv-clean.sh <TV_IP> --apply --set-home     # also make Clean Home the default home screen
```

Edit [`scripts/packages.txt`](scripts/packages.txt) to choose what gets disabled. Comment out anything you want to keep, and add your TV maker's bloat. To list every package on the TV, run `adb shell pm list packages`.

The stock launchers are never disabled. Disabling them without a working replacement can leave the TV with no home screen.

### Undo everything

```bash
./tv-restore.sh <TV_IP>
```

This re-enables every package the script turned off and restores your original home screen. It uses `scripts/tv-clean.state`, which is written when you run `tv-clean.sh --apply`.
