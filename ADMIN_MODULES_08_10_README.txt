UNIVERSAL LIVE ADMIN — MODULES 08, 09, 10
================================================

This package continues the locked Universal Live desktop admin design and is built on top of Modules 01-07.

MODULE 08 — STREAM HEALTH & DIAGNOSTICS
---------------------------------------
- Real telemetry overview; no fake health values.
- Active / healthy / warning / stale counts.
- Actual bitrate vs configured target bitrate.
- Encoded FPS.
- Dropped frames.
- Encoder resolution/name.
- Publish/network/audio/thermal/battery state.
- Per-stream detail.
- Per-destination status.
- Recent telemetry samples.
- Recent stream events.
- Health filtering and refresh.
- Health thresholds are admin diagnostics only and do not mutate the mobile publisher.

MODULE 09 — ANALYTICS
---------------------
- 7 / 30 / 90 day views.
- Stream count.
- Live minutes.
- Active creators.
- Average measured bitrate.
- Daily stream activity.
- Stream status distribution.
- Destination platform distribution.
- Membership distribution.
- Health snapshot.
- Top creators by streams/live minutes.
- Data is derived from existing backend/mobile tables.

MODULE 10 — MEMBERSHIP & PLANS
------------------------------
PLAN CATALOG
- List.
- Detail.
- Add.
- Edit.
- Activate/deactivate.
- Entitlements JSON editor.
- Sort order.

USER ENTITLEMENTS
- List/search.
- Detail.
- Edit plan/status/source/expiry.
- Entitlements override JSON.
- Uses existing ul_plans and ul_user_entitlements.

MOBILE/BACKEND SCHEMA SAFETY
----------------------------
No mobile database migration is included in this package.

Run:
backend/sql/admin/ADMIN_MODULES_08_10_VERIFY_EXISTING_SCHEMA.sql

It contains SELECT statements only and is used to verify that the existing tables/columns are available.
The package intentionally does NOT ALTER, DROP, CREATE, INSERT, UPDATE or DELETE schema/data via SQL.

Admin write actions happen only through protected NestJS admin endpoints and operate on the already-existing tables.

MERGE
-----
Extract into:
E:\UniversalLive

BACKEND
-------
cd E:\UniversalLive\backend
Remove-Item -Recurse -Force dist -ErrorAction SilentlyContinue
npm install
npm run build
npm run dev

ADMIN
-----
cd E:\UniversalLive\apps\admin
Remove-Item -Recurse -Force node_modules\.vite -ErrorAction SilentlyContinue
npm install
npm run build
npm run dev

VERIFY
------
1. Admin login works.
2. Stream Health opens without browser errors.
3. If telemetry exists, actual bitrate/FPS/drop values are visible.
4. Analytics switches between 7/30/90 days.
5. Membership lists existing plans.
6. Plan detail/edit works.
7. New plan can be added.
8. User entitlement detail/edit works.
9. Mobile application tables/schema were not changed.

IMPORTANT
---------
This batch does not claim that YouTube RTMP low-bitrate / No Data is fixed.
It exposes the real telemetry needed to diagnose that pipeline.
The actual Android encoder/RTMP publisher correction belongs to the dedicated mobile/backend streaming integration phase.
