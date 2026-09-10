$ErrorActionPreference = 'Stop'

$base = 'http://127.0.0.1:3000/api/v1'

Write-Host 'UniversalLive Backend Batch 04 verification' -ForegroundColor Cyan

Invoke-RestMethod "$base/foundation" | ConvertTo-Json -Depth 6
Write-Host '[OK] foundation' -ForegroundColor Green

Write-Host ''
Write-Host 'Authenticated Studio endpoints:' -ForegroundColor Yellow
Write-Host "GET    $base/studio/scenes"
Write-Host "GET    $base/studio/scenes/{id}"
Write-Host "POST   $base/studio/scenes"
Write-Host "PATCH  $base/studio/scenes/{id}"
Write-Host "POST   $base/studio/scenes/{id}/duplicate"
Write-Host "DELETE $base/studio/scenes/{id}"
Write-Host "GET    $base/studio/scenes/{sceneId}/sources"
Write-Host "POST   $base/studio/scenes/{sceneId}/sources"
Write-Host "PATCH  $base/studio/sources/{id}"
Write-Host "POST   $base/studio/scenes/{sceneId}/sources/reorder"
Write-Host "DELETE $base/studio/sources/{id}"
Write-Host "GET    $base/studio/assets"
Write-Host "POST   $base/studio/assets"
Write-Host "DELETE $base/studio/assets/{id}"
Write-Host "GET    $base/studio/presets"
Write-Host "POST   $base/studio/presets/{presetId}/create-scene"
