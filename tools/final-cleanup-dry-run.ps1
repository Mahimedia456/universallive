param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path $ProjectRoot).Path

$targets = New-Object System.Collections.Generic.List[string]

$exactDirs = @(
    ".gradle",
    ".idea",
    "androidApp\build",
    "composeApp\build",
    "backend\dist",
    "backend\node_modules"
)
foreach ($rel in $exactDirs) {
    $p = Join-Path $ProjectRoot $rel
    if (Test-Path $p) { $targets.Add($p) }
}

Get-ChildItem $ProjectRoot -Recurse -Force -File -ErrorAction SilentlyContinue |
    Where-Object {
        $_.Extension -in @('.bak','.tmp','.log') -or
        $_.Name -match '~$' -or
        ($_.Extension -eq '.zip' -and $_.FullName -notmatch '\\.git\\')
    } |
    ForEach-Object { $targets.Add($_.FullName) }

$legacyRootFiles = @(
    'BUILD_FIX_README.txt','BUILD_FIX_17_33_README.txt','GRADLE_WRAPPER_NOTE.txt','run.txt',
    'PHASE_01_SPLASH_README.txt','PHASE_02_WELCOME_README.txt','PHASE_07_CREATOR_ONBOARDING_README.txt',
    'PHASE_08_10_LOCKED_README.txt','PHASE_10_12_LOCKED_README.txt','PHASE_13_16_LOCKED_README.txt',
    'PHASE_17_25_LOCKED_README.txt','PHASE_26_33_LOCKED_README.txt','PHASE_34_39_LOCKED_README.txt',
    'UNIVERSAL_LIVE_AUTH_01_06_LOCKED_README.txt','UNIVERSAL_LIVE_FIXES_README.txt',
    'UNIVERSAL_LIVE_STREAMING_HOTFIX_README.txt','UNIVERSAL_LIVE_STREAM_STABILITY_V2_README.txt'
)
foreach ($name in $legacyRootFiles) {
    $p = Join-Path $ProjectRoot $name
    if (Test-Path $p) { $targets.Add($p) }
}

$legacyTools = @(
    'backend-phase00-01-verify.ps1','backend-phase07-12-verify.ps1','backend-phase13-16-verify.ps1',
    'backend-phase17-25-verify.ps1','backend-phase26-33-verify.ps1'
)
foreach ($name in $legacyTools) {
    $p = Join-Path (Join-Path $ProjectRoot 'tools') $name
    if (Test-Path $p) { $targets.Add($p) }
}

Get-ChildItem $ProjectRoot -Recurse -Force -File -Filter '.gitkeep' -ErrorAction SilentlyContinue |
    ForEach-Object { $targets.Add($_.FullName) }

$placeholder = Join-Path $ProjectRoot 'composeApp\src\commonMain\kotlin\com\universallive\app\features\placeholder\PlaceholderScreen.kt'
if (Test-Path $placeholder) {
    $otherRefs = Get-ChildItem (Join-Path $ProjectRoot 'composeApp\src') -Recurse -Filter '*.kt' -File |
        Where-Object { $_.FullName -ne $placeholder } |
        Select-String -Pattern 'PlaceholderScreen' -SimpleMatch -Quiet
    if (-not $otherRefs) { $targets.Add($placeholder) }
}

$unique = @($targets | Sort-Object -Unique)
Write-Host "Universal Live FINAL cleanup dry-run" -ForegroundColor Cyan
Write-Host "Project: $ProjectRoot"
Write-Host "Candidates: $($unique.Count)" -ForegroundColor Yellow
$unique | ForEach-Object { Write-Host "  REMOVE  $_" }
Write-Host "`nNothing was deleted. Run tools\final-cleanup-apply.ps1 -ConfirmCleanup after reviewing this list." -ForegroundColor Green
