$ErrorActionPreference = 'Stop'

$base = 'https://universallive.vercel.app/api/v1'

Write-Host '=== UniversalLive Vercel Runtime Check - Batch 06 ===' -ForegroundColor Cyan

$health = Invoke-RestMethod "$base/health"
Write-Host '[OK] /health' -ForegroundColor Green
$health | ConvertTo-Json -Depth 5

$runtime = Invoke-RestMethod "$base/admin-console/runtime-status"
Write-Host '[OK] /admin-console/runtime-status' -ForegroundColor Green
$runtime | ConvertTo-Json -Depth 5

Write-Host ''
Write-Host 'Testing admin CORS preflight...' -ForegroundColor Yellow

try {
    $response = Invoke-WebRequest `
        -Uri "$base/admin-console/login" `
        -Method Options `
        -Headers @{
            Origin = 'http://localhost:5173'
            'Access-Control-Request-Method' = 'POST'
            'Access-Control-Request-Headers' = 'content-type'
        } `
        -UseBasicParsing
} catch {
    $response = $_.Exception.Response

    if ($null -eq $response) {
        throw
    }
}

$allowOrigin = $response.Headers['Access-Control-Allow-Origin']
$allowMethods = $response.Headers['Access-Control-Allow-Methods']

Write-Host "Status: $([int]$response.StatusCode)"
Write-Host "Access-Control-Allow-Origin: $allowOrigin"
Write-Host "Access-Control-Allow-Methods: $allowMethods"

if (-not $allowOrigin) {
    throw 'CORS header is missing'
}

Write-Host ''
Write-Host 'VERCEL RUNTIME + CORS OK' -ForegroundColor Green
