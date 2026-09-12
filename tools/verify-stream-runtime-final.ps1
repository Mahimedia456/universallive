param(
  [string]$BaseUrl = "https://universallive.vercel.app/api/v1"
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

Write-Host "=== Universal Live stream runtime verification ===" -ForegroundColor Cyan

Write-Host "[1/4] Runtime route..."
try {
  $runtime = Invoke-RestMethod -Uri "$base/streams/runtime" -Method GET -TimeoutSec 20
  if ($runtime.contractVersion -lt 2 -or $runtime.publishRouteValidation -ne $true) {
    throw "Runtime contract is older than expected: $($runtime | ConvertTo-Json -Compress)"
  }
  Write-Host "PASS: stream lifecycle runtime contract v$($runtime.contractVersion)" -ForegroundColor Green
} catch {
  throw "FAILED: $base/streams/runtime is not the final deployed route. $($_.Exception.Message)"
}

Write-Host "[2/4] Protected preflight route exists..."
try {
  Invoke-WebRequest -Uri "$base/streams/preflight" -Method POST -ContentType "application/json" -Body '{"connectionIds":[],"config":{}}' -TimeoutSec 20 -UseBasicParsing | Out-Null
  Write-Host "PASS: route responded" -ForegroundColor Green
} catch {
  $code = [int]$_.Exception.Response.StatusCode
  if ($code -eq 401 -or $code -eq 403 -or $code -eq 400) {
    Write-Host "PASS: preflight route exists (HTTP $code without auth, expected)" -ForegroundColor Green
  } elseif ($code -eq 404) {
    throw "FAILED: /streams/preflight is still 404. Production is not running the latest backend."
  } else {
    throw "FAILED: unexpected preflight response HTTP $code"
  }
}

Write-Host "[3/4] Local source checks..."
$root = Split-Path -Parent $PSScriptRoot
$service = Join-Path $root "backend\src\stream-lifecycle\stream-lifecycle.service.ts"
$api = Join-Path $root "androidApp\src\main\kotlin\com\universallive\app\integration\AndroidMobileBackendApi.kt"
$publisher = Join-Path $root "androidApp\src\main\kotlin\com\universallive\app\streaming\RtmpPublisher.kt"

foreach ($f in @($service,$api,$publisher)) {
  if (!(Test-Path $f)) { throw "Missing $f" }
}

if ((Get-Content $service -Raw) -notmatch "publish route error") { throw "Backend route validation hardening missing" }
if ((Get-Content $api -Raw) -notmatch "StreamPreflightCheck") { throw "Mobile preflight check parser missing" }
if ((Get-Content $publisher -Raw) -notmatch "awaitingFirstKeyframe") { throw "RTMP first-keyframe gate missing" }

Write-Host "PASS: mobile/backend source contract" -ForegroundColor Green

Write-Host "[4/4] Commands to run locally..."
Write-Host "  cd E:\UniversalLive\backend && npm run build"
Write-Host "  cd E:\UniversalLive && .\gradlew.bat :androidApp:assembleDebug"
Write-Host ""
Write-Host "PASS: runtime verification script completed" -ForegroundColor Green
