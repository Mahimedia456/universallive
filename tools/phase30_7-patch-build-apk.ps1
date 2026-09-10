$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"
$file = Join-Path $root "androidApp\src\main\kotlin\com\universallive\app\streaming\ScreenCaptureService.kt"

if (-not (Test-Path $file)) {
    throw "ScreenCaptureService.kt not found: $file"
}

Write-Host "=== UniversalLive Phase 30.7.1 Repair ===" -ForegroundColor Cyan
Write-Host "Fixing literal backtick-r-backtick-n sequences in ScreenCaptureService.kt..." -ForegroundColor Yellow

$text = Get-Content $file -Raw

$before = $text

# Fix only the accidental literal PowerShell newline escape sequences that were written into Kotlin.
$text = $text.Replace('`r`n', [Environment]::NewLine)
$text = $text.Replace('`n', [Environment]::NewLine)

Set-Content -Path $file -Value $text -Encoding UTF8

if ($before -eq $text) {
    Write-Host "No literal newline escape sequences were found. Continuing with build..." -ForegroundColor DarkYellow
} else {
    Write-Host "Broken literal newline sequences repaired." -ForegroundColor Green
}

Write-Host ""
Write-Host "Checking suspicious lines..." -ForegroundColor Yellow
Select-String -Path $file -Pattern "publishTarget =|renderOverlays =|useCompositor =" |
    ForEach-Object { Write-Host ("  " + $_.LineNumber + ": " + $_.Line.Trim()) }

Write-Host ""
Write-Host "Building Android debug APK..." -ForegroundColor Yellow
Set-Location $root

& .\gradlew.bat :androidApp:assembleDebug

if ($LASTEXITCODE -ne 0) {
    throw "Android APK build failed"
}

$apk = Join-Path $root "androidApp\build\outputs\apk\debug\androidApp-debug.apk"

if (-not (Test-Path $apk)) {
    throw "Build completed but APK not found: $apk"
}

Write-Host ""
Write-Host "APK BUILD SUCCESSFUL" -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
