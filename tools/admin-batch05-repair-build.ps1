$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'

Write-Host '=== UniversalLive Admin Batch 05 Repair ===' -ForegroundColor Cyan
Write-Host 'DI + CORS TS + Vercel runtime + schema alignment' -ForegroundColor Cyan

Set-Location (Join-Path $root 'backend')

Write-Host ''
Write-Host 'Building backend...' -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    throw 'Backend build failed'
}

Set-Location (Join-Path $root 'apps\admin')

if (-not (Test-Path 'node_modules')) {
    npm install
    if ($LASTEXITCODE -ne 0) {
        throw 'Admin npm install failed'
    }
}

Write-Host ''
Write-Host 'Building admin frontend...' -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    throw 'Admin frontend build failed'
}

Write-Host ''
Write-Host 'ADMIN BATCH 05 BUILD SUCCESSFUL' -ForegroundColor Green
