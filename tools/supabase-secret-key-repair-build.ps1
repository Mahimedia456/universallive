$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$backend = Join-Path $root 'backend'

Write-Host '=== Universal Live Supabase Secret-Key Repair ===' -ForegroundColor Cyan

Set-Location $backend
npm run build

if ($LASTEXITCODE -ne 0) {
    throw 'Backend build failed after Supabase secret-key repair'
}

Write-Host ''
Write-Host 'BACKEND BUILD SUCCESSFUL' -ForegroundColor Green
Write-Host 'Now run:'
Write-Host '  cd E:\UniversalLive'
Write-Host '  .\tools\seed-test-accounts.ps1'
