$ErrorActionPreference = 'Stop'

$base = 'https://universallive.vercel.app/api/v1'

Write-Host '=== UniversalLive Final Public API Check ===' -ForegroundColor Cyan

Invoke-RestMethod "$base/health" | ConvertTo-Json -Depth 5
Invoke-RestMethod "$base/foundation" | ConvertTo-Json -Depth 6
Invoke-RestMethod "$base/billing/plans" | ConvertTo-Json -Depth 8
Invoke-RestMethod "$base/security/status" | ConvertTo-Json -Depth 6
Invoke-RestMethod "$base/system/final-status" | ConvertTo-Json -Depth 6

Write-Host ''
Write-Host '[OK] Public API surface is reachable.' -ForegroundColor Green
Write-Host 'Authenticated live/history/notifications/support endpoints are validated from the signed-in APK.'
