$ErrorActionPreference = 'Stop'

$base = 'http://127.0.0.1:3000/api/v1'

Write-Host 'UniversalLive Backend Batch 05 verification' -ForegroundColor Cyan

Invoke-RestMethod "$base/foundation" | ConvertTo-Json -Depth 6
Write-Host '[OK] Foundation' -ForegroundColor Green

Write-Host ''
Write-Host 'Authenticated Phase 11-14 endpoints:' -ForegroundColor Yellow
Write-Host "GET   $base/stream/configs"
Write-Host "POST  $base/stream/configs"
Write-Host "PATCH $base/stream/configs/{id}"
Write-Host "POST  $base/streams/sessions"
Write-Host "GET   $base/streams/sessions/{id}"
Write-Host "POST  $base/streams/sessions/{id}/start"
Write-Host "POST  $base/streams/sessions/{id}/heartbeat"
Write-Host "POST  $base/streams/sessions/{id}/end"
Write-Host "PATCH $base/streams/sessions/{sessionId}/destinations/{destinationId}"
Write-Host "POST  $base/streams/sessions/{sessionId}/telemetry"
Write-Host "POST  $base/streams/sessions/{sessionId}/events"
Write-Host "GET   $base/streams/sessions/{sessionId}/health"
Write-Host "GET   $base/streams/history"
Write-Host "GET   $base/streams/history/{sessionId}"
Write-Host "POST  $base/streams/history/{sessionId}/finalize"
