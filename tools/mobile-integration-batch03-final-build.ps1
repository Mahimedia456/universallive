$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$apk = Join-Path $root 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'

Write-Host '=== UniversalLive FINAL Mobile Integration Batch 03 ===' -ForegroundColor Cyan
Write-Host 'Live + Activity + Billing refresh + Notifications + Support' -ForegroundColor Cyan

Write-Host ''
Write-Host 'Checking production API...' -ForegroundColor Yellow
$health = Invoke-RestMethod 'https://universallive.vercel.app/api/v1/health'
if (-not $health.ok) {
    throw 'Production backend is not healthy'
}
Write-Host '[OK] Production backend online' -ForegroundColor Green

Set-Location $root

Write-Host ''
Write-Host 'Building Android debug APK...' -ForegroundColor Yellow
.\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw 'Android debug build failed'
}

if (-not (Test-Path $apk)) {
    throw "APK not found: $apk"
}

Write-Host ''
Write-Host 'FINAL MOBILE INTEGRATION BUILD SUCCESSFUL' -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
