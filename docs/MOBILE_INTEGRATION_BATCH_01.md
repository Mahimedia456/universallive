# UniversalLive Mobile Integration Batch 01

## Phase 1 — API Core + Auth
- Production API base URL:
  `https://universallive.vercel.app/api/v1`
- Android HTTP client
- persistent access + refresh tokens via private SharedPreferences
- restore/refresh session on launch
- real login
- real registration
- email verification
- resend verification
- forgot password
- recovery-code verification
- password update
- logout
- API errors shown in auth UI
- existing Android streaming engine untouched

## Phase 2 — Profile + Membership
- real `/profiles/me`
- real `/billing/entitlements/me`
- real `/onboarding/me`
- dynamic Free / Creator / Pro badge
- dynamic plan resolution and simultaneous destination limits
- dynamic creator name/username/email
- profile update
- account sign-out
- seeded test accounts now identify their real membership

## API configuration
`gradle.properties`:
`UL_API_BASE_URL=https://universallive.vercel.app/api/v1`

The value is compiled into Android BuildConfig.
There are no Supabase secret keys in the APK.

## Backend addition
Adds missing recovery endpoints:
- POST `/auth/mobile/verify-recovery`
- POST `/auth/mobile/update-password`

Push backend files before testing recovery flow on Vercel.

## Test accounts
FREE:
free.test@universallive.local

CREATOR:
creator.test@universallive.local

PRO:
pro.test@universallive.local

Use the password established by the test-account seed.

## Not in Batch 01
Connections are Batch 02.
Scenes/cloud sync are Batch 02.
Live session + telemetry/history are Batch 03.
