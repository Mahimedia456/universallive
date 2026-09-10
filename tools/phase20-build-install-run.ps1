$ErrorActionPreference = "Stop"
Set-Location (Split-Path -Parent $PSScriptRoot)

Write-Host "[1/7] Checking Wi-Fi ADB device..."
$devices = adb devices
$devices | Write-Host
if (($devices | Select-String "\tdevice$").Count -lt 1) {
  throw "No authorized ADB device found. Keep Wireless debugging enabled and reconnect first."
}

$gradleHome = Join-Path $env:TEMP "gradle-9.6.1"
$gradleBat = Join-Path $gradleHome "bin\gradle.bat"

if (!(Test-Path ".\gradlew.bat")) {
  Write-Host "[2/7] Gradle wrapper missing; generating it from $gradleBat"
  if (!(Test-Path $gradleBat)) {
    throw "Gradle 9.6.1 not found at $gradleBat. Re-run the Gradle 9.6.1 download/extract command used earlier."
  }
  & $gradleBat wrapper --gradle-version 9.6.1
  if ($LASTEXITCODE -ne 0) { throw "Gradle wrapper generation failed." }
} else {
  Write-Host "[2/7] Existing Gradle wrapper found."
}

Write-Host "[3/7] Gradle configuration check..."
& .\gradlew.bat help
if ($LASTEXITCODE -ne 0) { throw "Gradle configuration check failed." }

Write-Host "[4/7] Building debug APK..."
& .\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Debug build failed." }

Write-Host "[5/7] Installing on connected Wi-Fi ADB device..."
& .\gradlew.bat :androidApp:installDebug
if ($LASTEXITCODE -ne 0) { throw "APK installation failed." }

Write-Host "[6/7] Launching UniversalLive..."
adb shell am force-stop com.universallive.app | Out-Null
adb shell am start -n com.universallive.app/.MainActivity

Write-Host "[7/7] Done. For focused logs run:"
Write-Host 'adb logcat | Select-String "UniversalLive|MediaProjection|MediaCodec|Rtmp|AudioRecord|EGL"'
