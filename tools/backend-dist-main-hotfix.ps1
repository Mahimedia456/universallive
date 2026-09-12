$ErrorActionPreference = "Stop"

$root = "E:\UniversalLive\backend"

Write-Host "[1/7] Stop stale backend Node processes" -ForegroundColor Cyan
Get-CimInstance Win32_Process -Filter "Name='node.exe'" -ErrorAction SilentlyContinue |
  Where-Object { $_.CommandLine -like "*E:\UniversalLive\backend*" } |
  ForEach-Object {
    Write-Host "Stopping PID $($_.ProcessId): $($_.CommandLine)" -ForegroundColor Yellow
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
  }

Write-Host "[2/7] Remove dist and TypeScript incremental state" -ForegroundColor Cyan
Set-Location $root

if (Test-Path "$root\dist") {
    cmd.exe /c "rmdir /s /q `"$root\dist`""
}

Get-ChildItem $root -Recurse -File -Include *.tsbuildinfo -ErrorAction SilentlyContinue |
  ForEach-Object {
    Write-Host "Removing $($_.FullName)" -ForegroundColor DarkGray
    Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue
  }

Write-Host "[3/7] Show active TypeScript/Nest configuration" -ForegroundColor Cyan
if (Test-Path "$root\tsconfig.json") {
    Write-Host "--- tsconfig.json ---"
    Get-Content "$root\tsconfig.json"
}
if (Test-Path "$root\tsconfig.build.json") {
    Write-Host "--- tsconfig.build.json ---"
    Get-Content "$root\tsconfig.build.json"
}
if (Test-Path "$root\nest-cli.json") {
    Write-Host "--- nest-cli.json ---"
    Get-Content "$root\nest-cli.json"
}

Write-Host "[4/7] Clean production build" -ForegroundColor Cyan
npm run build
if ($LASTEXITCODE -ne 0) {
    throw "Backend production build failed"
}

Write-Host "[5/7] Locate emitted main.js" -ForegroundColor Cyan
$mainCandidates = @(
    "$root\dist\main.js",
    "$root\dist\src\main.js"
)

$main = $null
foreach ($candidate in $mainCandidates) {
    if (Test-Path $candidate) {
        $main = $candidate
        break
    }
}

if (-not $main) {
    $found = Get-ChildItem "$root\dist" -Recurse -File -Filter main.js -ErrorAction SilentlyContinue |
      Select-Object -First 1
    if ($found) {
        $main = $found.FullName
    }
}

if (-not $main) {
    Write-Host "Files emitted under dist:" -ForegroundColor Yellow
    Get-ChildItem "$root\dist" -Recurse -File -ErrorAction SilentlyContinue |
      Select-Object -First 80 |
      ForEach-Object { Write-Host "  $($_.FullName)" }
    throw "Build completed but no emitted main.js was found"
}

Write-Host "Found backend entry: $main" -ForegroundColor Green

Write-Host "[6/7] Normalize Nest runtime entry if necessary" -ForegroundColor Cyan
$expected = "$root\dist\main.js"

if ($main -ne $expected) {
    if ($main -eq "$root\dist\src\main.js") {
        Write-Host "Detected dist\src\main.js layout." -ForegroundColor Yellow

        # The project runtime expects dist\main.js. Fix build layout at the source:
        # tsconfig.build.json should explicitly use rootDir ./src and outDir ./dist.
        $buildConfig = "$root\tsconfig.build.json"

        if (!(Test-Path $buildConfig)) {
            @'
{
  "extends": "./tsconfig.json",
  "compilerOptions": {
    "rootDir": "./src",
    "outDir": "./dist"
  },
  "exclude": ["node_modules", "test", "dist", "**/*spec.ts"]
}
'@ | Set-Content -Path $buildConfig -Encoding UTF8
        } else {
            $json = Get-Content $buildConfig -Raw | ConvertFrom-Json
            if (-not $json.compilerOptions) {
                $json | Add-Member -NotePropertyName compilerOptions -NotePropertyValue ([pscustomobject]@{})
            }
            $json.compilerOptions | Add-Member -NotePropertyName rootDir -NotePropertyValue "./src" -Force
            $json.compilerOptions | Add-Member -NotePropertyName outDir -NotePropertyValue "./dist" -Force
            $json | ConvertTo-Json -Depth 20 | Set-Content -Path $buildConfig -Encoding UTF8
        }

        if (Test-Path "$root\dist") {
            cmd.exe /c "rmdir /s /q `"$root\dist`""
        }
        Get-ChildItem $root -Recurse -File -Include *.tsbuildinfo -ErrorAction SilentlyContinue |
          Remove-Item -Force -ErrorAction SilentlyContinue

        npm run build
        if ($LASTEXITCODE -ne 0) {
            throw "Backend rebuild after output-layout fix failed"
        }

        if (!(Test-Path $expected)) {
            throw "Backend still does not emit dist\main.js after tsconfig.build.json normalization"
        }
        Write-Host "PASS: backend now emits dist\main.js" -ForegroundColor Green
    } else {
        throw "Unexpected backend entry layout: $main"
    }
} else {
    Write-Host "PASS: backend emits expected dist\main.js" -ForegroundColor Green
}

Write-Host "[7/7] Runtime smoke test" -ForegroundColor Cyan
$proc = Start-Process -FilePath "node.exe" `
  -ArgumentList @("$expected") `
  -WorkingDirectory $root `
  -PassThru `
  -RedirectStandardOutput "$root\.backend-smoke-out.log" `
  -RedirectStandardError "$root\.backend-smoke-err.log"

Start-Sleep -Seconds 4

if ($proc.HasExited) {
    Write-Host "--- stdout ---"
    if (Test-Path "$root\.backend-smoke-out.log") { Get-Content "$root\.backend-smoke-out.log" }
    Write-Host "--- stderr ---"
    if (Test-Path "$root\.backend-smoke-err.log") { Get-Content "$root\.backend-smoke-err.log" }
    throw "dist\main.js exists but backend exited during runtime smoke test"
}

Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "PASS: clean backend build" -ForegroundColor Green
Write-Host "PASS: dist\main.js exists" -ForegroundColor Green
Write-Host "PASS: backend runtime entry starts" -ForegroundColor Green
Write-Host ""
Write-Host "Now start development mode:" -ForegroundColor Cyan
Write-Host "  cd E:\UniversalLive\backend"
Write-Host "  npm run dev"
