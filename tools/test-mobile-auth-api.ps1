$ErrorActionPreference = 'Stop'

$base = 'https://universallive.vercel.app/api/v1'

Write-Host '=== UniversalLive API Contract Test ===' -ForegroundColor Cyan

Invoke-RestMethod "$base/health" | ConvertTo-Json -Depth 5
Invoke-RestMethod "$base/auth/mobile/status" | ConvertTo-Json -Depth 5
Invoke-RestMethod "$base/billing/plans" | ConvertTo-Json -Depth 6

Write-Host ''
Write-Host '[OK] Public mobile API contract is reachable.' -ForegroundColor Green
Write-Host 'Authenticated account endpoints are tested from the app after login.'
