param(
    [Parameter(Mandatory=$true)][string]$ServiceAccountJson,
    [string]$EnvPath = (Join-Path (Split-Path -Parent $PSScriptRoot) 'backend\.env')
)

$ErrorActionPreference = 'Stop'
$source = (Resolve-Path $ServiceAccountJson).Path
$sa = Get-Content $source -Raw | ConvertFrom-Json
if ([string]$sa.project_id -ne 'universallive-d5d90') { throw 'This service-account file is not for universallive-d5d90.' }
if ([string]::IsNullOrWhiteSpace([string]$sa.private_key)) { throw 'private_key is missing from the service-account JSON.' }

if (-not (Test-Path $EnvPath)) {
    $example = Join-Path (Split-Path -Parent $PSScriptRoot) 'backend\.env.example'
    Copy-Item $example $EnvPath
}

$content = Get-Content $EnvPath -Raw
$key = ([string]$sa.private_key).Replace("`r", '').Replace("`n", '\n')
$values = [ordered]@{
    FIREBASE_PROJECT_ID = [string]$sa.project_id
    FIREBASE_CLIENT_EMAIL = [string]$sa.client_email
    FIREBASE_PRIVATE_KEY = '"' + $key + '"'
    GOOGLE_PLAY_PACKAGE_NAME = 'com.universallive.app'
}

foreach ($name in $values.Keys) {
    $value = $values[$name]
    $pattern = '(?m)^' + [regex]::Escape($name) + '=.*$'
    $line = "$name=$value"
    if ([regex]::IsMatch($content, $pattern)) {
        $content = [regex]::Replace($content, $pattern, [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $line })
    } else {
        $content = $content.TrimEnd() + "`r`n$line`r`n"
    }
}

Set-Content -Path $EnvPath -Value $content -Encoding UTF8
Write-Host "Firebase backend credentials written to $EnvPath" -ForegroundColor Green
Write-Host "Private key was not printed. Keep the service-account JSON outside Git and rotate any key previously exposed." -ForegroundColor Yellow
