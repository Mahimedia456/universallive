$ErrorActionPreference = "Stop"
$root = "E:\UniversalLive"
Set-Location $root
Write-Host "=== UniversalLive Mobile UI Batch 02 ===" -ForegroundColor Cyan
Write-Host "Building Android debug APK only..." -ForegroundColor Yellow
& .\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Android build failed" }
$apk = Join-Path $root "androidApp\build\outputs\apk\debug\androidApp-debug.apk"
if (-not (Test-Path $apk)) { throw "APK not found: $apk" }
Write-Host "APK BUILD SUCCESSFUL" -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
