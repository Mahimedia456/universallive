$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$backend = Join-Path $root 'backend'
$appModule = Join-Path $backend 'src\app.module.ts'

if (-not (Test-Path $appModule)) { throw "app.module.ts not found: $appModule" }

Write-Host '=== UniversalLive Backend FINAL Batch — Phase 19/20 ===' -ForegroundColor Cyan

$backup = "$appModule.backend-batch07-final.bak"
if (-not (Test-Path $backup)) { Copy-Item $appModule $backup }

$text = Get-Content -Path $appModule -Raw

function Add-NestModule {
    param([string]$ModuleName, [string]$ImportLine)

    if ($script:text.Contains($ModuleName)) {
        Write-Host "[SKIP] $ModuleName" -ForegroundColor DarkGray
        return
    }

    $moduleIndex = $script:text.IndexOf('@Module(')
    if ($moduleIndex -lt 0) { throw 'Could not locate @Module(' }

    $script:text =
        $script:text.Substring(0, $moduleIndex) +
        $ImportLine +
        [Environment]::NewLine +
        [Environment]::NewLine +
        $script:text.Substring($moduleIndex)

    $importsIndex = $script:text.IndexOf('imports: [')
    if ($importsIndex -lt 0) { throw 'Could not locate imports: [' }

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

Add-NestModule 'AuthV2Module' "import { AuthV2Module } from './auth-v2/auth-v2.module';"
Add-NestModule 'ProfilesV2Module' "import { ProfilesV2Module } from './profiles-v2/profiles-v2.module';"
Add-NestModule 'DevicesModule' "import { DevicesModule } from './devices/devices.module';"
Add-NestModule 'StreamingConnectionsModule' "import { StreamingConnectionsModule } from './streaming-connections/streaming-connections.module';"
Add-NestModule 'PlatformOauthModule' "import { PlatformOauthModule } from './platform-oauth/platform-oauth.module';"
Add-NestModule 'RtmpCredentialsModule' "import { RtmpCredentialsModule } from './rtmp-credentials/rtmp-credentials.module';"
Add-NestModule 'ScenesV2Module' "import { ScenesV2Module } from './scenes-v2/scenes-v2.module';"
Add-NestModule 'SceneSourcesModule' "import { SceneSourcesModule } from './scene-sources/scene-sources.module';"
Add-NestModule 'AssetsV2Module' "import { AssetsV2Module } from './assets-v2/assets-v2.module';"
Add-NestModule 'StreamConfigV2Module' "import { StreamConfigV2Module } from './stream-config-v2/stream-config-v2.module';"
Add-NestModule 'BroadcastSessionsModule' "import { BroadcastSessionsModule } from './broadcast-sessions/broadcast-sessions.module';"
Add-NestModule 'StreamTelemetryModule' "import { StreamTelemetryModule } from './stream-telemetry/stream-telemetry.module';"
Add-NestModule 'StreamHistoryV2Module' "import { StreamHistoryV2Module } from './stream-history-v2/stream-history-v2.module';"
Add-NestModule 'EntitlementsModule' "import { EntitlementsModule } from './entitlements/entitlements.module';"
Add-NestModule 'BillingModule' "import { BillingModule } from './billing/billing.module';"
Add-NestModule 'NotificationsV2Module' "import { NotificationsV2Module } from './notifications-v2/notifications-v2.module';"
Add-NestModule 'SupportV2Module' "import { SupportV2Module } from './support-v2/support-v2.module';"
Add-NestModule 'SecurityAuditModule' "import { SecurityAuditModule } from './security-audit/security-audit.module';"
Add-NestModule 'RealtimeFinalModule' "import { RealtimeFinalModule } from './realtime-final/realtime-final.module';"

Set-Content -Path $appModule -Value $text -Encoding UTF8

Set-Location $backend
npm run build
if ($LASTEXITCODE -ne 0) { throw "Backend build failed. Backup: $backup" }

Write-Host 'FINAL BACKEND BUILD SUCCESSFUL' -ForegroundColor Green
