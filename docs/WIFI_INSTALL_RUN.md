# Install and run over Wi-Fi ADB

The phone used during development has IP 192.168.100.25. The wireless-debugging port can change after reconnect/reboot, so always use the current port displayed on the phone.

## 1. Verify existing Wi-Fi connection
```powershell
adb devices
```
Expected:
```text
192.168.100.25:<port>    device
```

## 2. Reconnect if needed
```powershell
adb connect 192.168.100.25:<CURRENT_WIRELESS_DEBUGGING_PORT>
adb devices
```

## 3. Build + install from terminal
If Gradle is available in PATH:
```powershell
cd E:\UniversalLive
gradle :androidApp:installDebug
```

If using Android Studio, select the Wi-Fi device and press Run. Android Studio performs the Gradle build and adb install automatically.

## 4. Launch app from terminal
```powershell
adb shell monkey -p com.universallive.app -c android.intent.category.LAUNCHER 1
```

## 5. Stop app
```powershell
adb shell am force-stop com.universallive.app
```

## 6. Reinstall an already-built APK manually
```powershell
adb install -r .\androidApp\build\outputs\apk\debug\androidApp-debug.apk
adb shell monkey -p com.universallive.app -c android.intent.category.LAUNCHER 1
```

## 7. Useful live logs
```powershell
adb logcat | Select-String "UniversalLive|MediaCodec|MediaProjection|Rtmp"
```
