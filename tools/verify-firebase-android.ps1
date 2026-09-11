param(
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot)
)
$ErrorActionPreference = 'Stop'
$path = Join-Path $ProjectRoot 'androidApp\google-services.json'
if (-not (Test-Path $path)) { throw "Missing $path" }
$j = Get-Content $path -Raw | ConvertFrom-Json
$client = @($j.client | Where-Object { $_.client_info.android_client_info.package_name -eq 'com.universallive.app' }) | Select-Object -First 1
if ($null -eq $client) { throw 'google-services.json does not contain com.universallive.app' }
if ([string]$j.project_info.project_id -ne 'universallive-d5d90') { throw 'Unexpected Firebase project_id' }
if ([string]$j.project_info.project_number -ne '322163676607') { throw 'Unexpected Firebase sender/project number' }
if ([string]$client.client_info.mobilesdk_app_id -ne '1:322163676607:android:cdb05b171ef7fc3c27bd0a') { throw 'Unexpected Firebase Android App ID' }
Write-Host 'PASS: Firebase project universallive-d5d90' -ForegroundColor Green
Write-Host 'PASS: package com.universallive.app' -ForegroundColor Green
Write-Host 'PASS: sender/project number 322163676607' -ForegroundColor Green
Write-Host 'PASS: Android App ID matches registered Firebase app' -ForegroundColor Green
