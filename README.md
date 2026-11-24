# Apna Launcher

A complete, fully customizable Android HOME launcher built with **Kotlin + Jetpack Compose**.

- `minSdk 26`, `targetSdk 34`
- MVVM + Room (layout, folders, hidden/locked apps) + DataStore (settings)
- Registered as HOME launcher (`MAIN` + `HOME` + `DEFAULT`), `launchMode="singleTask"`
- Transparent window so the device wallpaper stays visible
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## Features

**Home** — 5 swipeable pages with dots, 5-app customizable dock, drag & drop with
animations, folders (drop one app on another), AppWidgetHost widgets (add/resize/remove),
long-press menu (Wallpaper, Widgets, Themes, Settings), gestures (swipe up drawer,
swipe down notifications, double-tap lock, pinch edit mode).

**Drawer** — A-Z list with fast-scroll alphabet strip, live search, All/Social/Games/Tools
tabs, long-press menu (App Info, Uninstall, Hide, Add to Home, Lock, Edit name/icon,
LauncherApps shortcuts).

**Notifications** — NotificationListenerService, explanation screen, badges on icons,
in-launcher panel with tap-to-open and swipe-to-dismiss.

**Customization** — icon packs (Nova/ADW `appfilter.xml`), 5 icon shapes, icon and label
size sliders, hide labels, per-app name/icon change, home/drawer grid sizes, Light/Dark/
System themes, Material You, accent picker, AMOLED/Glass/Neon/Minimal presets, blur dock
and drawer, fonts, animation speed, JSON export/import.

**Settings** — Home, Drawer, Icons, Themes, Gestures, Notifications, Backup/Restore, About.

## Build

```bash
cd /root/projects/ApnaLauncher
./gradlew assembleDebug
```
APK: `app/build/outputs/apk/debug/app-debug.apk`

Install on a device/emulator:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Notification access and app shortcuts require the user to grant notification
> listener access and set Apna Launcher as the default HOME app.
