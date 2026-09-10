$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$apk = Join-Path $root 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'

Write-Host '=== UniversalLive FINAL Mobile Build Repair ===' -ForegroundColor Cyan
Write-Host 'Repairs corrupted Kotlin/Gradle incremental caches and rebuilds Android.' -ForegroundColor Cyan

Set-Location $root

Write-Host ''
Write-Host 'Stopping Gradle daemons...' -ForegroundColor Yellow
& .\gradlew.bat --stop | Out-Host

Write-Host ''
Write-Host 'Removing corrupted project build caches...' -ForegroundColor Yellow

$paths = @(
    (Join-Path $root '.gradle'),
    (Join-Path $root 'build'),
    (Join-Path $root 'androidApp\build'),
    (Join-Path $root 'composeApp\build')
)

foreach ($path in $paths) {
    if (Test-Path $path) {
        Write-Host "  removing $path"
        Remove-Item $path -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ''
Write-Host 'Clearing Kotlin daemon cache for this project when present...' -ForegroundColor Yellow

$kotlinDirs = @(
    (Join-Path $env:LOCALAPPDATA 'kotlin'),
    (Join-Path $env:USERPROFILE '.kotlin')
)

foreach ($path in $kotlinDirs) {
    if (Test-Path $path) {
        Get-ChildItem $path -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match 'daemon|compile|cache' } |
            ForEach-Object {
                Remove-Item $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
            }
    }
}

Write-Host ''
Write-Host 'Running clean Android compile without Gradle build cache...' -ForegroundColor Yellow

& .\gradlew.bat clean --no-build-cache
if ($LASTEXITCODE -ne 0) {
    throw 'Gradle clean failed'
}

& .\gradlew.bat :composeApp:compileAndroidMain --no-build-cache --rerun-tasks
if ($LASTEXITCODE -ne 0) {
    throw 'composeApp Android compilation failed'
}

Write-Host ''
Write-Host 'Building fresh Android debug APK...' -ForegroundColor Yellow

& .\gradlew.bat :androidApp:assembleDebug --no-build-cache --rerun-tasks
if ($LASTEXITCODE -ne 0) {
    throw 'Android debug APK build failed'
}

if (-not (Test-Path $apk)) {
    throw "APK was not generated: $apk"
}

Write-Host ''
Write-Host 'ANDROID DEBUG APK BUILD SUCCESSFUL' -ForegroundColor Green
Get-Item $apk | Select-Object FullName, Length, LastWriteTime
