UNIVERSAL LIVE — ADMIN BUILD HOTFIX
=====================================

This patch addresses the build output supplied after Module 20.

ROOT CAUSES FIXED
-----------------
1. import.meta.env TS2339
   Added:
   apps/admin/src/vite-env.d.ts
   with Vite client ambient types.

2. 40+ missing consoleApi methods
   The merged local consoleApi.ts was still an older Module 05-07 version.
   It only exposed Dashboard / Users / Creators / Streams / Connections.
   This patch replaces it with the full cumulative API contract through Module 20:
   Diagnostics, Analytics, Membership, Billing, Notifications, Support,
   Moderation, System Health, Feature Flags, Admin Users, Audit, Settings, Final QA.

3. NotificationsPage implicit-any TS7006
   Catch parameter changed to unknown and narrowed safely.

4. Backend tsconfig editor warnings
   tools/fix-backend-tsconfig.ps1:
   - sets rootDir to ./src
   - adds ignoreDeprecations 6.0
   - preserves baseUrl so current absolute NestJS imports are not broken.

IMPORTANT
---------
The backend already built successfully in the supplied log.
The blocking failure was the admin TypeScript build.
This patch does not change mobile database tables.

MERGE
-----
Extract the ZIP into:
E:\UniversalLive

Then run:

cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
powershell.exe -NoProfile -ExecutionPolicy Bypass `
  -File "E:\UniversalLive\tools\admin-build-hotfix-verify.ps1"

EXPECTED
--------
PASS: backend tsconfig rootDir/TypeScript compatibility updated
PASS: vite/client typing present
PASS: cumulative consoleApi methods present
PASS: backend build
PASS: admin build
PASS: Universal Live Admin build hotfix verified

Do NOT run npm audit fix --force as part of this build fix. The backend vulnerability report is a separate dependency review because --force may introduce breaking dependency upgrades.
