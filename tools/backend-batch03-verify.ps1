$ErrorActionPreference = 'Stop'

$base = 'http://127.0.0.1:3000/api/v1'

Write-Host 'UniversalLive Backend Batch 03 verification' -ForegroundColor Cyan

Invoke-RestMethod "$base/foundation" | ConvertTo-Json -Depth 6
Write-Host '[OK] Foundation' -ForegroundColor Green

Invoke-RestMethod "$base/auth/mobile/status" | ConvertTo-Json -Depth 6
Write-Host '[OK] Batch 02 auth module repaired/available' -ForegroundColor Green

Write-Host ''
Write-Host 'Authenticated Phase 05-07 endpoints:' -ForegroundColor Yellow
Write-Host "GET    $base/streaming/connections"
Write-Host "POST   $base/streaming/connections"
Write-Host "PATCH  $base/streaming/connections/{id}"
Write-Host "DELETE $base/streaming/connections/{id}"
Write-Host "POST   $base/streaming/connections/{id}/test"
Write-Host "POST   $base/streaming/oauth/{platform}/start"
Write-Host "GET    $base/streaming/oauth/{platform}/channels"
Write-Host "POST   $base/streaming/oauth/connections/{id}/select-channel"
Write-Host "POST   $base/streaming/rtmp"
Write-Host "GET    $base/streaming/rtmp/{connectionId}/status"
