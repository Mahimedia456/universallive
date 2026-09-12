$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive\apps\admin\src"

if (!(Test-Path $root)) {
    throw "Admin source directory not found: $root"
}

Write-Host "[1/3] Removing stale JS/JSX source collisions..." -ForegroundColor Cyan

$removed = New-Object System.Collections.Generic.List[string]

# If the canonical TypeScript source exists, remove stale generated/legacy JS.
Get-ChildItem -Path $root -Recurse -File -Filter *.ts | ForEach-Object {
    $js = [System.IO.Path]::ChangeExtension($_.FullName, ".js")
    if (Test-Path $js) {
        Write-Host "  KEEP   $($_.FullName)" -ForegroundColor DarkGray
        Write-Host "  REMOVE $js" -ForegroundColor Yellow
        Remove-Item -LiteralPath $js -Force
        $removed.Add($js)
    }
}

Get-ChildItem -Path $root -Recurse -File -Filter *.tsx | ForEach-Object {
    $jsx = [System.IO.Path]::ChangeExtension($_.FullName, ".jsx")
    $js  = [System.IO.Path]::ChangeExtension($_.FullName, ".js")

    foreach ($candidate in @($jsx,$js)) {
        if (Test-Path $candidate) {
            Write-Host "  KEEP   $($_.FullName)" -ForegroundColor DarkGray
            Write-Host "  REMOVE $candidate" -ForegroundColor Yellow
            Remove-Item -LiteralPath $candidate -Force
            $removed.Add($candidate)
        }
    }
}

Write-Host "[2/3] Explicit auth API resolution check..." -ForegroundColor Cyan

$apiTs = Join-Path $root "api.ts"
$apiJs = Join-Path $root "api.js"

if (!(Test-Path $apiTs)) {
    throw "Canonical src\api.ts is missing"
}

if (Test-Path $apiJs) {
    Remove-Item -LiteralPath $apiJs -Force
    $removed.Add($apiJs)
}

Write-Host "PASS: ./api resolves to api.ts only" -ForegroundColor Green

Write-Host "[3/3] Clear Vite cache..." -ForegroundColor Cyan

$viteCache = "E:\UniversalLive\apps\admin\node_modules\.vite"
if (Test-Path $viteCache) {
    cmd.exe /c "rmdir /s /q `"$viteCache`""
}

Write-Host ""
if ($removed.Count -gt 0) {
    Write-Host "Removed stale source files:" -ForegroundColor Green
    $removed | Sort-Object -Unique | ForEach-Object { Write-Host "  $_" }
} else {
    Write-Host "No stale source collisions remained." -ForegroundColor Yellow
}

Write-Host "PASS: admin source collision cleanup complete" -ForegroundColor Green
