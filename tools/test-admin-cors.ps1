$ErrorActionPreference = 'Stop'

$url = 'https://universallive.vercel.app/api/v1/admin-console/login'

Write-Host '=== UniversalLive Admin CORS Preflight Test ===' -ForegroundColor Cyan

$response = Invoke-WebRequest `
    -Uri $url `
    -Method Options `
    -Headers @{
        Origin = 'http://localhost:5173'
        'Access-Control-Request-Method' = 'POST'
        'Access-Control-Request-Headers' = 'content-type'
    } `
    -SkipHttpErrorCheck

Write-Host "Status: $($response.StatusCode)"
Write-Host "Access-Control-Allow-Origin: $($response.Headers['Access-Control-Allow-Origin'])"
Write-Host "Access-Control-Allow-Methods: $($response.Headers['Access-Control-Allow-Methods'])"

if (-not $response.Headers['Access-Control-Allow-Origin']) {
    throw 'CORS header is missing. Confirm latest Git commit has deployed to Vercel.'
}

Write-Host ''
Write-Host 'ADMIN CORS PREFLIGHT OK' -ForegroundColor Green
