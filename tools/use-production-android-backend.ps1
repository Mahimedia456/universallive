param(
    [string]$ProjectRoot = "E:\UniversalLive"
)

$ErrorActionPreference = "Stop"
$gradle = Join-Path $ProjectRoot "gradle.properties"
if (-not (Test-Path $gradle)) { throw "gradle.properties not found: $gradle" }

$productionUrl = "https://universallive.vercel.app/api/v1"
$content = Get-Content $gradle -Raw
if ($content -match '(?m)^UL_API_BASE_URL=.*$') {
    $content = [regex]::Replace($content, '(?m)^UL_API_BASE_URL=.*$', "UL_API_BASE_URL=$productionUrl")
} else {
    $content = $content.TrimEnd() + "`r`nUL_API_BASE_URL=$productionUrl`r`n"
}
Set-Content $gradle $content -Encoding UTF8
Write-Host "Android production backend: $productionUrl" -ForegroundColor Green
Write-Host "Rebuild/reinstall the Android app after changing UL_API_BASE_URL." -ForegroundColor Yellow
