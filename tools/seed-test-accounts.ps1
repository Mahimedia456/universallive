$ErrorActionPreference = 'Stop'

Set-Location 'E:\UniversalLive'

Write-Host '=== Universal Live Seed Repair ===' -ForegroundColor Cyan
Write-Host 'Supports new sb_secret_* and legacy service_role JWT keys.' -ForegroundColor Cyan
Write-Host ''

node .\tools\seed-test-accounts.mjs

if ($LASTEXITCODE -ne 0) {
    throw 'Universal Live test-account seed failed'
}
