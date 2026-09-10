# UniversalLive Phase 30 — Final Integration + Brand v2

This cumulative checkpoint contains the Android broadcaster through Phase 20.3 and backend/cloud work through Phase 29, plus the final Phase 30 integration/run tooling and a completely new UniversalLive brand identity.

## Brand v2
- New black-first streaming identity (not derived from the previous supplied mark)
- Cyan/blue broadcast symbol with live accent
- launcher + round launcher icon densities
- Android splash artwork
- master wordmark and landscape brand artwork
- source/master assets in `brand-assets-v2/`

## Android runtime
Package: `com.universallive.app`
Version: `0.30.0`

## Backend
Migrations, in order:
1. `0001_universallive_core.sql`
2. `0002_cloud_sync.sql`
3. `0003_secure_credentials_admin.sql`

Keep `SUPABASE_SECRET_KEY`, `STREAM_SECRET_MASTER_KEY`, and `ADMIN_API_KEY` only in `backend/.env`.

## One-command local run
From the project root:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\phase30-run-all.ps1
```

The script validates/builds the backend, waits for `/api/v1/health`, builds/installs the Android debug APK over the currently connected ADB device, and launches UniversalLive.
