# SizeME Phase 22.2 — Custom Database Auth + Email OTP

This checkpoint replaces Supabase Auth in the mobile/backend auth flow.

## Database

New tables:

- `public.users`
- `public.auth_otp_codes`
- `public.auth_refresh_tokens`
- `public.auth_audit_log`

`public.profiles.user_id` links a SizeME account to its sizing profile. The old `auth_user_id` column is left nullable for migration compatibility but is no longer used by the backend.

## Signup

`POST /api/v1/auth/register` → database user → branded 6-digit email OTP → `POST /api/v1/auth/verify-email` → access + refresh tokens.

## Login/session

Passwords are bcrypt hashed. Access JWTs are short-lived. Refresh tokens are opaque random values, stored hashed in the database, and rotated every refresh.

## Password reset

Forgot-password sends a separate OTP. OTP verification returns a short-lived reset token. Password reset revokes existing refresh tokens.

## Email

Templates are in `backend/src/email/email.templates.ts`. SMTP is provider-agnostic and configured with `SMTP_*` environment variables.

## Mobile

Supabase Auth client code is no longer used. `expo-secure-store` holds the SizeME access/refresh tokens on native devices.
