param(
  [Parameter(Mandatory=$true)]
  [string]$BaseUrl
)

$ErrorActionPreference = 'Stop'
$base = $BaseUrl.TrimEnd('/')
$url = "$base/streams/preflight"

Write-Host "Checking $url" -ForegroundColor Cyan
try {
  $resp = Invoke-WebRequest -Uri $url -Method POST -ContentType 'application/json' -Body '{"connectionIds":[],"config":{}}' -UseBasicParsing
  $code = [int]$resp.StatusCode
} catch {
  if ($_.Exception.Response) {
    $code = [int]$_.Exception.Response.StatusCode
  } else {
    throw
  }
}

if ($code -eq 404) {
  throw "FAIL: /streams/preflight is still 404. StreamLifecycleModule is not active in the deployed backend."
}

if ($code -in 401,403) {
  Write-Host "PASS: preflight route exists (HTTP $code without auth, expected)." -ForegroundColor Green
  exit 0
}

if ($code -ge 200 -and $code -lt 500) {
  Write-Host "PASS: preflight route is mounted (HTTP $code)." -ForegroundColor Green
  exit 0
}

throw "FAIL: unexpected HTTP $code from preflight route."
