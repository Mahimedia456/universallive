$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$apk = Join-Path $root 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'

Write-Host '=== UniversalLive Mobile Integration Batch 03.1 Build Repair ===' -ForegroundColor Cyan
Write-Host 'Fix: CaptureController reference + Notifications route signature' -ForegroundColor Cyan

Set-Location $root

Write-Host ''
Write-Host 'Compiling Android common/mobile sources...' -ForegroundColor Yellow
.\gradlew.bat :composeApp:compileAndroidMain
if ($LASTEXITCODE -ne 0) {
    throw 'composeApp Android compilation failed'
}

Write-Host ''
Write-Host 'Building Android debug APK...' -ForegroundColor Yellow
.\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw 'Android debug APK build failed'
}

if (-not (Test-Path $apk)) {
    throw "APK was not generated: $apk"
}

Write-Host ''
Write-Host 'ANDROID BUILD SUCCESSFUL' -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
