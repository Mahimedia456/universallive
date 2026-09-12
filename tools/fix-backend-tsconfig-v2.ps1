$ErrorActionPreference = "Stop"

$path = "E:\UniversalLive\backend\tsconfig.json"

if (!(Test-Path $path)) {
    throw "Backend tsconfig not found: $path"
}

$raw = Get-Content $path -Raw

# rootDir is required by the current TypeScript migration diagnostic.
if ($raw -match '"rootDir"\s*:') {
    $raw = [regex]::Replace(
        $raw,
        '"rootDir"\s*:\s*"[^"]*"',
        '"rootDir": "./src"',
        1
    )
} else {
    $raw = [regex]::Replace(
        $raw,
        '"compilerOptions"\s*:\s*\{',
        '"compilerOptions": {' + "`r`n    " + '"rootDir": "./src",',
        1
    )
}

# The installed TypeScript version rejects ignoreDeprecations: "6.0".
# Remove ignoreDeprecations entirely instead of guessing another version.
$raw = [regex]::Replace(
    $raw,
    '(?m)^\s*"ignoreDeprecations"\s*:\s*"[^"]*"\s*,?\s*\r?\n',
    ''
)

# Clean up a possible dangling comma before the closing compilerOptions brace.
$raw = [regex]::Replace(
    $raw,
    ',(\s*\})',
    '$1'
)

Set-Content -Path $path -Value $raw -Encoding UTF8

Write-Host "PASS: backend tsconfig rootDir fixed" -ForegroundColor Green
Write-Host "PASS: invalid ignoreDeprecations option removed" -ForegroundColor Green
