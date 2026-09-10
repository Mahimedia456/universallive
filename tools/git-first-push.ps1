$ErrorActionPreference = 'Stop'

Set-Location 'E:\UniversalLive'

Write-Host '=== UniversalLive First GitHub Push ===' -ForegroundColor Cyan

if (-not (Test-Path '.git')) {
    git init
}

git branch -M main

$originExists = $false

$remotes = git remote

if ($remotes -contains 'origin') {
    $originExists = $true
}

if (-not $originExists) {
    git remote add origin https://github.com/Mahimedia456/universallive.git
    Write-Host '[ADD] origin remote' -ForegroundColor Green
}
else {
    $currentRemote = git remote get-url origin

    if ($currentRemote -ne 'https://github.com/Mahimedia456/universallive.git') {
        git remote set-url origin https://github.com/Mahimedia456/universallive.git
        Write-Host '[UPDATE] origin remote' -ForegroundColor Green
    }
    else {
        Write-Host '[OK] origin already correct' -ForegroundColor DarkGray
    }
}

git add .

git status --short

$status = git status --porcelain

if ($status) {
    git commit -m "UniversalLive initial mobile backend deployment"
}
else {
    Write-Host 'No new changes to commit.' -ForegroundColor DarkGray
}

Write-Host ''
Write-Host 'Pushing to GitHub...' -ForegroundColor Yellow

git push -u origin main

if ($LASTEXITCODE -ne 0) {
    throw 'GitHub push failed. Check GitHub authentication and repository permissions.'
}

Write-Host ''
Write-Host 'GITHUB PUSH SUCCESSFUL' -ForegroundColor Green