$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive\apps\admin\src"

if (!(Test-Path $root)) {
    throw "Admin source directory not found: $root"
}

Write-Host "[1/4] Detecting stale JSX/TSX filename collisions..." -ForegroundColor Cyan

$tsxFiles = Get-ChildItem -Path $root -Recurse -File -Filter *.tsx
$removed = @()

foreach ($tsx in $tsxFiles) {
    $jsx = [System.IO.Path]::ChangeExtension($tsx.FullName, ".jsx")

    if (Test-Path $jsx) {
        Write-Host "Collision found:" -ForegroundColor Yellow
        Write-Host "  KEEP   $($tsx.FullName)"
        Write-Host "  REMOVE $jsx"

        Remove-Item -LiteralPath $jsx -Force
        $removed += $jsx
    }
}

Write-Host "[2/4] Checking DashboardPage resolution..." -ForegroundColor Cyan

$dashboardTsx = Join-Path $root "pages\DashboardPage.tsx"
$dashboardJsx = Join-Path $root "pages\DashboardPage.jsx"

if (!(Test-Path $dashboardTsx)) {
    throw "Expected DashboardPage.tsx was not found: $dashboardTsx"
}

if (Test-Path $dashboardJsx) {
    Remove-Item -LiteralPath $dashboardJsx -Force
    $removed += $dashboardJsx
}

Write-Host "[3/4] Clearing Vite cache..." -ForegroundColor Cyan

$viteCache = "E:\UniversalLive\apps\admin\node_modules\.vite"
if (Test-Path $viteCache) {
    cmd.exe /c "rmdir /s /q `"$viteCache`""
}

Write-Host "[4/4] Result" -ForegroundColor Cyan

if ($removed.Count -eq 0) {
    Write-Host "No duplicate JSX files were found." -ForegroundColor Yellow
} else {
    Write-Host "Removed stale JSX files:" -ForegroundColor Green
    $removed | Sort-Object -Unique | ForEach-Object { Write-Host "  $_" }
}

Write-Host ""
Write-Host "PASS: stale JSX/TSX collisions cleaned." -ForegroundColor Green
Write-Host "Now run: cd E:\UniversalLive\apps\admin ; npm run build" -ForegroundColor Green
