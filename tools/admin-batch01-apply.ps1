$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$appModule = Join-Path $root 'backend\src\app.module.ts'

Write-Host '=== UniversalLive Admin Batch 01 ===' -ForegroundColor Cyan

if (-not (Test-Path $appModule)) {
    throw "Missing $appModule"
}

$content = Get-Content $appModule -Raw

if ($content -notmatch "AdminConsoleModule") {
    $content = "import { AdminConsoleModule } from './admin-console/admin-console.module';`r`n" + $content

    if ($content -match 'imports\s*:\s*\[') {
        $content = [regex]::Replace(
            $content,
            'imports\s*:\s*\[',
            'imports: [AdminConsoleModule, ',
            1
        )
    } else {
        throw 'Could not find imports array in app.module.ts'
    }

    Set-Content $appModule $content -Encoding UTF8
    Write-Host '[ADD] AdminConsoleModule' -ForegroundColor Green
} else {
    Write-Host '[OK] AdminConsoleModule already registered' -ForegroundColor DarkGray
}

Set-Location (Join-Path $root 'backend')
npm run build
if ($LASTEXITCODE -ne 0) { throw 'Backend build failed' }

Set-Location (Join-Path $root 'apps\admin')
npm install
if ($LASTEXITCODE -ne 0) { throw 'Admin npm install failed' }

npm run build
if ($LASTEXITCODE -ne 0) { throw 'Admin build failed' }

Write-Host ''
Write-Host 'ADMIN BATCH 01 BUILD SUCCESSFUL' -ForegroundColor Green
