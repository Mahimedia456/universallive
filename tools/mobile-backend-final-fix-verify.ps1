param(
    [string]$BaseUrl = "http://127.0.0.1:3000/api/v1",
    [string]$Email = "creator.test@universallive.local",
    [string]$Password = "1231231234"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')

function Invoke-UlApi {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = ""
    )

    $headers = @{ Accept = "application/json" }
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $headers.Authorization = "Bearer $Token"
    }

    $request = @{
        Method  = $Method
        Uri     = "$BaseUrl/$Path"
        Headers = $headers
    }

    if ($null -ne $Body) {
        $request.ContentType = "application/json"
        $request.Body = ($Body | ConvertTo-Json -Depth 12)
    }

    Invoke-RestMethod @request
}

Write-Host "[1/5] Final backend contract ..." -ForegroundColor Cyan
$root = Invoke-UlApi -Method GET -Path ""
if ($root.mobileContractVersion -ne "2026.09-final") {
    throw "FAIL: backend contract is $($root.mobileContractVersion)"
}
Write-Host "PASS: final mobile contract" -ForegroundColor Green

Write-Host "[2/5] Custom auth login ..." -ForegroundColor Cyan
$login = Invoke-UlApi -Method POST -Path "auth/mobile/login" -Body @{ email = $Email; password = $Password }
$token = [string]$login.access_token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "FAIL: no access token"
}
Write-Host "PASS: login" -ForegroundColor Green

Write-Host "[3/5] Onboarding/service-role access ..." -ForegroundColor Cyan
$onboarding = Invoke-UlApi -Method GET -Path "onboarding/me" -Token $token
if ($null -eq $onboarding) {
    throw "FAIL: onboarding contract unavailable"
}
if ($null -eq $onboarding.creator_setup_completed) {
    throw "FAIL: onboarding contract unavailable"
}
Write-Host "PASS: ul_onboarding_state readable through backend" -ForegroundColor Green

Write-Host "[4/5] Streaming Phase 13 route deployment ..." -ForegroundColor Cyan
$draft = Invoke-UlApi -Method GET -Path "streams/draft/current" -Token $token
Write-Host "PASS: /streams/draft/current route exists" -ForegroundColor Green

Write-Host "[5/5] Actual-live restore contract ..." -ForegroundColor Cyan
$active = Invoke-UlApi -Method GET -Path "streams/sessions/active/current" -Token $token

# No active broadcast is a valid state. Some PowerShell/Nest combinations return
# an empty string for a null/empty HTTP response, so normalize both cases.
$hasActive = $false
if ($null -ne $active) {
    if ($active -is [string]) {
        $raw = ([string]$active).Trim()
        if ($raw -ne "" -and $raw -ne "null") {
            $hasActive = $true
        }
    }
    else {
        $hasActive = $true
    }
}

if (-not $hasActive) {
    Write-Host "INFO: no active broadcast session (expected when not live)" -ForegroundColor DarkGray
}
else {
    $status = ([string]$active.status).Trim().ToLowerInvariant()
    $validStatuses = @("live", "reconnecting", "created", "starting", "connecting")
    if ($validStatuses -notcontains $status) {
        throw "FAIL: unexpected active session status: $status"
    }
    Write-Host "INFO: active session status = $status" -ForegroundColor DarkGray
}

Write-Host "PASS: active-session endpoint" -ForegroundColor Green
Write-Host "MOBILE + BACKEND FINAL FIX VERIFICATION PASSED" -ForegroundColor Green
