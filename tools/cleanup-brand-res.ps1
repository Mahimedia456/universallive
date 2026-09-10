$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$res = Join-Path $projectRoot "androidApp\src\main\res"

Write-Host "Cleaning invalid Android resource files from: $res" -ForegroundColor Cyan

$badDirs = @(
  (Join-Path $res "source"),
  (Join-Path $res "android")
)
foreach ($dir in $badDirs) {
  if (Test-Path $dir) {
    Remove-Item $dir -Recurse -Force
    Write-Host "Removed $dir"
  }
}

$badRootPatterns = @(
  "app-icon-*.png",
  "logo-dark.png",
  "logo-light.png",
  "splash-portrait.png",
  "README.txt"
)
foreach ($pattern in $badRootPatterns) {
  Get-ChildItem -Path $res -Filter $pattern -File -ErrorAction SilentlyContinue | ForEach-Object {
    Remove-Item $_.FullName -Force
    Write-Host "Removed $($_.FullName)"
  }
}

Write-Host "Android res cleanup complete." -ForegroundColor Green
Write-Host "Correct resources should only live in drawable-*/mipmap-*/values folders." -ForegroundColor DarkGray
