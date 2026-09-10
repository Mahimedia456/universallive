UNIVERSAL LIVE — ADMIN BATCH 03
PLANS + NOTIFICATIONS + AUDIT + SYSTEM

REQUIRES:
Admin Batch 01 + 02 already merged.

MERGE INTO:
E:\UniversalLive

1. RUN SQL:
backend/sql/0026_admin_operations.sql

2. BUILD:
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\admin-batch03-apply.ps1

3. PUSH:
cd E:\UniversalLive
git add .
git commit -m "UniversalLive admin batch 03 operations"
git push origin main

4. LOCAL ADMIN:
cd E:\UniversalLive\apps\admin
npm run dev

NEW ADMIN FEATURES:
- Plans & Entitlements editor
- enable/disable plans
- max simultaneous destinations
- max resolution
- advanced scenes/overlays/analytics switches
- notifications broadcast to:
  all / free / creator / pro
- recent notification feed
- admin audit log
- system health dashboard
- system flags read surface
- role-aware backend protection

IMPORTANT:
Notification broadcast writes UniversalLive in-app notification records.
Actual APNs/FCM push delivery still requires push-provider credentials and device token delivery integration.

SYSTEM FLAGS:
This batch exposes read-only UI for flags.
The backend has an owner-only update endpoint prepared, but no generic raw JSON editor is exposed in the UI to avoid accidental production configuration corruption.
