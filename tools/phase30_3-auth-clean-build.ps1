$ErrorActionPreference = 'Stop'
$project = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $project 'backend'

Write-Host '=== UniversalLive Phase 30.3 Auth Cleanup ===' -ForegroundColor Cyan
Write-Host "Project: $project"

$stale = @(
  (Join-Path $backend 'src\auth\supabase-jwt.guard.ts'),
  (Join-Path $backend 'src\auth\size-me-jwt.guard.ts'),
  (Join-Path $backend 'src\auth\auth.service.ts')
)
foreach ($path in $stale) {
  if (Test-Path $path) {
    Write-Host "Removing stale auth file: $path" -ForegroundColor Yellow
    Remove-Item $path -Force
  }
}

# Remove any remaining SizeME-named TS files anywhere in backend/src.
Get-ChildItem (Join-Path $backend 'src') -Recurse -File -ErrorAction SilentlyContinue |
  Where-Object { $_.Name -match '(?i)size[-_]?me|sizeme' } |
  ForEach-Object {
    Write-Host "Removing stale SizeME file: $($_.FullName)" -ForegroundColor Yellow
    Remove-Item $_.FullName -Force
  }

$dist = Join-Path $backend 'dist'
if (Test-Path $dist) {
  Write-Host 'Clearing stale backend dist...'
  Remove-Item $dist -Recurse -Force
}

Write-Host "`nAuth files remaining:" -ForegroundColor Cyan
Get-ChildItem (Join-Path $backend 'src\auth') -File | Sort-Object Name | ForEach-Object { Write-Host "  - $($_.Name)" }

Push-Location $backend
try {
  Write-Host "`nBuilding UniversalLive backend..." -ForegroundColor Cyan
  npm run build
  if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' }
  Write-Host "`nBackend build succeeded." -ForegroundColor Green
}
finally {
  Pop-Location
}
