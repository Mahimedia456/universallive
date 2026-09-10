$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"
$apk = Join-Path $root "androidApp\build\outputs\apk\debug\androidApp-debug.apk"

Write-Host "=== UniversalLive Phase 30.6 RTMP Real-time Fix ===" -ForegroundColor Cyan

Set-Location $root

Write-Host "[1/2] Building debug APK..." -ForegroundColor Yellow
& .\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw "Android build failed"
}

if (-not (Test-Path $apk)) {
    throw "APK not found: $apk"
}

Write-Host "[2/2] APK ready:" -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
