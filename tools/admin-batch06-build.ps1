$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'

Write-Host '=== UniversalLive Admin Batch 06 ===' -ForegroundColor Cyan
Write-Host 'Operations + Search/Filters + Responsive UX + QA Script Fix' -ForegroundColor Cyan

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
Write-Host 'ADMIN BATCH 06 BUILD SUCCESSFUL' -ForegroundColor Green
