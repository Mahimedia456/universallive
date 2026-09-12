$ErrorActionPreference = "Stop"
$root = "E:\UniversalLive"

Write-Host "[1/6] Apply backend tsconfig compatibility fix" -ForegroundColor Cyan
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "$root\tools\fix-backend-tsconfig.ps1"
if ($LASTEXITCODE -ne 0) { throw "tsconfig fix failed" }

Write-Host "[2/6] Verify Vite env typings" -ForegroundColor Cyan
if (!(Test-Path "$root\apps\admin\src\vite-env.d.ts")) { throw "vite-env.d.ts missing" }
Write-Host "PASS: vite/client typing present"

Write-Host "[3/6] Verify cumulative console API" -ForegroundColor Cyan
$api = Get-Content "$root\apps\admin\src\consoleApi.ts" -Raw
$required = @(
 "analytics:", "diagnostics:", "diagnosticStream:", "plans:", "entitlements:",
 "billing:", "notifications:", "supportTickets:", "moderation:", "systemHealth:",
 "systemFlags:", "admins:", "auditLogs:", "adminSettings:", "finalQa:"
)
foreach ($token in $required) {
  if (!$api.Contains($token)) { throw "consoleApi is missing $token" }
}
Write-Host "PASS: cumulative consoleApi methods present"

Write-Host "[4/6] Backend build" -ForegroundColor Cyan
Set-Location "$root\backend"
if (Test-Path dist) { Remove-Item -Recurse -Force dist }
npm run build
if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
Write-Host "PASS: backend build"

Write-Host "[5/6] Admin build" -ForegroundColor Cyan
Set-Location "$root\apps\admin"
if (Test-Path "node_modules\.vite") { Remove-Item -Recurse -Force "node_modules\.vite" }
npm run build
if ($LASTEXITCODE -ne 0) { throw "Admin build failed" }
Write-Host "PASS: admin build"

Write-Host "[6/6] Complete" -ForegroundColor Cyan
Write-Host "PASS: Universal Live Admin build hotfix verified" -ForegroundColor Green
