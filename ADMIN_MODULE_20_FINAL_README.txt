UNIVERSAL LIVE ADMIN — MODULE 20 FINAL INTEGRATION + QA
======================================================

THIS IS THE FINAL ADMIN ROADMAP BATCH.

WHAT MODULE 20 ADDS
-------------------
1. Final QA / Admin Readiness screen.
2. Live backend QA endpoint.
3. Checks for:
   - admin auth
   - admin tables
   - creator tables
   - broadcast/telemetry tables
   - plans/entitlements
   - notifications
   - support
   - moderation
   - audit
   - admin settings
   - system flags
   - SMTP configuration presence
   - push configuration presence
   - recent telemetry
   - mobile schema isolation
4. PASS / WARN / FAIL release gate UI.
5. Shared role-permission map for future permission hardening.
6. Final responsive polish.
7. Final PowerShell build verifier.
8. Read-only final SQL verifier.

MODULE 16 COMPATIBILITY
-----------------------
This final batch preserves the corrected ul_system_flags schema:
key
value
description
is_public
updated_at
created_at

There is NO required enabled column.

DATABASE
--------
No new SQL migration is required for Module 20.

Optional read-only verification:
backend/sql/admin/ADMIN_MODULE_20_FINAL_VERIFY.sql

FINAL BUILD VERIFY
------------------
From E:\UniversalLive:

Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "E:\UniversalLive\tools\admin-final-verify.ps1"

EXPECTED:
PASS backend build
PASS admin build
PASS stale App.jsx protection
PASS feature flag compatibility

LIVE ADMIN QA
-------------
1. Start backend.
2. Start admin.
3. Login as SUPER_ADMIN.
4. Open Final QA in sidebar.
5. Re-run checks.

A warning for recent telemetry is normal when nobody is currently streaming.
A warning for SMTP or Push means the provider configuration is not detected.
A FAIL indicates a required table/contract is missing and should be fixed before release.

MOBILE SAFETY
-------------
Module 20 does not ALTER/DROP/create mobile user/stream API tables.
It only reads existing contracts and uses admin-owned tables already introduced in earlier admin modules.
