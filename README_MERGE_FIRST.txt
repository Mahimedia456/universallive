UNIVERSAL LIVE — ADMIN BATCH 02

THIS ZIP IS SEPARATE FROM THE MOBILE FIX.

REQUIRES:
Admin Batch 01 already merged.

MERGE INTO:
E:\UniversalLive

1. RUN SQL:
backend/sql/0025_admin_management.sql

2. BUILD:
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\admin-batch02-apply.ps1

3. PUSH BACKEND + ADMIN SOURCE:
cd E:\UniversalLive
git add .
git commit -m "UniversalLive admin batch 02 management"
git push origin main

4. LOCAL ADMIN:
cd E:\UniversalLive\apps\admin
npm run dev

FEATURES:
- Creators list
- Free / Creator / Pro membership change
- Connections list
- enable/disable saved connections
- Broadcasts list
- stop active broadcast from control plane
- Support tickets list
- update support status
- role enforcement:
  owner/admin => membership, connections, broadcasts
  support => support workflow
- admin action audit table
- no service-role key exposed to browser

NOTE:
Admin stop updates the backend broadcast session/control-plane state.
It cannot magically terminate a third-party platform stream if the mobile
publisher is disconnected from the backend; device-side STOP remains the
authoritative media stop operation.
