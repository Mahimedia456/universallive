$ErrorActionPreference = 'Stop'
$root = 'E:\UniversalLive'
$package = 'com.universallive.app'
$activity = '.MainActivity'
$apk = Join-Path $root 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'

Write-Host '=== UniversalLive Phase 30.5 Professional Screen Capture ===' -ForegroundColor Cyan
Set-Location $root

Write-Host '[1/5] Building fresh debug APK...' -ForegroundColor Yellow
& .\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
if (-not (Test-Path $apk)) { throw "APK not found: $apk" }

Write-Host '[2/5] Detecting connected ADB device...' -ForegroundColor Yellow
$lines = (& adb devices) | Select-String "\sdevice$" | ForEach-Object { ($_ -split '\s+')[0] }
if (-not $lines -or $lines.Count -eq 0) {
    throw 'No ADB device connected. Pair/connect Wireless debugging first.'
}
$serial = ($lines | Where-Object { $_ -like '*_adb-tls-connect._tcp*' } | Select-Object -First 1)
if (-not $serial) { $serial = $lines | Select-Object -First 1 }
Write-Host "Using device: $serial" -ForegroundColor Green

Write-Host '[3/5] Removing old UniversalLive...' -ForegroundColor Yellow
& adb -s $serial uninstall $package | Out-Host

Write-Host '[4/5] Installing fresh APK...' -ForegroundColor Yellow
& adb -s $serial install -r -g $apk
if ($LASTEXITCODE -ne 0) { throw 'APK install failed' }

Write-Host '[5/5] Launching UniversalLive...' -ForegroundColor Yellow
& adb -s $serial shell am force-stop $package
& adb -s $serial shell am start -n "$package/$activity"
if ($LASTEXITCODE -ne 0) { throw 'App launch failed' }

Write-Host ''
Write-Host 'UniversalLive installed and launched.' -ForegroundColor Green
Write-Host 'For the first verification: disable Facecam and remove overlays, choose Entire device, then Go Live.' -ForegroundColor Cyan
Write-Host 'If video is still black, run:' -ForegroundColor Cyan
Write-Host "adb -s `"$serial`" logcat -c"
Write-Host "adb -s `"$serial`" logcat | findstr /I `"UniversalLiveCapture MediaProjection MediaCodec VirtualDisplay EGL OpenGL`""
