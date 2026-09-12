$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"

Write-Host "[1/6] Fix backend TypeScript config" -ForegroundColor Cyan
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File "$root\tools\fix-backend-tsconfig-v2.ps1"
if ($LASTEXITCODE -ne 0) { throw "Backend tsconfig fix failed" }

Write-Host "[2/6] Clean stale admin JS/JSX collisions" -ForegroundColor Cyan
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File "$root\tools\fix-admin-source-collisions-v2.ps1"
if ($LASTEXITCODE -ne 0) { throw "Admin source collision cleanup failed" }

Write-Host "[3/6] Verify auth API export source" -ForegroundColor Cyan
$apiTs = "$root\apps\admin\src\api.ts"
$apiJs = "$root\apps\admin\src\api.js"

if (!(Test-Path $apiTs)) { throw "src\api.ts missing" }
if (Test-Path $apiJs) { throw "src\api.js still exists and can shadow api.ts" }

$apiText = Get-Content $apiTs -Raw
if (!$apiText.Contains("authApi")) {
    throw "src\api.ts itself does not contain authApi. Send that file/output if this occurs."
}

Write-Host "PASS: api.ts contains authApi and stale api.js is gone" -ForegroundColor Green

Write-Host "[4/6] Backend production build" -ForegroundColor Cyan
Set-Location "$root\backend"
npm run build
if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
Write-Host "PASS: backend build" -ForegroundColor Green

Write-Host "[5/6] Admin production build" -ForegroundColor Cyan
Set-Location "$root\apps\admin"
npm run build
if ($LASTEXITCODE -ne 0) { throw "Admin build failed" }
Write-Host "PASS: admin build" -ForegroundColor Green

Write-Host "[6/6] Final collision scan" -ForegroundColor Cyan
$collisions = @()

Get-ChildItem "$root\apps\admin\src" -Recurse -File -Filter *.ts | ForEach-Object {
    $js = [System.IO.Path]::ChangeExtension($_.FullName, ".js")
    if (Test-Path $js) { $collisions += $js }
}
Get-ChildItem "$root\apps\admin\src" -Recurse -File -Filter *.tsx | ForEach-Object {
    foreach ($candidate in @(
        [System.IO.Path]::ChangeExtension($_.FullName, ".jsx"),
        [System.IO.Path]::ChangeExtension($_.FullName, ".js")
    )) {
        if (Test-Path $candidate) { $collisions += $candidate }
    }
}

if ($collisions.Count -gt 0) {
    Write-Host "Remaining collisions:" -ForegroundColor Red
    $collisions | Sort-Object -Unique | ForEach-Object { Write-Host "  $_" }
    throw "Stale JS/TS source collisions remain"
}

Write-Host ""
Write-Host "PASS: no stale JS/TS source collisions" -ForegroundColor Green
Write-Host "PASS: Universal Live backend build" -ForegroundColor Green
Write-Host "PASS: Universal Live admin build" -ForegroundColor Green
