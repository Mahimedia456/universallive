$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$backend = Join-Path $root 'backend'
$appModule = Join-Path $backend 'src\app.module.ts'

if (-not (Test-Path $appModule)) {
    throw "app.module.ts not found: $appModule"
}

Write-Host '=== UniversalLive Backend Batch 05 — Phase 11/12/13/14 ===' -ForegroundColor Cyan

$backup = "$appModule.backend-batch05.bak"
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

# Prior modules: safely register if missing.
Add-NestModule -ModuleName 'AuthV2Module' -ImportLine "import { AuthV2Module } from './auth-v2/auth-v2.module';"
Add-NestModule -ModuleName 'ProfilesV2Module' -ImportLine "import { ProfilesV2Module } from './profiles-v2/profiles-v2.module';"
Add-NestModule -ModuleName 'DevicesModule' -ImportLine "import { DevicesModule } from './devices/devices.module';"
Add-NestModule -ModuleName 'StreamingConnectionsModule' -ImportLine "import { StreamingConnectionsModule } from './streaming-connections/streaming-connections.module';"
Add-NestModule -ModuleName 'PlatformOauthModule' -ImportLine "import { PlatformOauthModule } from './platform-oauth/platform-oauth.module';"
Add-NestModule -ModuleName 'RtmpCredentialsModule' -ImportLine "import { RtmpCredentialsModule } from './rtmp-credentials/rtmp-credentials.module';"
Add-NestModule -ModuleName 'ScenesV2Module' -ImportLine "import { ScenesV2Module } from './scenes-v2/scenes-v2.module';"
Add-NestModule -ModuleName 'SceneSourcesModule' -ImportLine "import { SceneSourcesModule } from './scene-sources/scene-sources.module';"
Add-NestModule -ModuleName 'AssetsV2Module' -ImportLine "import { AssetsV2Module } from './assets-v2/assets-v2.module';"

# New modules.
Add-NestModule -ModuleName 'StreamConfigV2Module' -ImportLine "import { StreamConfigV2Module } from './stream-config-v2/stream-config-v2.module';"
Add-NestModule -ModuleName 'BroadcastSessionsModule' -ImportLine "import { BroadcastSessionsModule } from './broadcast-sessions/broadcast-sessions.module';"
Add-NestModule -ModuleName 'StreamTelemetryModule' -ImportLine "import { StreamTelemetryModule } from './stream-telemetry/stream-telemetry.module';"
Add-NestModule -ModuleName 'StreamHistoryV2Module' -ImportLine "import { StreamHistoryV2Module } from './stream-history-v2/stream-history-v2.module';"

Set-Content -Path $appModule -Value $text -Encoding UTF8

Set-Location $backend

Write-Host 'Compiling backend...' -ForegroundColor Yellow
npm run build

if ($LASTEXITCODE -ne 0) {
    throw "Backend build failed. Backup: $backup"
}

Write-Host ''
Write-Host 'BACKEND BATCH 05 BUILD SUCCESSFUL' -ForegroundColor Green
