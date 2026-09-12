$ErrorActionPreference = "Stop"
$path = "E:\UniversalLive\backend\tsconfig.json"
if (!(Test-Path $path)) { throw "Backend tsconfig not found: $path" }

$raw = Get-Content $path -Raw

# Explicit rootDir required by newer TypeScript migration checks.
if ($raw -match '"rootDir"\s*:') {
  $raw = [regex]::Replace($raw, '"rootDir"\s*:\s*"[^"]*"', '"rootDir": "./src"', 1)
} else {
  $raw = [regex]::Replace($raw, '"compilerOptions"\s*:\s*\{', '"compilerOptions": {' + "`r`n    " + '"rootDir": "./src",', 1)
}

# Current project may still rely on baseUrl for absolute imports.
# Keep it working under current TS while silencing the TS6 deprecation warning.
if ($raw -match '"ignoreDeprecations"\s*:') {
  $raw = [regex]::Replace($raw, '"ignoreDeprecations"\s*:\s*"[^"]*"', '"ignoreDeprecations": "6.0"', 1)
} else {
  $raw = [regex]::Replace($raw, '"compilerOptions"\s*:\s*\{', '"compilerOptions": {' + "`r`n    " + '"ignoreDeprecations": "6.0",', 1)
}

Set-Content -Path $path -Value $raw -Encoding UTF8
Write-Host "PASS: backend tsconfig rootDir/TypeScript compatibility updated" -ForegroundColor Green
