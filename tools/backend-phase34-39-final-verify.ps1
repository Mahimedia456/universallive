param(
    [string]$BaseUrl = "http://127.0.0.1:3000/api/v1",
    [string]$Email = "creator.test@universallive.local",
    [string]$Password = "1231231234",
    [switch]$RequireFirebase,
    [switch]$ExercisePush
)

$ErrorActionPreference = "Stop"

function Invoke-UlApi {
    param([string]$Method,[string]$Path,$Body=$null,[string]$Token="")
    $headers = @{ Accept = "application/json" }
    if ($Token) { $headers.Authorization = "Bearer $Token" }
    $params = @{ Method=$Method; Uri="$BaseUrl/$Path"; Headers=$headers; ContentType="application/json" }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 40 -Compress) }
    Invoke-RestMethod @params
}

function Assert-True([bool]$Condition,[string]$Message) {
    if (-not $Condition) { throw "FAIL: $Message" }
    Write-Host "PASS: $Message" -ForegroundColor Green
}

Write-Host "[1/9] API root + bootstrap/system state ..." -ForegroundColor Cyan
$root = Invoke-UlApi GET ""
Assert-True ([string]$root.authProvider -eq "universallive-db") "Universal Live custom-auth provider"
$system = Invoke-UlApi GET "system/state"
Assert-True ($system.online -eq $true) "Phase 37 system state endpoint"
Assert-True ($null -ne $system.featureFlags) "Phase 38 public feature flags"

Write-Host "[2/9] Login + final contract ..." -ForegroundColor Cyan
$login = Invoke-UlApi POST "auth/mobile/login" @{email=$Email;password=$Password}
$token = [string]$login.access_token
Assert-True (-not [string]::IsNullOrWhiteSpace($token)) "custom JWT login"
$bootstrap = Invoke-UlApi GET "app/bootstrap" $null $token
Assert-True ([string]$bootstrap.mobile_contract_version -eq "2026.09-final") "final mobile contract version"

Write-Host "[3/9] Phase 34 support ..." -ForegroundColor Cyan
$tickets = Invoke-UlApi GET "support/tickets" $null $token
Assert-True ($null -ne $tickets) "support ticket list endpoint"

Write-Host "[4/9] Phase 35 diagnostics + redacted snapshot ..." -ForegroundColor Cyan
$diag = Invoke-UlApi GET "diagnostics/summary" $null $token
Assert-True ($diag.ok -eq $true) "diagnostics summary"
Assert-True ($diag.redaction.secretsIncluded -eq $false) "diagnostics excludes secrets"
$snapshot = Invoke-UlApi POST "diagnostics/snapshot" @{appVersion="0.40.0";buildNumber="39";platform="android";captureStatus="qa";publishStatus="idle"} $token
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$snapshot.id)) "sanitized support snapshot persisted"
if ($RequireFirebase) {
    Assert-True ($diag.backend.firebaseConfigured -eq $true) "Firebase service account configured"
} elseif ($diag.backend.firebaseConfigured -ne $true) {
    Write-Host "INFO: Firebase backend credentials are not configured yet. Use -RequireFirebase after adding them." -ForegroundColor Yellow
}

Write-Host "[5/9] Phase 36 legal/about ..." -ForegroundColor Cyan
$about = Invoke-UlApi GET "legal/about"
$documents = Invoke-UlApi GET "legal/documents"
Assert-True ([string]$about.mobileContractVersion -eq "2026.09-final") "legal/about contract"
$documentCount = if ($null -eq $documents) { 0 } elseif ($documents -is [System.Array]) { $documents.Length } else { 1 }
Write-Host "INFO: active legal documents returned by API = $documentCount" -ForegroundColor DarkGray
Assert-True ($documentCount -ge 4) "active legal document catalog"
$required = @($documents | Where-Object { $_.required_acceptance -eq $true } | Select-Object -First 1)
if ($required.Count -gt 0) {
    Invoke-UlApi POST "legal/accept" @{documentKey=[string]$required[0].document_key;version=[string]$required[0].version} $token | Out-Null
    $acceptances = @(Invoke-UlApi GET "legal/acceptances" $null $token)
    Assert-True ($acceptances.Count -ge 1) "legal acceptance persisted"
}

Write-Host "[6/9] Phase 29 plan catalog regression ..." -ForegroundColor Cyan
$plans = Invoke-UlApi GET "billing/plans" $null $token
$keys = @($plans | ForEach-Object { [string]$_.plan_key })
Write-Host "INFO: billing plans returned = $($keys -join ', ')" -ForegroundColor DarkGray
Assert-True (($keys -contains "free") -and ($keys -contains "creator") -and ($keys -contains "pro")) "FREE/CREATOR/PRO plans available"

Write-Host "[7/9] Phase 39 persisted backend QA ..." -ForegroundColor Cyan
$qa = Invoke-UlApi POST "qa/smoke" @{} $token
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$qa.id)) "QA run persisted"
Assert-True ($qa.requiredPassed -eq $true) "required backend QA checks pass"
$latest = Invoke-UlApi GET "qa/latest" $null $token
Assert-True ([string]$latest.id -eq [string]$qa.id) "latest QA run endpoint"

Write-Host "[8/9] Notifications / Android FCM contract ..." -ForegroundColor Cyan
$pushStatus = Invoke-UlApi GET "notifications/push/status" $null $token
Assert-True ([string]$pushStatus.provider -eq "fcm-http-v1") "FCM HTTP v1 provider"
if ($ExercisePush) {
    Assert-True ($pushStatus.configured -eq $true) "Firebase backend credentials configured"
    $devices = Invoke-UlApi GET "devices/me" $null $token
    $registered = @($devices | Where-Object { $_.platform -eq "android" -and $_.push_registered -eq $true })
    Assert-True ($registered.Count -gt 0) "Android FCM token registered"
    $push = Invoke-UlApi POST "notifications/push/test" @{} $token
    Assert-True ([int]$push.delivery.sent -gt 0) "real Android push sent"
} else {
    Write-Host "INFO: add -ExercisePush for a real device push test." -ForegroundColor Yellow
}

Write-Host "[9/9] Final account/history smoke ..." -ForegroundColor Cyan
$profile = Invoke-UlApi GET "profiles/me" $null $token
$history = Invoke-UlApi GET "streams/history?limit=5&offset=0" $null $token
Assert-True (-not [string]::IsNullOrWhiteSpace([string]$profile.user_id)) "profile endpoint"
Assert-True ($null -ne $history) "history endpoint"

Write-Host "`nPHASE 34-39 FINAL VERIFICATION PASSED" -ForegroundColor Green
