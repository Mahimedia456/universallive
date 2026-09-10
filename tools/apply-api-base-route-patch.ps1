$ErrorActionPreference = 'Stop'

$root = 'E:\UniversalLive'
$backend = Join-Path $root 'backend'
$appModule = Join-Path $backend 'src\app.module.ts'
$controllerSource = Join-Path $root 'backend\src\app.controller.ts'

Write-Host '=== UniversalLive API Base Route Patch ===' -ForegroundColor Cyan

if (-not (Test-Path $appModule)) {
    throw "Missing AppModule: $appModule"
}

if (-not (Test-Path $controllerSource)) {
    throw "Missing AppController: $controllerSource"
}

$content = Get-Content $appModule -Raw

# Add import if it does not already exist.
if ($content -notmatch "import\s+\{\s*AppController\s*\}\s+from\s+['""]\.\/app\.controller['""]") {
    $content = "import { AppController } from './app.controller';`r`n" + $content
    Write-Host '[ADD] AppController import' -ForegroundColor Green
}
else {
    Write-Host '[OK] AppController import already exists' -ForegroundColor DarkGray
}

# Register controller in @Module metadata.
if ($content -match 'controllers\s*:\s*\[') {
    if ($content -notmatch 'controllers\s*:\s*\[[^\]]*\bAppController\b') {
        $content = [regex]::Replace(
            $content,
            'controllers\s*:\s*\[',
            'controllers: [AppController, ',
            1
        )
        Write-Host '[ADD] AppController to existing controllers array' -ForegroundColor Green
    }
    else {
        Write-Host '[OK] AppController already registered' -ForegroundColor DarkGray
    }
}
elseif ($content -match '@Module\s*\(\s*\{') {
    $content = [regex]::Replace(
        $content,
        '@Module\s*\(\s*\{',
        "@Module({`r`n  controllers: [AppController],",
        1
    )
    Write-Host '[ADD] controllers array to AppModule' -ForegroundColor Green
}
else {
    throw 'Could not locate @Module({ ... }) in backend/src/app.module.ts'
}

Set-Content -Path $appModule -Value $content -Encoding UTF8

Write-Host ''
Write-Host 'Building backend...' -ForegroundColor Yellow

Set-Location $backend
npm run build

if ($LASTEXITCODE -ne 0) {
    throw 'Backend build failed after API base-route patch'
}

Write-Host ''
Write-Host 'BACKEND BUILD SUCCESSFUL' -ForegroundColor Green
Write-Host ''
Write-Host 'Next:'
Write-Host '  cd E:\UniversalLive'
Write-Host '  git add .'
Write-Host '  git commit -m "Add UniversalLive API base route"'
Write-Host '  git push origin main'
