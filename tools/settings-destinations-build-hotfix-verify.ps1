$ErrorActionPreference = "Stop"
$root = "E:\UniversalLive"
$file = "$root\composeApp\src\commonMain\kotlin\com\universallive\app\integration\MobileIntegrationState.kt"
$text = Get-Content $file -Raw
if ($text -match "fun finishConnectionsFlow\(\)\s*\{[\s\S]*?\}\s*\r?\n\s*private set\s*\r?\n\s*var liveTitle") { throw "Orphaned private set still present" }
if ($text -notmatch 'var pendingConnectionPlatform: String by mutableStateOf\("custom_rtmp"\)\s*\r?\n\s*private set') { throw "pendingConnectionPlatform private setter not restored" }
Write-Host "PASS: setter structure fixed" -ForegroundColor Green
Set-Location $root
.\gradlew.bat :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin
if ($LASTEXITCODE -ne 0) { throw "Kotlin compile failed" }
.\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Android build failed" }
Write-Host "PASS: Android debug build" -ForegroundColor Green
