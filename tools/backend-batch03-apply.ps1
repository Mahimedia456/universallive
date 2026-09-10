$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$backend = Join-Path $root 'backend'
$appModule = Join-Path $backend 'src\app.module.ts'

if (-not (Test-Path $appModule)) {
    throw "app.module.ts not found: $appModule"
}

Write-Host '=== UniversalLive Backend Batch 03 — Phase 05/06/07 ===' -ForegroundColor Cyan

$backup = "$appModule.backend-batch03.bak"
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

    $moduleIndex = $script:text.IndexOf('@Module(')
    if ($moduleIndex -lt 0) {
        throw "Could not locate @Module( in app.module.ts"
    }

    $script:text =
        $script:text.Substring(0, $moduleIndex) +
        $ImportLine +
        [Environment]::NewLine +
        [Environment]::NewLine +
        $script:text.Substring($moduleIndex)

    $importsIndex = $script:text.IndexOf('imports: [')
    if ($importsIndex -lt 0) {
        throw "Could not locate imports: [ in app.module.ts"
    }

    $insertAt = $importsIndex + 'imports: ['.Length

    $script:text =
        $script:text.Substring(0, $insertAt) +
        [Environment]::NewLine +
        '    ' +
        $ModuleName +
        ',' +
        $script:text.Substring($insertAt)

    Write-Host "[ADD] $ModuleName" -ForegroundColor Green
}

# Repair/register Batch 02 modules too, if the earlier broken script never ran.
Add-NestModule `
    -ModuleName 'AuthV2Module' `
    -ImportLine "import { AuthV2Module } from './auth-v2/auth-v2.module';"

Add-NestModule `
    -ModuleName 'ProfilesV2Module' `
    -ImportLine "import { ProfilesV2Module } from './profiles-v2/profiles-v2.module';"

Add-NestModule `
    -ModuleName 'DevicesModule' `
    -ImportLine "import { DevicesModule } from './devices/devices.module';"

# Batch 03 modules.
Add-NestModule `
    -ModuleName 'StreamingConnectionsModule' `
    -ImportLine "import { StreamingConnectionsModule } from './streaming-connections/streaming-connections.module';"

Add-NestModule `
    -ModuleName 'PlatformOauthModule' `
    -ImportLine "import { PlatformOauthModule } from './platform-oauth/platform-oauth.module';"

Add-NestModule `
    -ModuleName 'RtmpCredentialsModule' `
    -ImportLine "import { RtmpCredentialsModule } from './rtmp-credentials/rtmp-credentials.module';"

Set-Content -Path $appModule -Value $text -Encoding UTF8

Set-Location $backend
Write-Host 'Compiling backend...' -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed. Backup: $backup"
}

Write-Host ''
Write-Host 'BACKEND BATCH 03 BUILD SUCCESSFUL' -ForegroundColor Green
