UNIVERSAL LIVE ADMIN — MODULES 02, 03, 04

MODULE 02 — Enterprise Shell
- Desktop dark/cyan Universal Live shell
- Sidebar + topbar + global search shell
- Role identity + logout
- Reserved navigation for future admin modules
- No fake page data on unimplemented destinations

MODULE 03 — Dashboard
- Real backend counts from Supabase via NestJS service role
- Users, active users, verified users, profiles, connections, broadcasts, live broadcasts, open support
- Recent users and recent broadcasts
- Operational status presentation

MODULE 04 — Users
- List + pagination + search + active/inactive + verified/pending filters
- Add user
- User detail
- Edit account/profile/membership
- Activate/deactivate account
- Mark email verified
- Password reset by admin through edit form
- Membership FREE / CREATOR / PRO
- Connections and recent stream records on detail
- Optional SUPER_ADMIN hard-delete endpoint; UI intentionally defaults to safe deactivation
- Audit log writes for create/update/status/verification/delete

MERGE TARGET
Extract/merge into E:\UniversalLive AFTER Admin Module 01.

SQL
Run backend/sql/admin/ADMIN_MODULES_02_04_SHELL_DASHBOARD_USERS.sql in Supabase SQL Editor.
It preserves existing rows and adds compatibility fields/tables where missing.

BACKEND REQUIRED ENV
SUPABASE_URL
SUPABASE_SERVICE_ROLE_KEY
ADMIN_JWT_SECRET (32+ chars)

BACKEND CHECK
cd E:\UniversalLive\backend
npm install
npm run build
npm run dev

ADMIN CHECK
cd E:\UniversalLive\apps\admin
npm install
npm run dev

EXPECTED ENDPOINTS
GET    /api/v1/admin/dashboard
GET    /api/v1/admin/users
GET    /api/v1/admin/users/:id
POST   /api/v1/admin/users
PATCH  /api/v1/admin/users/:id
POST   /api/v1/admin/users/:id/status
POST   /api/v1/admin/users/:id/verify-email
DELETE /api/v1/admin/users/:id

IMPORTANT
- Admin browser never uses Supabase Auth directly.
- User passwords are bcrypt hashes only.
- This batch assumes Admin Module 01 custom admin auth is already merged.
- Current mobile/backend database contains historical auth.users-linked tables. This batch reads those operational tables but makes public.users the canonical user-management target for the custom-auth migration direction.
