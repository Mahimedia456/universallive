$ErrorActionPreference = "Stop"

$admin = "E:\UniversalLive\apps\admin"

Write-Host "[1/3] Apply stale JSX cleanup" -ForegroundColor Cyan
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File "E:\UniversalLive\tools\fix-admin-stale-jsx.ps1"

if ($LASTEXITCODE -ne 0) {
    throw "Stale JSX cleanup failed"
}

Write-Host "[2/3] Verify DashboardPage collision is gone" -ForegroundColor Cyan

$tsx = "$admin\src\pages\DashboardPage.tsx"
$jsx = "$admin\src\pages\DashboardPage.jsx"

if (!(Test-Path $tsx)) {
    throw "DashboardPage.tsx missing"
}

if (Test-Path $jsx) {
    throw "DashboardPage.jsx still exists and can shadow DashboardPage.tsx"
}

Write-Host "PASS: DashboardPage resolves to TSX only" -ForegroundColor Green

Write-Host "[3/3] Admin production build" -ForegroundColor Cyan
Set-Location $admin
npm run build

if ($LASTEXITCODE -ne 0) {
    throw "Admin build still has another issue"
}

Write-Host ""
Write-Host "PASS: Universal Live admin production build" -ForegroundColor Green
