UNIVERSAL LIVE — ADMIN BATCH 05
DEPLOYMENT + SCHEMA HARDENING

THIS IS CUMULATIVE ON TOP OF ADMIN BATCH 04.

CURRENT ISSUES FIXED
====================

1. VERCEL 500 / FUNCTION_INVOCATION_FAILED

Likely runtime Nest DI failure fixed:
AdminConsoleService injects BackendSupabase, but AdminConsoleModule previously
did not provide BackendSupabase.

Now:
providers: [AdminConsoleService, BackendSupabase]

This is a runtime problem that TypeScript compilation alone does not detect.

2. TYPESCRIPT CORS BUILD FAILURE

Fixed:
Parameter 'origin' implicitly has an 'any' type.
Parameter 'callback' implicitly has an 'any' type.

Both backend/src/main.ts and backend/src/server.ts now have explicit types.

Also removed the unnecessary helmet import from this deployment entry patch
so no extra runtime dependency is introduced.

3. SQL 0027 FAILURE

WRONG TABLE:
public.ul_stream_telemetry_samples

ACTUAL TABLE:
public.ul_stream_telemetry

ACTUAL TIME COLUMN:
sampled_at

DO NOT RUN THE OLD 0027 FILE AGAIN.

RUN:
backend/sql/0027_admin_final_management_REPAIRED.sql

The repaired SQL is guarded with to_regclass and is idempotent.

4. SUPPORT MESSAGE SCHEMA

WRONG:
sender_user_id

ACTUAL:
user_id

Admin support replies now use the real schema.

5. NOTIFICATION SCHEMA

Actual ul_notifications columns include:
type
title
body
severity
read_at

Old admin code incorrectly used:
is_read

Fixed:
- admin broadcasts insert type='admin_broadcast'
- severity='info'
- read_at=null
- admin UI reads read_at

6. SYSTEM FLAGS SCHEMA

Aligned to:
key
value
description
is_public

7. VERCEL RUNTIME DIAGNOSTIC

Added:
GET /api/v1/admin-console/runtime-status

MERGE
=====
E:\UniversalLive

SQL
===
Supabase SQL Editor:
backend/sql/0027_admin_final_management_REPAIRED.sql

Do not rerun the broken old 0027 SQL.

BUILD
=====
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\admin-batch05-repair-build.ps1

PUSH
====
cd E:\UniversalLive
git add .
git commit -m "UniversalLive admin batch 05 deployment schema repair"
git push origin main

WAIT FOR VERCEL REDEPLOY.

TEST
====
cd E:\UniversalLive
.\tools\test-vercel-runtime-after-batch05.ps1

EXPECTED:
- /api/v1/health works
- /api/v1/admin-console/runtime-status works
- localhost:5173 CORS preflight succeeds

VERCEL ENV
==========
Recommended:
CORS_ORIGINS=http://localhost:5173,https://universallive.vercel.app

If testing only:
CORS_ORIGINS=*

ADMIN LOCAL
===========
cd E:\UniversalLive\apps\admin
npm run dev
