Write-Host 'Generate these locally and paste them into Vercel Environment Variables.' -ForegroundColor Cyan
Write-Host ''
Write-Host ('STREAM_SECRET_MASTER_KEY=' + [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 })))
Write-Host ('ADMIN_API_KEY=' + [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 })))
