UNIVERSAL LIVE ADMIN — HOTFIX MODULE 16 + MODULES 17, 18, 19
================================================================

IMPORTANT HOTFIX — MODULE 16
----------------------------
Your existing ul_system_flags table does NOT contain an "enabled" column.

Actual schema from the current Universal Live backend foundation:
- key
- value
- description
- is_public
- updated_at
- created_at

The previous verification SQL was wrong.

Fixed SQL:
backend/sql/admin/HOTFIX_MODULE_16_SYSTEM_FLAGS_EXISTING_SCHEMA.sql

The Feature Flags backend/UI is also fixed:
- removed enabled column usage
- uses is_public
- keeps feature/config state inside JSON value
- no mobile schema change

MODULE 17 — ADMIN USERS / ROLES / PERMISSIONS
---------------------------------------------
- List/search/filter.
- Detail.
- Add administrator.
- Edit administrator.
- Roles:
  SUPER_ADMIN
  ADMIN
  SUPPORT
  MODERATOR
  FINANCE
  VIEWER
- JSON permission array.
- Enable/disable.
- Lock/failed-login visibility.
- Secure bcrypt password reset.
- Password reset revokes active refresh tokens.
- Recent audit activity.
- Admin authentication remains separate from mobile users.

MODULE 18 — AUDIT LOGS
----------------------
- Uses ul_admin_audit_log from Module 01 custom admin auth.
- List/search/action filter.
- Admin identity resolution.
- Target/type/details inspection.
- Metrics for auth events, write actions, unique admins.
- Read-only in UI; no audit editing/deleting.

MODULE 19 — ADMIN SETTINGS
--------------------------
- My admin profile.
- Admin email/display name.
- Per-admin UI preferences.
- Table density.
- Sidebar preference.
- Timezone.
- Additional JSON preferences.
- SUPER_ADMIN global admin settings:
  - support SLA
  - session warning
  - page size
  - admin maintenance banner
- Global admin settings are separate from mobile ul_system_flags.

SQL ORDER
---------
1. Run:
backend/sql/admin/HOTFIX_MODULE_16_SYSTEM_FLAGS_EXISTING_SCHEMA.sql
This is SELECT-only.

2. Run:
backend/sql/admin/ADMIN_MODULES_17_19_ADMIN_SETTINGS.sql
This creates/ensures ONLY admin-owned UI/settings tables.
It does not alter mobile user/stream tables.

MERGE
-----
Extract ZIP into:
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

PASS
----
- Feature Flags loads without "enabled column" error.
- Existing backend_contract flag is visible.
- is_public can be edited.
- JSON flag values can be edited.
- SUPER_ADMIN can add/edit/disable admin accounts.
- Admin password reset revokes refresh tokens.
- Audit Logs display existing ul_admin_audit_log rows.
- Admin Settings profile/preferences save.
- Global settings save for SUPER_ADMIN.
- Existing mobile tables are unchanged.
