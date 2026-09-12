param(
    [string]$BaseUrl = "https://universallive.vercel.app/api/v1",
    [Parameter(Mandatory=$true)][string]$Email,
    [Parameter(Mandatory=$true)][string]$Password,
    [string]$Origin = "http://localhost:5173"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')

Write-Host "[1/3] CORS preflight" -ForegroundColor Cyan
$preflight = Invoke-WebRequest -Method Options `
  -Uri "$BaseUrl/admin/auth/login" `
  -Headers @{
    Origin = $Origin
    'Access-Control-Request-Method' = 'POST'
    'Access-Control-Request-Headers' = 'content-type,authorization'
  } `
  -SkipHttpErrorCheck

if ($preflight.StatusCode -notin @(200,204)) {
    throw "CORS preflight failed with HTTP $($preflight.StatusCode)"
}
Write-Host "PASS: CORS preflight HTTP $($preflight.StatusCode)" -ForegroundColor Green

Write-Host "[2/3] Admin login" -ForegroundColor Cyan
$body = @{ email=$Email; password=$Password } | ConvertTo-Json
$response = Invoke-WebRequest -Method Post `
  -Uri "$BaseUrl/admin/auth/login" `
  -Headers @{ Origin=$Origin } `
  -ContentType 'application/json' `
  -Body $body `
  -SkipHttpErrorCheck

if ($response.StatusCode -ne 200 -and $response.StatusCode -ne 201) {
    Write-Host $response.Content -ForegroundColor Yellow
    throw "Admin login failed with HTTP $($response.StatusCode). 401 means credentials/admin row; 500 means deployed backend/env/database runtime issue."
}
Write-Host "PASS: admin login" -ForegroundColor Green

$json = $response.Content | ConvertFrom-Json
$token = $json.accessToken
if (!$token) { throw "Login succeeded but accessToken is missing" }

Write-Host "[3/3] Admin session" -ForegroundColor Cyan
$me = Invoke-WebRequest -Method Get `
  -Uri "$BaseUrl/admin/auth/me" `
  -Headers @{ Authorization="Bearer $token"; Origin=$Origin } `
  -SkipHttpErrorCheck
if ($me.StatusCode -ne 200) {
    Write-Host $me.Content -ForegroundColor Yellow
    throw "Admin /me failed with HTTP $($me.StatusCode)"
}
Write-Host "PASS: deployed admin auth + CORS verified" -ForegroundColor Green
