# Kutu Home

Minimal Android TV / Google TV launcher.

## Build on GitHub
Actions -> Build Kutu Home APK -> Run workflow.

Download artifact: `Kutu-Home-APK`.

## Install
```powershell
$ADB="C:\platform-tools\adb.exe"
& $ADB install -r "app-debug.apk"
```

## Test first
Launch without changing the stock launcher:
```powershell
& $ADB shell monkey -p local.kutu.home 1
```

## Set as HOME
Only after testing:
```powershell
& $ADB shell cmd package set-home-activity local.kutu.home/.HomeActivity
```

## Rollback
```powershell
& $ADB shell cmd package set-home-activity com.google.android.apps.tv.launcherx/.home.HomeActivity
& $ADB shell pm enable --user 0 com.google.android.apps.tv.launcherx
& $ADB shell pm enable --user 0 com.google.android.tungsten.setupwraith
```

Do **not** disable the stock Google TV launcher until Kutu Home is tested and ADB is confirmed working.
