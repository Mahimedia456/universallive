param(
    [switch]$Apply,
    [switch]$IncludeBackups
)

$ErrorActionPreference = "Stop"

# This script is expected at:
# E:\UniversalLive\tools\cleanup-unused-legacy.ps1
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$Backend = Join-Path $Root "backend"

Write-Host "Universal Live safe cleanup" -ForegroundColor Cyan
Write-Host "Root:    $Root"
Write-Host "Backend: $Backend"
Write-Host "Mode:    $(if ($Apply) { 'APPLY' } else { 'DRY RUN' })" -ForegroundColor Yellow

# Safety guard: never clean an unexpected folder.
$requiredProjectMarkers = @(
    "settings.gradle.kts",
    "build.gradle.kts",
    "composeApp",
    "androidApp"
)

$missingMarkers = $requiredProjectMarkers | Where-Object {
    -not (Test-Path (Join-Path $Root $_))
}

if ($missingMarkers.Count -gt 0) {
    Write-Host ""
    Write-Host "Cleanup stopped: this does not look like the UniversalLive project root." -ForegroundColor Red
    $missingMarkers | ForEach-Object { Write-Host "  Missing: $_" }
    exit 2
}

# Replacement screens must exist before legacy screens can be removed.
$requiredNewScreens = @(
    "composeApp\src\commonMain\kotlin\com\universallive\app\features\integration\ConnectedConnectionsStudio.kt",
    "composeApp\src\commonMain\kotlin\com\universallive\app\features\integration\ConnectedGoLiveSetup.kt",
    "composeApp\src\commonMain\kotlin\com\universallive\app\features\batch2\ConnectionSupportScreens.kt"
)

$missingScreens = $requiredNewScreens | Where-Object {
    -not (Test-Path (Join-Path $Root $_))
}

if ($missingScreens.Count -gt 0) {
    Write-Host ""
    Write-Host "Cleanup stopped: new replacement screens are missing." -ForegroundColor Red
    $missingScreens | ForEach-Object { Write-Host "  Missing: $_" }
    exit 3
}

$mobileTargets = @(
    # Superseded legacy UI. Active navigation now uses connected/backend screens.
    "composeApp\src\commonMain\kotlin\com\universallive\app\features\connections\ConnectionsScreen.kt",
    "composeApp\src\commonMain\kotlin\com\universallive\app\features\golive\GoLiveScreen.kt",
    "composeApp\src\commonMain\kotlin\com\universallive\app\features\batch2\Phase05Connections.kt",
    "composeApp\src\commonMain\kotlin\com\universallive\app\features\batch4\Phase11GoLiveSetup.kt",

    # Regenerable Gradle/Kotlin build caches.
    ".gradle",
    ".kotlin",
    "build",
    "androidApp\build",
    "composeApp\build"
)

function Remove-SafeTarget([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    if ($Apply) {
        Remove-Item -LiteralPath $Path -Recurse -Force
        Write-Host "Removed: $Path" -ForegroundColor Green
    } else {
        Write-Host "Would remove: $Path" -ForegroundColor DarkYellow
    }
}

foreach ($relative in $mobileTargets) {
    Remove-SafeTarget (Join-Path $Root $relative)
}

if (Test-Path -LiteralPath $Backend) {
    # Compiled backend output only; source and .env are preserved.
    Remove-SafeTarget (Join-Path $Backend "dist")

    if ($IncludeBackups) {
        $backendSrc = Join-Path $Backend "src"
        if (Test-Path -LiteralPath $backendSrc) {
            Get-ChildItem -Path $backendSrc -Filter "*.bak" -File -Recurse -ErrorAction SilentlyContinue |
                ForEach-Object { Remove-SafeTarget $_.FullName }
        }
    }
} else {
    Write-Host "Backend folder not found; skipped backend cleanup: $Backend" -ForegroundColor Yellow
}

Write-Host ""
if (-not $Apply) {
    Write-Host "Dry run complete. Nothing was deleted." -ForegroundColor Cyan
    Write-Host "If the list is correct, run:" -ForegroundColor Cyan
    Write-Host "  .\tools\cleanup-unused-legacy.ps1 -Apply"
} else {
    Write-Host "Cleanup complete." -ForegroundColor Cyan
    Write-Host "Next: rebuild the mobile app and backend." -ForegroundColor Cyan
}
