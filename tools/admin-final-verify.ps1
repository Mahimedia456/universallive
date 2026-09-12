$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

Write-Host "[1/5] Backend install/build" -ForegroundColor Cyan
Set-Location "$root\backend"
if (Test-Path dist) { Remove-Item -Recurse -Force dist }
npm install
npm run build
if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }

Write-Host "[2/5] Admin install/build" -ForegroundColor Cyan
Set-Location "$root\apps\admin"
if (Test-Path "node_modules\.vite") { Remove-Item -Recurse -Force "node_modules\.vite" }
npm install
npm run build
if ($LASTEXITCODE -ne 0) { throw "Admin build failed" }

Write-Host "[3/5] Stale App.jsx collision check" -ForegroundColor Cyan
$main = Get-Content "$root\apps\admin\src\main.tsx" -Raw
if ($main -match "from\s+['""]\./App['""]") { throw "main.tsx still imports ./App and may resolve stale App.jsx" }
Write-Host "PASS: main.tsx does not import stale ./App"

Write-Host "[4/5] Feature flags enabled-column regression check" -ForegroundColor Cyan
$files = @(
 "$root\apps\admin\src\pages\FeatureFlagsPage.tsx",
 "$root\backend\src\admin-console\admin-console.service.ts"
)
foreach ($f in $files) {
  $txt = Get-Content $f -Raw
  if ($txt -match "\.enabled\b" -and $f -match "FeatureFlags") { throw "Feature flags frontend still references enabled" }
}
Write-Host "PASS: feature flag UI uses existing is_public/value schema"

Write-Host "[5/5] Final status" -ForegroundColor Cyan
Write-Host "PASS: backend build"
Write-Host "PASS: admin build"
Write-Host "PASS: stale App.jsx protection"
Write-Host "PASS: feature flag compatibility"
Write-Host ""
Write-Host "Now log in as SUPER_ADMIN and open Admin > Final QA to run live database/runtime checks." -ForegroundColor Green
