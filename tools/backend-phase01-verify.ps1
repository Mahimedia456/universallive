$ErrorActionPreference = "Stop"

$base = "http://127.0.0.1:3000/api/v1"

Write-Host "UniversalLive Backend Phase 01 verification" -ForegroundColor Cyan

$health = Invoke-RestMethod "$base/health"
Write-Host "[OK] Existing health endpoint responded" -ForegroundColor Green

$foundation = Invoke-RestMethod "$base/foundation"
Write-Host "[OK] Foundation endpoint responded" -ForegroundColor Green
$foundation | ConvertTo-Json -Depth 8

$readiness = Invoke-RestMethod "$base/foundation/readiness"
Write-Host "[OK] Readiness endpoint responded" -ForegroundColor Green
$readiness | ConvertTo-Json -Depth 8
