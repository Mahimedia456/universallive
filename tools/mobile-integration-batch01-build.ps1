$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$apk = Join-Path $root 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'

Write-Host '=== UniversalLive Mobile Integration Batch 01 ===' -ForegroundColor Cyan
Write-Host 'API: https://universallive.vercel.app/api/v1' -ForegroundColor Cyan

Write-Host ''
Write-Host 'Checking public backend...' -ForegroundColor Yellow
$health = Invoke-RestMethod 'https://universallive.vercel.app/api/v1/health'
if (-not $health.ok) {
    throw 'UniversalLive Vercel backend health check failed'
}
Write-Host '[OK] Backend health' -ForegroundColor Green

Set-Location (Join-Path $root 'backend')
Write-Host ''
Write-Host 'Building backend auth recovery patch...' -ForegroundColor Yellow
npm run build
if ($LASTEXITCODE -ne 0) {
    throw 'Backend build failed'
}

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
Write-Host 'MOBILE INTEGRATION BATCH 01 BUILD SUCCESSFUL' -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
