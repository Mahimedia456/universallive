$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$backend = Join-Path $root 'backend'
$appModule = Join-Path $backend 'src\app.module.ts'

if (-not (Test-Path $appModule)) {
    throw "app.module.ts not found: $appModule"
}

Write-Host '=== UniversalLive Backend Batch 02 REPAIR ===' -ForegroundColor Cyan

$backup = "$appModule.backend-batch02-repair.bak"
if (-not (Test-Path $backup)) {
    Copy-Item $appModule $backup
}

$text = Get-Content -Path $appModule -Raw

function Add-NestModule {
    param(
        [Parameter(Mandatory=$true)][string]$ModuleName,
        [Parameter(Mandatory=$true)][string]$ImportLine
    )

    if ($script:text.Contains($ModuleName)) {
        Write-Host "[SKIP] $ModuleName already registered" -ForegroundColor DarkGray
        return
    }

    $moduleMarker = '@Module('
    $moduleIndex = $script:text.IndexOf($moduleMarker)

    if ($moduleIndex -lt 0) {
        throw "Could not locate @Module( in app.module.ts"
    }

    $script:text =
        $script:text.Substring(0, $moduleIndex) +
        $ImportLine +
        [Environment]::NewLine +
        [Environment]::NewLine +
        $script:text.Substring($moduleIndex)

    $importsMarker = 'imports: ['
    $importsIndex = $script:text.IndexOf($importsMarker)

    if ($importsIndex -lt 0) {
        throw "Could not locate imports: [ in app.module.ts"
    }

    $insertAt = $importsIndex + $importsMarker.Length

    $script:text =
        $script:text.Substring(0, $insertAt) +
        [Environment]::NewLine +
        '    ' +
        $ModuleName +
        ',' +
        $script:text.Substring($insertAt)

    Write-Host "[ADD] $ModuleName" -ForegroundColor Green
}

Add-NestModule `
    -ModuleName 'AuthV2Module' `
    -ImportLine "import { AuthV2Module } from './auth-v2/auth-v2.module';"

Add-NestModule `
    -ModuleName 'ProfilesV2Module' `
    -ImportLine "import { ProfilesV2Module } from './profiles-v2/profiles-v2.module';"

Add-NestModule `
    -ModuleName 'DevicesModule' `
    -ImportLine "import { DevicesModule } from './devices/devices.module';"

Set-Content -Path $appModule -Value $text -Encoding UTF8

Set-Location $backend
Write-Host 'Compiling backend...' -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed. Backup: $backup"
}

Write-Host ''
Write-Host 'BATCH 02 REPAIR BUILD SUCCESSFUL' -ForegroundColor Green
