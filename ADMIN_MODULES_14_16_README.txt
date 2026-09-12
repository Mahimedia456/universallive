UNIVERSAL LIVE ADMIN — MODULES 14, 15, 16
================================================

14 MODERATION / ABUSE
- List, search, status/severity filters.
- Open case.
- Detail.
- Edit status/severity/resolution/internal notes.
- Target user, stream, or connection.
- Explicit actions: disable/re-enable user, disable connection, end stream.
- Audit log for create/update/action.
- One NEW ADMIN-ONLY table: ul_moderation_cases.
- Existing mobile tables are not altered.

15 SYSTEM HEALTH
- Overall state.
- Backend/database check.
- SMTP configuration presence.
- Push provider configuration presence.
- Recent stream telemetry.
- Active broadcasts.
- Open support tickets.
- Failed push deliveries.
- Locked admins.
- Enabled connections.
- Recent stream errors.
- Backend version history.
- Read-only admin UI, 30-second refresh.

16 FEATURE FLAGS
- Existing ul_system_flags reused.
- List/detail-style cards.
- Add/edit/delete.
- Enabled state.
- JSON value.
- Description.
- Audit trail on every write.
- Do NOT put secrets in flags.

SQL
---
Run:
backend/sql/admin/ADMIN_MODULE_14_MODERATION.sql

This creates ONLY the admin-owned moderation case table.

Optional read-only verification:
backend/sql/admin/ADMIN_MODULES_15_16_VERIFY_EXISTING_SCHEMA.sql

MERGE
-----
Extract into E:\UniversalLive

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

PASS
----
- Moderation case can be created/opened/edited.
- Target actions only apply to matching target type.
- System Health loads actual database/runtime counters.
- Feature Flags loads existing ul_system_flags.
- Flag add/edit/delete works and is audited.
- No existing mobile schema is altered.
