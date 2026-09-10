$ErrorActionPreference = 'Stop'
$base = 'https://universallive.vercel.app/api/v1'

Write-Host '=== Batch 02 Public Endpoint Check ===' -ForegroundColor Cyan
Invoke-RestMethod "$base/health" | ConvertTo-Json -Depth 5
Invoke-RestMethod "$base/auth/mobile/status" | ConvertTo-Json -Depth 5

Write-Host ''
Write-Host 'Protected Batch 02 endpoints are intentionally authenticated:' -ForegroundColor Yellow
Write-Host 'GET    /streaming/connections'
Write-Host 'POST   /streaming/connections'
Write-Host 'POST   /streaming/connections/:id/test'
Write-Host 'POST   /streaming/rtmp'
Write-Host 'GET    /studio/scenes'
Write-Host 'POST   /studio/scenes'
Write-Host 'POST   /studio/scenes/:id/duplicate'
Write-Host 'GET    /studio/scenes/:id/sources'
Write-Host ''
Write-Host 'Validate them from the signed-in Android app.' -ForegroundColor Green
