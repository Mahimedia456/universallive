$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BackendRoot = Join-Path $ProjectRoot 'backend'
$SrcRoot = Join-Path $BackendRoot 'src'

Write-Host '=== UniversalLive Phase 30.1 Backend Cleanup ===' -ForegroundColor Cyan
Write-Host "Project: $ProjectRoot"

$stalePaths = @(
  (Join-Path $SrcRoot 'measurements'),
  (Join-Path $SrcRoot 'recommendations'),
  (Join-Path $SrcRoot 'scans'),
  (Join-Path $SrcRoot 'size-charts'),
  (Join-Path $SrcRoot 'profile'),
  (Join-Path $SrcRoot 'email'),
  (Join-Path $SrcRoot 'auth\auth.service.ts'),
  (Join-Path $SrcRoot 'common\decorators\current-profile.decorator.ts')
)

foreach ($path in $stalePaths) {
  if (Test-Path $path) {
    Write-Host "Removing stale non-UniversalLive source: $path" -ForegroundColor Yellow
    Remove-Item $path -Recurse -Force
  }
}

# Remove empty stale common/decorators folders if they remain.
$maybeEmpty = @(
  (Join-Path $SrcRoot 'common\decorators'),
  (Join-Path $SrcRoot 'common')
)
foreach ($path in $maybeEmpty) {
  if (Test-Path $path) {
    $items = @(Get-ChildItem $path -Force -ErrorAction SilentlyContinue)
    if ($items.Count -eq 0) { Remove-Item $path -Force }
  }
}

$dist = Join-Path $BackendRoot 'dist'
if (Test-Path $dist) {
  Write-Host 'Clearing stale backend dist...' -ForegroundColor DarkGray
  Remove-Item $dist -Recurse -Force
}

Write-Host ''
Write-Host 'UniversalLive backend source folders now present:' -ForegroundColor Green
Get-ChildItem $SrcRoot -Directory | Select-Object -ExpandProperty Name | Sort-Object | ForEach-Object { Write-Host "  - $_" }

Write-Host ''
Write-Host 'Cleanup complete.' -ForegroundColor Green
Write-Host 'Next: cd backend; npm install; npm run build' -ForegroundColor Cyan
