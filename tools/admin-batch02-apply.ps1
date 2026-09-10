$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'

Write-Host '=== UniversalLive Admin Batch 02 ===' -ForegroundColor Cyan
Write-Host 'Creators + Memberships + Broadcasts + Connections + Support' -ForegroundColor Cyan

Set-Location (Join-Path $root 'backend')
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

npm run build
if ($LASTEXITCODE -ne 0) {
    throw 'Admin frontend build failed'
}

Write-Host ''
Write-Host 'ADMIN BATCH 02 BUILD SUCCESSFUL' -ForegroundColor Green
