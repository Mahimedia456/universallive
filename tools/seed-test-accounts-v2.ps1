$ErrorActionPreference = 'Stop'

Set-Location 'E:\UniversalLive'

Write-Host '=== Universal Live Supabase Admin Seed V2 ===' -ForegroundColor Cyan
Write-Host ''

node .\tools\seed-test-accounts-v2.mjs

if ($LASTEXITCODE -eq 2) {
    throw 'No valid Supabase secret/service_role key was found in backend .env'
}

if ($LASTEXITCODE -eq 3) {
    throw '@supabase/supabase-js is missing from backend node_modules'
}

if ($LASTEXITCODE -eq 4) {
    throw 'Configured Supabase admin key was rejected'
}

if ($LASTEXITCODE -ne 0) {
    throw 'Universal Live test-account seed failed'
}
