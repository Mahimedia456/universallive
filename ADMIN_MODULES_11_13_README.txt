UNIVERSAL LIVE ADMIN — MODULES 11, 12, 13
================================================

MODULE 11 — BILLING / STORE PURCHASES
- List and filters.
- Purchase detail.
- Store / product / transaction data.
- Verification state.
- Linked membership state.
- Controlled admin correction of status, plan, expiry and auto-renew.
- NO manual creation of fake App Store / Google Play purchase transactions.
- Every correction is written to admin audit log.

MODULE 12 — NOTIFICATIONS
- List.
- Severity filters.
- Detail.
- Single-user send.
- Broadcast to all current users.
- Optional device push queue rows.
- Push delivery detail.
- Delete notification.
- Broadcast safety cap: 5000 users per direct admin request.
- Real high-scale campaigns should later use the queue/worker module.

MODULE 13 — SUPPORT
- Ticket list / filters / search.
- Ticket detail.
- Create ticket on behalf of an existing user.
- Priority management.
- Status lifecycle management.
- Diagnostics JSON.
- Full chronological message thread.
- Admin reply.
- Admin reply moves ticket to waiting_user.
- Audit records for create/edit/reply.

DATABASE SAFETY
---------------
This batch DOES NOT change current mobile API tables or schemas.

Run only if you want to verify existing structures:
backend/sql/admin/ADMIN_MODULES_11_13_VERIFY_EXISTING_SCHEMA.sql

It is SELECT-only.

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

PASS CHECK
----------
1. Billing page loads existing ul_store_purchases.
2. Purchase detail opens and an audited correction can be saved.
3. Notifications list opens.
4. Single-user notification creates a real ul_notifications row.
5. If push=true and the user has registered ul_devices with push_token, ul_push_deliveries rows are queued.
6. Support list/detail opens.
7. Admin can create a user ticket, update priority/status, and reply.
8. The same ticket/reply remains visible to the mobile support flow.
9. No mobile table migration was run.
