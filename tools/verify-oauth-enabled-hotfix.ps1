$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive"
$file = "$root\composeApp\src\commonMain\kotlin\com\universallive\app\components\UlPrimitives.kt"

if (!(Test-Path $file)) { throw "UlPrimitives.kt not found" }

$text = Get-Content $file -Raw

if ($text -notmatch 'fun\s+UlSecondaryButton\([\s\S]*?enabled:\s*Boolean\s*=\s*true') {
    throw "UlSecondaryButton enabled parameter not found"
}

if ($text -notmatch 'OutlinedButton\([\s\S]*?enabled\s*=\s*enabled') {
    throw "OutlinedButton does not receive enabled = enabled"
}

Write-Host "PASS: UlSecondaryButton enabled contract fixed" -ForegroundColor Green

Set-Location $root

.\gradlew.bat :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin
if ($LASTEXITCODE -ne 0) { throw "Android Kotlin compile failed" }

.\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Android debug build failed" }

Write-Host "PASS: Android debug build" -ForegroundColor Green
