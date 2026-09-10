UNIVERSAL LIVE — SUPABASE SECRET KEY 401 REPAIR

ERROR FIXED:
401: This endpoint requires a valid Bearer token

CAUSE:
The project is using the newer Supabase `sb_secret_*` server API key.
That key is an API key, not a JWT.
It must not be sent as:
Authorization: Bearer sb_secret_...

FIX:
- `apikey: sb_secret_...` for new Supabase secret keys
- Bearer is used only when the configured key is the legacy JWT service_role key
- backend `BackendSupabase.adminRest()` repaired too
- seed script repaired

MERGE INTO:
E:\UniversalLive

NO SQL REQUIRED FOR THIS REPAIR.

RUN:

cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\supabase-secret-key-repair-build.ps1

THEN:

cd E:\UniversalLive
.\tools\seed-test-accounts.ps1

EXPECTED:
FREE     free.test@universallive.local
CREATOR  creator.test@universallive.local
PRO      pro.test@universallive.local

Default password:
UniversalLive@Test12345

Do not expose SUPABASE_SECRET_KEY or SUPABASE_SERVICE_ROLE_KEY in mobile code.
