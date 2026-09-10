param(
    [switch]$Install
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $Root

Write-Host "Building Universal Live Android debug APK..." -ForegroundColor Cyan
& .\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$apk = Get-ChildItem -Path (Join-Path $Root "androidApp\build\outputs\apk") -Filter "*.apk" -Recurse -File |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if (-not $apk) {
    Write-Host "Build succeeded but no APK was found under androidApp\build\outputs\apk." -ForegroundColor Red
    exit 2
}

Write-Host "" 
Write-Host "DEBUG APK:" -ForegroundColor Green
Write-Host $apk.FullName -ForegroundColor Green

if ($Install) {
    Write-Host "Installing to connected Android device..." -ForegroundColor Cyan
    & adb install -r $apk.FullName
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
