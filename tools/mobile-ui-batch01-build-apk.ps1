$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"
$apk = Join-Path $root "androidApp\build\outputs\apk\debug\androidApp-debug.apk"

Write-Host "=== UniversalLive Mobile UI Batch 01 ===" -ForegroundColor Cyan
Write-Host "Phase 01-03 shared UI + routes + theme" -ForegroundColor DarkCyan

Set-Location $root

Write-Host "Building Android debug APK..." -ForegroundColor Yellow
& .\gradlew.bat :androidApp:assembleDebug

if ($LASTEXITCODE -ne 0) {
    throw "Android APK build failed"
}

if (-not (Test-Path $apk)) {
    throw "Build completed but APK was not found: $apk"
}

Write-Host "" 
Write-Host "APK BUILD SUCCESSFUL" -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
