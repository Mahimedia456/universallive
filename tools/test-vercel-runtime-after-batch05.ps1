$ErrorActionPreference = 'Stop'

$base = 'https://universallive.vercel.app/api/v1'

Write-Host '=== UniversalLive Vercel Runtime Check ===' -ForegroundColor Cyan

$health = Invoke-RestMethod "$base/health"
Write-Host '[OK] /health'
$health | ConvertTo-Json -Depth 5

$runtime = Invoke-RestMethod "$base/admin-console/runtime-status"
Write-Host '[OK] /admin-console/runtime-status'
$runtime | ConvertTo-Json -Depth 5

Write-Host ''
Write-Host 'Testing admin CORS preflight...' -ForegroundColor Yellow

$response = Invoke-WebRequest `
  -Uri "$base/admin-console/login" `
  -Method Options `
  -Headers @{
    Origin = 'http://localhost:5173'
    'Access-Control-Request-Method' = 'POST'
    'Access-Control-Request-Headers' = 'content-type'
  } `
  -SkipHttpErrorCheck

Write-Host "Status: $($response.StatusCode)"
Write-Host "Allow-Origin: $($response.Headers['Access-Control-Allow-Origin'])"

if (-not $response.Headers['Access-Control-Allow-Origin']) {
    throw 'CORS header missing'
}

Write-Host ''
Write-Host 'VERCEL RUNTIME + CORS OK' -ForegroundColor Green
