param(
    [string]$BaseUrl = "https://universallive.vercel.app/api/v1"
)
$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd('/')
Write-Host "Checking $BaseUrl ..." -ForegroundColor Cyan
$root = Invoke-RestMethod -Method GET -Uri $BaseUrl
if ($root.authProvider -ne 'universallive-db') { throw "FAIL: unexpected authProvider: $($root.authProvider)" }
if ($root.mobileContractVersion -ne '2026.09-final') { throw "FAIL: backend mobile contract is $($root.mobileContractVersion); expected 2026.09-final" }
Write-Host "PASS: authProvider = universallive-db" -ForegroundColor Green
Write-Host "PASS: mobileContractVersion = 2026.09-final" -ForegroundColor Green
Write-Host "NOTE: authenticated streaming routes are exercised by the in-app preflight after login." -ForegroundColor DarkGray
