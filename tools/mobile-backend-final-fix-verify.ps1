param(
    [string]$BaseUrl = "http://127.0.0.1:3000/api/v1",
    [string]$Email = "creator.test@universallive.local",
    [string]$Password = "1231231234"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')

function Invoke-UlApi {
    param([string]$Method, [string]$Path, $Body = $null, [string]$Token = "")
    $headers = @{ Accept = "application/json" }
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $args = @{ Method = $Method; Uri = "$BaseUrl/$Path"; Headers = $headers }
    if ($null -ne $Body) {
        $args.ContentType = "application/json"
        $args.Body = ($Body | ConvertTo-Json -Depth 12)
    }
    Invoke-RestMethod @args
}

Write-Host "[1/5] Final backend contract ..." -ForegroundColor Cyan
$root = Invoke-UlApi GET ""
if ($root.mobileContractVersion -ne "2026.09-final") { throw "FAIL: backend contract is $($root.mobileContractVersion)" }
Write-Host "PASS: final mobile contract" -ForegroundColor Green

Write-Host "[2/5] Custom auth login ..." -ForegroundColor Cyan
$login = Invoke-UlApi POST "auth/mobile/login" @{ email = $Email; password = $Password }
$token = [string]$login.access_token
if (-not $token) { throw "FAIL: no access token" }
Write-Host "PASS: login" -ForegroundColor Green

Write-Host "[3/5] Onboarding/service-role access ..." -ForegroundColor Cyan
$onboarding = Invoke-UlApi GET "onboarding/me" $null $token
if ($null -eq $onboarding.creator_setup_completed) { throw "FAIL: onboarding contract unavailable" }
Write-Host "PASS: ul_onboarding_state readable through backend" -ForegroundColor Green

Write-Host "[4/5] Streaming Phase 13 route deployment ..." -ForegroundColor Cyan
$draft = Invoke-UlApi GET "streams/draft/current" $null $token
Write-Host "PASS: /streams/draft/current route exists" -ForegroundColor Green

Write-Host "[5/5] Actual-live restore contract ..." -ForegroundColor Cyan
$active = Invoke-UlApi GET "streams/sessions/active/current" $null $token
if ($null -ne $active -and @('live','reconnecting','created','starting','connecting') -notcontains [string]$active.status) {
    throw "FAIL: unexpected active session status: $($active.status)"
}
Write-Host "PASS: active-session endpoint" -ForegroundColor Green

Write-Host "MOBILE + BACKEND FINAL FIX VERIFICATION PASSED" -ForegroundColor Green
