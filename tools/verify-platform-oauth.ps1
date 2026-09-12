param(
  [string]$Root = "E:\UniversalLive",
  [switch]$SkipAndroid
)

$ErrorActionPreference = "Stop"

Write-Host "=== Universal Live OAuth verification ===" -ForegroundColor Cyan

$service = Join-Path $Root "backend\src\platform-oauth\platform-oauth.service.ts"
$mobileApi = Join-Path $Root "composeApp\src\commonMain\kotlin\com\universallive\app\integration\MobileBackendApi.kt"
$screen = Join-Path $Root "composeApp\src\commonMain\kotlin\com\universallive\app\features\connections\Phase10ConnectionsScreens.kt"

foreach ($file in @($service,$mobileApi,$screen)) {
  if (!(Test-Path $file)) { throw "Missing required file: $file" }
}

$serviceText = Get-Content $service -Raw
foreach ($token in @(
  "streaming/oauth",
  "provisionTwitchRoute",
  "provisionYouTubeRoute",
  "OAUTH_CALLBACK_BASE_URL",
  "ul_oauth_credentials"
)) {
  if ($serviceText -notmatch [regex]::Escape($token)) { throw "OAuth backend token missing: $token" }
}

$screenText = Get-Content $screen -Raw
foreach ($token in @(
  "Connect with",
  "Use Custom RTMP",
  "startPlatformOAuth",
  "platformOAuthStatus"
)) {
  if ($screenText -notmatch [regex]::Escape($token)) { throw "OAuth mobile flow token missing: $token" }
}

Write-Host "PASS: OAuth source contract" -ForegroundColor Green

Push-Location (Join-Path $Root "backend")
try {
  npm run build
  if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
} finally { Pop-Location }
Write-Host "PASS: backend production build" -ForegroundColor Green

if (!$SkipAndroid) {
  Push-Location $Root
  try {
    .\gradlew.bat :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin
    if ($LASTEXITCODE -ne 0) { throw "Android Kotlin compile failed" }
    .\gradlew.bat :androidApp:assembleDebug
    if ($LASTEXITCODE -ne 0) { throw "Android debug build failed" }
  } finally { Pop-Location }
  Write-Host "PASS: Android debug build" -ForegroundColor Green
}

Write-Host "PASS: OAuth patch verification complete" -ForegroundColor Green
