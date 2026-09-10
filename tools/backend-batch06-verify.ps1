$ErrorActionPreference = 'Stop'

$base = 'http://127.0.0.1:3000/api/v1'

Write-Host 'UniversalLive Backend Batch 06 verification' -ForegroundColor Cyan

Invoke-RestMethod "$base/foundation" | ConvertTo-Json -Depth 6
Write-Host '[OK] foundation' -ForegroundColor Green

$plans = Invoke-RestMethod "$base/billing/plans"
Write-Host '[OK] public plan endpoint' -ForegroundColor Green
$plans | ConvertTo-Json -Depth 8

Write-Host ''
Write-Host 'Authenticated Phase 15-18 endpoints:' -ForegroundColor Yellow
Write-Host "GET  $base/billing/entitlements/me"
Write-Host "POST $base/billing/purchases/verify"
Write-Host "GET  $base/billing/purchases/me"
Write-Host "POST $base/billing/purchases/restore"
Write-Host "GET  $base/notifications"
Write-Host "PATCH $base/notifications/{id}/read"
Write-Host "POST $base/notifications/read-all"
Write-Host "GET  $base/support/tickets"
Write-Host "POST $base/support/tickets"
Write-Host "GET  $base/support/tickets/{id}"
Write-Host "POST $base/support/tickets/{id}/messages"
