param(
    [string]$ProjectRoot = "E:\UniversalLive",
    [int]$Port = 3000
)

$ErrorActionPreference = "Stop"
$gradle = Join-Path $ProjectRoot "gradle.properties"
if (-not (Test-Path $gradle)) { throw "gradle.properties not found: $gradle" }

$localUrl = "http://127.0.0.1:$Port/api/v1"
$content = Get-Content $gradle -Raw
if ($content -match '(?m)^UL_API_BASE_URL=.*$') {
    $content = [regex]::Replace($content, '(?m)^UL_API_BASE_URL=.*$', "UL_API_BASE_URL=$localUrl")
} else {
    $content = $content.TrimEnd() + "`r`nUL_API_BASE_URL=$localUrl`r`n"
}
Set-Content $gradle $content -Encoding UTF8

$adb = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adb) {
    Write-Warning "adb is not in PATH. Add Android platform-tools to PATH, then run: adb reverse tcp:$Port tcp:$Port"
} else {
    & adb reverse "tcp:$Port" "tcp:$Port"
    if ($LASTEXITCODE -ne 0) { throw "adb reverse failed. Connect the Android phone with USB debugging enabled." }
    Write-Host "PASS: adb reverse tcp:$Port -> desktop tcp:$Port" -ForegroundColor Green
}

Write-Host "Android debug backend: $localUrl" -ForegroundColor Cyan
Write-Host "Rebuild/reinstall the Android app after changing UL_API_BASE_URL." -ForegroundColor Yellow
