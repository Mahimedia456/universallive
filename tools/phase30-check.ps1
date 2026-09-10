$ErrorActionPreference = "Continue"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
Write-Host "UniversalLive Phase 30 diagnostics" -ForegroundColor Cyan
Write-Host "--- ADB ---"; adb devices
Write-Host "--- Java ---"; java -version
Write-Host "--- Node ---"; node --version; npm --version
Write-Host "--- Gradle ---"; if (Test-Path .\gradlew.bat) { .\gradlew.bat --version }
Write-Host "--- Backend health ---"; try { Invoke-RestMethod http://127.0.0.1:3000/api/v1/health } catch { $_.Exception.Message }
Write-Host "--- APK ---"; Get-ChildItem .\androidApp\build\outputs\apk\debug\*.apk -ErrorAction SilentlyContinue | Select-Object FullName,Length,LastWriteTime
