UNIVERSAL LIVE — BACKEND UPGRADE PHASE 01
FOUNDATION + API CONTRACT

This package is intentionally NON-DESTRUCTIVE.

It does not replace the current Universal Live backend.
It adds:
- versioned foundation API
- standard success response contract
- backend/mobile contract metadata
- system flags table
- backend version table
- API audit-event table foundation
- RLS for public system flags
- build/apply and verify scripts

1) MERGE ZIP INTO:
E:\UniversalLive

2) RUN SQL IN:
Supabase -> SQL Editor

File:
E:\UniversalLive\backend\sql\0004_backend_foundation.sql

3) APPLY BACKEND MODULE:
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\backend-phase01-apply.ps1

4) START BACKEND:
cd E:\UniversalLive\backend
npm run dev

5) VERIFY:
cd E:\UniversalLive
.\tools\backend-phase01-verify.ps1

Expected URLs:
http://127.0.0.1:3000/api/v1/health
http://127.0.0.1:3000/api/v1/foundation
http://127.0.0.1:3000/api/v1/foundation/readiness

MOBILE:
Phase 01 establishes the backend contract used by upcoming mobile integrations.
Actual auth screen integration begins in Backend Phase 02.
