UNIVERSAL LIVE — API BASE ROUTE PATCH

PURPOSE
-------
Existing deployed endpoints work:
- /api/v1/health
- /api/v1/foundation
- /api/v1/billing/plans
- /api/v1/security/status

But:
- /api/v1

returns 404 because no root controller is registered.

THIS PATCH
----------
Adds:
backend/src/app.controller.ts

Registers:
AppController inside backend/src/app.module.ts

Because server.ts already has:
app.setGlobalPrefix('api/v1');

the controller's @Get() route becomes:
https://universallive.vercel.app/api/v1

MERGE INTO
----------
E:\UniversalLive

RUN
---
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\apply-api-base-route-patch.ps1

THEN PUSH
---------
cd E:\UniversalLive
git add .
git commit -m "Add UniversalLive API base route"
git push origin main

AFTER VERCEL REDEPLOY
---------------------
Open:
https://universallive.vercel.app/api/v1

Expected:
{
  "ok": true,
  "service": "UniversalLive Backend",
  "name": "Universal Live API",
  "status": "online",
  "apiVersion": "v1",
  ...
}

NOTE
----
The site root:
https://universallive.vercel.app/

may still return 404. That is fine for an API-only backend.

The mobile API base URL should be:
https://universallive.vercel.app/api/v1
