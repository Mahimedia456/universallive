param(
    [string]$Root = "E:\UniversalLive",
    [switch]$SkipInstall,
    [switch]$SkipAndroid
)

$ErrorActionPreference = "Stop"

function Section([string]$Text) {
    Write-Host "`n=== $Text ===" -ForegroundColor Cyan
}

function Require-File([string]$Path) {
    if (!(Test-Path $Path)) { throw "Required file not found: $Path" }
}

Section "1/8 Source sanity"
Require-File "$Root\backend\src\main.ts"
Require-File "$Root\apps\admin\src\AdminApp.tsx"
Require-File "$Root\androidApp\src\main\kotlin\com\universallive\app\streaming\ScreenCaptureService.kt"
Require-File "$Root\composeApp\src\commonMain\kotlin\com\universallive\app\streaming\capture\CaptureController.kt"
Write-Host "PASS: required source files present" -ForegroundColor Green

Section "2/8 Production-content scan"
$roots = @(
    "$Root\composeApp\src",
    "$Root\androidApp\src",
    "$Root\apps\admin\src"
)
$visiblePhaseHits = @()
foreach ($src in $roots) {
    if (!(Test-Path $src)) { continue }
    $files = Get-ChildItem $src -Recurse -File -Include *.kt,*.ts,*.tsx,*.js,*.jsx
    foreach ($file in $files) {
        $matches = Select-String -Path $file.FullName -Pattern '"[^"\r\n]*(Phase|phase)\s*[0-9]+' -AllMatches
        foreach ($m in $matches) { $visiblePhaseHits += "$($file.FullName):$($m.LineNumber): $($m.Line.Trim())" }
    }
}
if ($visiblePhaseHits.Count -gt 0) {
    $visiblePhaseHits | ForEach-Object { Write-Host $_ -ForegroundColor Yellow }
    throw "Production-visible Phase-number content still exists"
}
Write-Host "PASS: no production-visible Phase-number strings" -ForegroundColor Green

Section "3/8 Admin stale JS/TS collision scan"
$adminSrc = "$Root\apps\admin\src"
$collisions = @()
Get-ChildItem $adminSrc -Recurse -File -Filter *.ts | ForEach-Object {
    $js = [System.IO.Path]::ChangeExtension($_.FullName, ".js")
    if (Test-Path $js) { $collisions += $js }
}
Get-ChildItem $adminSrc -Recurse -File -Filter *.tsx | ForEach-Object {
    foreach ($candidate in @(
        [System.IO.Path]::ChangeExtension($_.FullName, ".js"),
        [System.IO.Path]::ChangeExtension($_.FullName, ".jsx")
    )) {
        if (Test-Path $candidate) { $collisions += $candidate }
    }
}
if ($collisions.Count -gt 0) {
    $collisions | Sort-Object -Unique | ForEach-Object { Write-Host $_ -ForegroundColor Red }
    throw "Stale JS/TS source collisions remain"
}
Write-Host "PASS: no stale admin JS/TS collisions" -ForegroundColor Green

Section "4/8 Backend build"
Set-Location "$Root\backend"
if (!$SkipInstall) { npm ci }
npm run build
if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
if (!(Test-Path "$Root\backend\dist\main.js")) { throw "Backend build passed but dist\main.js is missing" }
Write-Host "PASS: backend build + dist\main.js" -ForegroundColor Green

Section "5/8 Admin build"
Set-Location "$Root\apps\admin"
if (!$SkipInstall) { npm ci }
npm run build
if ($LASTEXITCODE -ne 0) { throw "Admin build failed" }
Write-Host "PASS: admin production build" -ForegroundColor Green

Section "6/8 Streaming source contract"
$screen = Get-Content "$Root\androidApp\src\main\kotlin\com\universallive\app\streaming\ScreenCaptureService.kt" -Raw
$compositor = Get-Content "$Root\androidApp\src\main\kotlin\com\universallive\app\streaming\StreamCompositor.kt" -Raw
$capture = Get-Content "$Root\composeApp\src\commonMain\kotlin\com\universallive\app\streaming\capture\CaptureController.kt" -Raw
foreach ($token in @('EXTRA_RTMP_TARGETS_JSON','cameraPrimaryMode','requestedOrientationMode','rtmpPublishers')) {
    if (!$screen.Contains($token)) { throw "Streaming source missing token: $token" }
}
foreach ($token in @('primaryCamera','mirrored = false','updateScreenInputSize')) {
    if (!$compositor.Contains($token)) { throw "Compositor source missing token: $token" }
}
foreach ($token in @('VideoSourceMode','PublishTargetConfig','requestedPublishTargets','requestedOrientation: String = "Auto"')) {
    if (!$capture.Contains($token)) { throw "Capture controller missing token: $token" }
}
Write-Host "PASS: multi-destination, camera-primary, auto-orientation and unmirrored output source contract" -ForegroundColor Green

Section "7/8 Android debug build"
if ($SkipAndroid) {
    Write-Host "SKIP: Android build requested to be skipped" -ForegroundColor Yellow
} else {
    Require-File "$Root\gradlew.bat"
    Set-Location $Root
    & "$Root\gradlew.bat" :androidApp:processDebugGoogleServices :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin :androidApp:assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Android debug build failed" }

    $apkCandidates = @(
        "$Root\androidApp\build\outputs\apk\debug\androidApp-debug.apk",
        "$Root\androidApp\build\outputs\apk\debug\app-debug.apk"
    )
    $apk = $apkCandidates | Where-Object { Test-Path $_ } | Select-Object -First 1
    if (!$apk) { throw "Gradle passed but debug APK was not found" }

    New-Item -ItemType Directory -Force "$Root\artifacts" | Out-Null
    Copy-Item $apk "$Root\artifacts\UniversalLive-final-debug.apk" -Force
    Write-Host "PASS: Android debug APK -> $Root\artifacts\UniversalLive-final-debug.apk" -ForegroundColor Green
}

Section "8/8 Final"
Write-Host "PASS: Universal Live final mobile + backend + admin verification completed" -ForegroundColor Green
