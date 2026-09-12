UNIVERSAL LIVE — PREFLIGHT ROUTE HOTFIX
=======================================

ROOT CAUSE
----------
The backend source already contains StreamLifecycleController and the routes:

  PUT  /api/v1/streams/draft/current
  POST /api/v1/streams/preflight
  GET  /api/v1/streams/preflight/:id
  POST /api/v1/streams/sessions/:id/publisher-state
  POST /api/v1/streams/sessions/:id/recover
  GET  /api/v1/streams/sessions/:id/diagnostics

But StreamLifecycleModule was NOT imported into backend/src/app.module.ts.
Therefore Nest never mounted these routes, even after deploying the latest code.
The Android client's 404 mapper correctly translated that missing route into:

  "This app is connected to an older backend deployment."

FIX
---
This patch imports StreamLifecycleModule into AppModule.
No SQL migration is required.
No mobile table/schema is changed.
No admin schema is changed.

MERGE TARGET
------------
E:\UniversalLive

LOCAL BACKEND BUILD
-------------------
cd E:\UniversalLive\backend
npm run build

Expected:
PASS / successful Nest build

DEPLOY
------
Commit/push this patch and redeploy the BACKEND Vercel project.
If Vercel auto-deploys main, pushing to main is enough.

PRODUCTION ROUTE VERIFY
-----------------------
Run:

cd E:\UniversalLive
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\verify-preflight-route.ps1" `
  -BaseUrl "https://YOUR-BACKEND.vercel.app/api/v1"

Expected for an unauthenticated probe:
  HTTP 401 / 403 = PASS (route exists, auth rejected probe)

Bad result:
  HTTP 404 = FAIL (new backend deployment still does not contain/mount route)

APP TEST
--------
After backend deployment:
1. Reopen/restart Universal Live.
2. Select a valid enabled destination with RTMP credentials.
3. Run Stream Preflight -> Test Again.
4. The "older backend deployment" message must no longer appear.

If preflight still fails after this route is mounted, the UI should now show the
real backend validation reason (destination disabled, missing RTMP credentials,
expired credentials, plan limit, etc.) instead of the false old-backend message.
