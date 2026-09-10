# UniversalLive Backend Batch 02 — Phase 02, 03, 04

## Phase 02 — Authentication & Sessions
- Supabase Auth stays the identity provider.
- Email/password register.
- Email/password login.
- Email OTP verification.
- Resend verification.
- Forgot password initiation.
- Refresh token endpoint.
- Logout.
- Additive `ul_user_sessions` table for later device/session tracking.

## Phase 03 — Creator Profile
- `ul_creator_profiles`
- username uniqueness
- display name
- creator type
- avatar URL
- onboarding-completed flag
- locale/timezone foundation
- GET/PUT `/api/v1/profiles/me`

## Phase 04 — Device + Onboarding State
- `ul_devices`
- push token slot
- device/app metadata
- `ul_onboarding_state`
- permission education acknowledgement state
- creator setup completion
- first destination prompt completion

Native microphone/camera/screen permissions remain platform-native and are not treated as server-authoritative permissions.

## SQL order
Run:
1. `0005_auth_sessions.sql`
2. `0006_creator_profiles.sql`
3. `0007_device_onboarding.sql`

## Backend routes
- `GET  /api/v1/auth/mobile/status`
- `POST /api/v1/auth/mobile/register`
- `POST /api/v1/auth/mobile/login`
- `POST /api/v1/auth/mobile/refresh`
- `POST /api/v1/auth/mobile/verify-email`
- `POST /api/v1/auth/mobile/resend-verification`
- `POST /api/v1/auth/mobile/forgot-password`
- `POST /api/v1/auth/mobile/logout`
- `GET  /api/v1/profiles/me`
- `PUT  /api/v1/profiles/me`
- `POST /api/v1/devices/register`
- `GET  /api/v1/onboarding/me`
- `PUT  /api/v1/onboarding/me`

## Mobile
Shared Kotlin serialization models are included for auth/profile/onboarding contracts.
Actual screen-event wiring should be done after backend build/API verification against the user's existing HTTP client/session store, because the current project already contains mobile networking/session infrastructure and it should not be duplicated blindly.
