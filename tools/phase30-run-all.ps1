$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

Write-Host "=== UniversalLive Phase 30 ===" -ForegroundColor Cyan

# 1) Backend
$backend = Join-Path $root "backend"
$envFile = Join-Path $backend ".env"
if (!(Test-Path $envFile)) {
  Write-Host "backend\.env is missing." -ForegroundColor Yellow
  Write-Host "Copy backend\.env.example to backend\.env and fill Supabase/server-only values first." -ForegroundColor Yellow
  exit 2
}

Push-Location $backend
if (!(Test-Path (Join-Path $backend "node_modules"))) {
  Write-Host "Installing backend dependencies..." -ForegroundColor Cyan
  npm install
  if ($LASTEXITCODE -ne 0) { throw "npm install failed" }
}
Write-Host "Building backend..." -ForegroundColor Cyan
npm run build
if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
Pop-Location

$healthOk = $false
try {
  $r = Invoke-RestMethod -Uri "http://127.0.0.1:3000/api/v1/health" -TimeoutSec 2
  if ($r.ok -eq $true) { $healthOk = $true }
} catch {}

if (!$healthOk) {
  Write-Host "Starting backend dev server in a new PowerShell window..." -ForegroundColor Cyan
  Start-Process powershell.exe -ArgumentList @(
    '-NoExit','-ExecutionPolicy','Bypass','-Command',
    "cd '$backend'; npm run dev"
  )
  for ($i=0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 1
    try {
      $r = Invoke-RestMethod -Uri "http://127.0.0.1:3000/api/v1/health" -TimeoutSec 2
      if ($r.ok -eq $true) { $healthOk = $true; break }
    } catch {}
  }
}
if (!$healthOk) { throw "Backend health endpoint did not become ready on port 3000" }
Write-Host "Backend health: OK" -ForegroundColor Green

# 2) Android device
Write-Host "Checking ADB device..." -ForegroundColor Cyan
$devices = adb devices | Select-String "\tdevice$"
if (!$devices) { throw "No authorized Android device found. Connect Wireless debugging/ADB first." }
Write-Host ($devices | ForEach-Object { $_.Line }) -ForegroundColor Green

# 3) Android build/install
if (!(Test-Path ".\gradlew.bat")) {
  $localGradle = Join-Path $env:TEMP "gradle-9.6.1\bin\gradle.bat"
  if (!(Test-Path $localGradle)) { throw "gradlew.bat missing and $localGradle not found" }
  & $localGradle wrapper --gradle-version 9.6.1
  if ($LASTEXITCODE -ne 0) { throw "Gradle wrapper generation failed" }
}

Write-Host "Building Android debug APK..." -ForegroundColor Cyan
.\gradlew.bat :androidApp:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Android build failed" }

Write-Host "Installing Android app..." -ForegroundColor Cyan
.\gradlew.bat :androidApp:installDebug
if ($LASTEXITCODE -ne 0) { throw "Android install failed" }

adb shell am force-stop com.universallive.app | Out-Null
adb shell am start -n com.universallive.app/.MainActivity

Write-Host "UniversalLive 0.30.0 launched." -ForegroundColor Green
Write-Host "Backend: http://127.0.0.1:3000/api/v1/health" -ForegroundColor DarkGray
