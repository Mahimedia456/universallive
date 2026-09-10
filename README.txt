UNIVERSAL LIVE — SUPABASE ADMIN SEED REPAIR V2

The previous console message:
[AUTH] Admin key mode: unknown secure key format

means the backend .env did not contain a recognized Supabase admin credential
in the variables the previous script checked.

This V2:
- checks multiple common server-key variable names
- validates key TYPE before making admin requests
- rejects anon/publishable keys
- supports:
  * sb_secret_... Supabase Secret Key
  * legacy service_role JWT
- uses official @supabase/supabase-js Auth Admin API
- never prints the full key
- seeds FREE / CREATOR / PRO accounts idempotently

MERGE INTO:
E:\UniversalLive

RUN:
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\seed-test-accounts-v2.ps1

IF IT SAYS NO VALID ADMIN KEY:
Supabase Dashboard
-> Project Settings
-> API Keys

Copy either:
Secret key: sb_secret_...
OR Legacy service_role key

Put in:
E:\UniversalLive\backend\.env

Recommended:
SUPABASE_SECRET_KEY=sb_secret_...

Alternative:
SUPABASE_SERVICE_ROLE_KEY=<legacy service_role JWT>

IMPORTANT:
SUPABASE_PUBLISHABLE_KEY and SUPABASE_ANON_KEY are NOT admin keys.
The key must belong to the SAME project referenced by SUPABASE_URL.

No SQL is required for this repair.
