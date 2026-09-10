UniversalLive Phase 30.1 - Backend contamination cleanup

Why this patch exists:
Older SizeME files remained in E:\UniversalLive\backend\src because ZIP extraction merges files but does not delete stale directories. TypeScript compiles all src/**/*.ts files, so those old files caused errors for bcryptjs/jsonwebtoken/nodemailer/swagger and SizeME modules.

Removed by cleanup script:
- backend/src/measurements/
- backend/src/recommendations/
- backend/src/scans/
- backend/src/size-charts/
- backend/src/profile/       (UniversalLive uses profiles/)
- backend/src/email/
- backend/src/auth/auth.service.ts
- stale current-profile decorator

UniversalLive backend keeps only its own modules: auth, health, profiles, streams, destinations, settings, scenes, credentials, account, admin, supabase.

After merging this ZIP into E:\UniversalLive run:
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\phase30_1-clean-build-run.ps1

If successful, then:
.\tools\phase30-run-all.ps1
