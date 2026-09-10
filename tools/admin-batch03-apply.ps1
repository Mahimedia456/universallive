$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'

Write-Host '=== UniversalLive Admin Batch 03 ===' -ForegroundColor Cyan
Write-Host 'Plans + Notifications + Audit + System Operations' -ForegroundColor Cyan

Set-Location (Join-Path $root 'backend')
npm run build
if ($LASTEXITCODE -ne 0) {
    throw 'Backend build failed'
}

Set-Location (Join-Path $root 'apps\admin')
npm run build
if ($LASTEXITCODE -ne 0) {
    throw 'Admin frontend build failed'
}

Write-Host ''
Write-Host 'ADMIN BATCH 03 BUILD SUCCESSFUL' -ForegroundColor Green
