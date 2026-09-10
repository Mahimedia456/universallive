$ErrorActionPreference = 'Stop'
Set-Location 'E:\UniversalLive'
node .\tools\seed-admin-account.mjs
if ($LASTEXITCODE -ne 0) { throw 'Admin seed failed' }
