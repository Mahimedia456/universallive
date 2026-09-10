$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"
$backend = Join-Path $root "backend"
$appModule = Join-Path $backend "src\app.module.ts"

Write-Host "=== UniversalLive Backend Phase 01 ===" -ForegroundColor Cyan
Write-Host "Foundation + API contract" -ForegroundColor Cyan

if (-not (Test-Path (Join-Path $backend "package.json"))) {
    throw "Backend package.json not found: $backend"
}

if (-not (Test-Path $appModule)) {
    throw "app.module.ts not found: $appModule"
}

$backup = "$appModule.phase01.bak"
if (-not (Test-Path $backup)) {
    Copy-Item $appModule $backup
    Write-Host "Backup created: $backup" -ForegroundColor DarkGray
}

$text = Get-Content $appModule -Raw

if ($text -notmatch "FoundationModule") {
    $import = "import { FoundationModule } from './foundation/foundation.module';"

    # Put import before @Module.
    if ($text -match "@Module\s*\(") {
        $text = $text -replace "@Module\s*\(", ($import + [Environment]::NewLine + [Environment]::NewLine + "@Module(")
    } else {
        throw "Could not find @Module(...) in app.module.ts. Restore from $backup if needed."
    }

    # Add module as first import entry.
    if ($text -match "imports\s*:\s*\[") {
        $text = $text -replace "imports\s*:\s*\[", ("imports: [" + [Environment]::NewLine + "    FoundationModule,")
    } else {
        throw "Could not find imports: [ in app.module.ts. Restore from $backup if needed."
    }

    Set-Content -Path $appModule -Value $text -Encoding UTF8
    Write-Host "FoundationModule registered in app.module.ts" -ForegroundColor Green
} else {
    Write-Host "FoundationModule already registered; no app.module.ts change needed." -ForegroundColor Green
}

Write-Host ""
Write-Host "Compiling backend..." -ForegroundColor Yellow
Set-Location $backend

if (Test-Path (Join-Path $backend "node_modules")) {
    npm run build
} else {
    Write-Host "node_modules not present. Run npm install first if this is a fresh checkout." -ForegroundColor DarkYellow
    npm install
    npm run build
}

if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed. app.module backup: $backup"
}

Write-Host ""
Write-Host "BACKEND PHASE 01 BUILD SUCCESSFUL" -ForegroundColor Green
Write-Host ""
Write-Host "Run backend:" -ForegroundColor Yellow
Write-Host "  cd E:\UniversalLive\backend"
Write-Host "  npm run dev"
Write-Host ""
Write-Host "Then test:"
Write-Host "  http://127.0.0.1:3000/api/v1/foundation"
Write-Host "  http://127.0.0.1:3000/api/v1/foundation/readiness"
