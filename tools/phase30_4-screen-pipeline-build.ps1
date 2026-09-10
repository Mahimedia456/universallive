$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"

Write-Host "=== UniversalLive Phase 30.4 Screen Pipeline Build ===" -ForegroundColor Cyan
Write-Host "Project: $root"

Set-Location $root

Write-Host ""
Write-Host "Using already-merged Phase 30.4 source files." -ForegroundColor Yellow

$required = @(
    "androidApp\src\main\kotlin\com\universallive\app\streaming\StreamCompositor.kt",
    "androidApp\src\main\kotlin\com\universallive\app\streaming\ExternalTextureRenderer.kt",
    "androidApp\src\main\kotlin\com\universallive\app\streaming\EglWindow.kt"
)

foreach ($relative in $required) {
    $full = Join-Path $root $relative
    if (-not (Test-Path $full)) {
        throw "Missing required Phase 30.4 file: $full"
    }
    Write-Host "Found: $relative" -ForegroundColor DarkGray
}

Write-Host ""
Write-Host "Cleaning Android build..." -ForegroundColor Yellow
& .\gradlew.bat clean
if ($LASTEXITCODE -ne 0) {
    throw "Gradle clean failed"
}

Write-Host ""
Write-Host "Building debug APK..." -ForegroundColor Yellow
& .\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw "Android build failed"
}

Write-Host ""
Write-Host "Build completed successfully." -ForegroundColor Green
Write-Host ""

$apk = Join-Path $root "androidApp\build\outputs\apk\debug\androidApp-debug.apk"

if (Test-Path $apk) {
    Get-Item $apk | Select-Object FullName, Length, LastWriteTime
} else {
    Write-Host "Default APK path not found. Searching project..." -ForegroundColor Yellow
    Get-ChildItem $root -Recurse -Filter *.apk | Select-Object FullName, Length, LastWriteTime
}