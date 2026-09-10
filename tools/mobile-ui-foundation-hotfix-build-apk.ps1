$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"
$apk = Join-Path $root "androidApp\build\outputs\apk\debug\androidApp-debug.apk"

Write-Host "=== UniversalLive Mobile UI Foundation Hotfix ===" -ForegroundColor Cyan
Write-Host "Canonical app-icon-512 + left header + scroll fixes" -ForegroundColor Cyan

Set-Location $root

Write-Host "Building Android debug APK..." -ForegroundColor Yellow
& .\gradlew.bat :androidApp:assembleDebug

if ($LASTEXITCODE -ne 0) {
    throw "Android APK build failed"
}

if (-not (Test-Path $apk)) {
    throw "APK not found: $apk"
}

Write-Host ""
Write-Host "APK BUILD SUCCESSFUL" -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
