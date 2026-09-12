UNIVERSAL LIVE ADMIN — MODULES 05, 06, 07 + HOTFIX
===================================================

HOTFIXES INCLUDED
-----------------
1) Vite/App collision fixed permanently:
   - src/main.tsx now imports src/AdminApp.tsx.
   - Old/stale App.jsx can remain; Vite no longer resolves it.
   - This fixes: No matching export in src/App.jsx for import "App".

2) Backend nodemailer runtime dependency added:
   - backend/package.json now includes nodemailer.
   - Run npm install in backend after merge.
   - NO mobile tables are changed by this fix.

MODULE 05 — CREATORS
--------------------
- List/search creator profiles
- Real email lookup from current Supabase auth admin API
- Creator detail
- Add profile for an existing user UUID
- Edit profile/onboarding/type/locale/timezone
- Connections / streams / scenes operational summary
- No new creator/mobile table columns

MODULE 06 — LIVE STREAMS
------------------------
- List/filter broadcast sessions
- Real latest telemetry: bitrate, target bitrate, FPS, dropped frames
- Stream detail with destinations, telemetry, events, summary
- Create admin draft only (never fakes LIVE)
- Edit safe record fields
- End active session from admin with audit/event trail
- Existing mobile stream tables only; no schema mutation

MODULE 07 — CONNECTIONS
-----------------------
- List/search/filter destinations
- Connection detail
- Add/edit metadata
- Enable/disable/default destination
- Last success/error visibility
- Secure credential vault PRESENCE metadata only
- Secret stream keys/OAuth tokens are NEVER returned to browser
- Existing connection/vault tables only; no schema mutation

SQL
---
backend/sql/admin/ADMIN_MODULES_05_07_VERIFY_EXISTING_MOBILE_SCHEMA.sql
is READ-ONLY. It performs SELECTs only. It does not CREATE/ALTER/DROP/UPDATE any mobile table.

AFTER EXTRACT/MERGE
-------------------
Backend:
  cd E:\UniversalLive\backend
  npm install
  npm run build
  npm run dev

Admin:
  cd E:\UniversalLive\apps\admin
  Remove-Item -Recurse -Force node_modules\.vite -ErrorAction SilentlyContinue
  npm install
  npm run build
  npm run dev

Expected fixes:
- Vite no longer scans App.jsx through main.tsx.
- nodemailer resolves at backend runtime.
- No mobile API schema/table is changed by Modules 05-07.
