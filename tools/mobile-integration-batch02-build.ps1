$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$apk = Join-Path $root 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'

Write-Host '=== UniversalLive Mobile Integration Batch 02 ===' -ForegroundColor Cyan
Write-Host 'Batch 1 compile fix + Connections + Studio cloud sync' -ForegroundColor Cyan

Write-Host ''
Write-Host 'Checking backend...' -ForegroundColor Yellow
$health = Invoke-RestMethod 'https://universallive.vercel.app/api/v1/health'
if (-not $health.ok) { throw 'Backend health check failed' }
Write-Host '[OK] Vercel backend reachable' -ForegroundColor Green

Set-Location $root

Write-Host ''
Write-Host 'Android compile/build...' -ForegroundColor Yellow
.\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) {
    throw 'Android debug build failed'
}

if (-not (Test-Path $apk)) { throw "APK not generated: $apk" }

Write-Host ''
Write-Host 'ANDROID DEBUG BUILD SUCCESSFUL' -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
