$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot

& (Join-Path $PSScriptRoot 'phase30_1-clean-backend.ps1')

$BackendRoot = Join-Path $ProjectRoot 'backend'
Push-Location $BackendRoot
try {
  Write-Host ''
  Write-Host 'Installing/verifying backend dependencies...' -ForegroundColor Cyan
  npm install
  if ($LASTEXITCODE -ne 0) { throw 'npm install failed' }

  Write-Host 'Building UniversalLive backend...' -ForegroundColor Cyan
  npm run build
  if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' }
} finally {
  Pop-Location
}

Write-Host ''
Write-Host 'Backend build succeeded.' -ForegroundColor Green
Write-Host 'Now run: .\tools\phase30-run-all.ps1' -ForegroundColor Cyan
