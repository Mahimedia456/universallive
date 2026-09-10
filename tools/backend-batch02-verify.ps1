$ErrorActionPreference = "Stop"

$base = "http://127.0.0.1:3000/api/v1"

Write-Host "UniversalLive Backend Batch 02 verification" -ForegroundColor Cyan

Invoke-RestMethod "$base/foundation" | ConvertTo-Json -Depth 6
Write-Host "[OK] foundation" -ForegroundColor Green

Invoke-RestMethod "$base/auth/mobile/status" | ConvertTo-Json -Depth 6
Write-Host "[OK] auth/mobile/status" -ForegroundColor Green

Write-Host ""
Write-Host "Authenticated profile/device endpoints require a real Bearer token." -ForegroundColor Yellow
Write-Host "Endpoints:"
Write-Host "  GET  $base/profiles/me"
Write-Host "  PUT  $base/profiles/me"
Write-Host "  POST $base/devices/register"
Write-Host "  GET  $base/onboarding/me"
Write-Host "  PUT  $base/onboarding/me"
