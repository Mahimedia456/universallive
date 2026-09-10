Write-Host "=== ADB ==="
adb devices -l
Write-Host "`n=== Package ==="
adb shell pm list packages | Select-String "universallive"
Write-Host "`n=== Launcher ==="
adb shell cmd package resolve-activity --brief com.universallive.app
Write-Host "`n=== Gradle wrapper ==="
if (Test-Path ".\gradlew.bat") { .\gradlew.bat --version } else { Write-Host "gradlew.bat not generated yet" }
