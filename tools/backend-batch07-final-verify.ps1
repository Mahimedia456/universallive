$ErrorActionPreference = 'Stop'
$base = 'http://127.0.0.1:3000/api/v1'

Invoke-RestMethod "$base/foundation" | Out-Null
Write-Host '[OK] Foundation' -ForegroundColor Green

Invoke-RestMethod "$base/security/status" | ConvertTo-Json -Depth 8
Write-Host '[OK] Security' -ForegroundColor Green

Invoke-RestMethod "$base/system/final-status" | ConvertTo-Json -Depth 8
Write-Host '[OK] Final status' -ForegroundColor Green

Invoke-RestMethod "$base/billing/plans" | ConvertTo-Json -Depth 8
Write-Host '[OK] Plans' -ForegroundColor Green
