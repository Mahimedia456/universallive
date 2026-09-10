$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"
$backend = Join-Path $root "backend"
$appModule = Join-Path $backend "src\app.module.ts"

if (-not (Test-Path $appModule)) {
    throw "app.module.ts not found: $appModule"
}

Write-Host "=== UniversalLive Backend Batch 02 — Phase 02/03/04 ===" -ForegroundColor Cyan

$backup = "$appModule.backend-batch02.bak"
if (-not (Test-Path $backup)) {
    Copy-Item $appModule $backup
}

$text = Get-Content $appModule -Raw

$modules = @(
    @{ Name = "AuthV2Module"; Import = "import { AuthV2Module } from './auth-v2/auth-v2.module';" },
    @{ Name = "ProfilesV2Module"; Import = "import { ProfilesV2Module } from './profiles-v2/profiles-v2.module';" },
    @{ Name = "DevicesModule"; Import = "import { DevicesModule } from './devices/devices.module';" }
)

foreach ($m in $modules) {
    if ($text -notmatch [regex]::Escape($m.Name)) {
        if ($text -notmatch "@Module\s*\(") {
            throw "Could not locate @Module in app.module.ts"
        }

        $text = $text -replace "@Module\s*\(", ($m.Import + [Environment]::NewLine + [Environment]::NewLine + "@Module(")

        if ($text -notmatch "imports\s*:\s*\[") {
            throw "Could not locate imports: [ in app.module.ts"
        }

        $text = $text -replace "imports\s*:\s*\[", ("imports: [" + [Environment]::NewLine + "    " + $m.Name + ",")
    }
}

Set-Content -Path $appModule -Value $text -Encoding UTF8

Set-Location $backend
Write-Host "Compiling backend..." -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed. Backup: $backup"
}

Write-Host "BACKEND BATCH 02 BUILD SUCCESSFUL" -ForegroundColor Green
