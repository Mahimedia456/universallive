UNIVERSAL LIVE ADMIN - MODULE 01 AUTHENTICATION

MERGE TARGET: E:\UniversalLive

1) Extract this ZIP into E:\UniversalLive and allow merge/overwrite.
2) Supabase SQL Editor: run backend/sql/admin/ADMIN_MODULE_01_AUTH.sql
3) Backend .env add a long random secret (32+ chars):
   ADMIN_JWT_SECRET=<your-long-random-secret>
4) Backend dependencies/build:
   cd E:\UniversalLive\backend
   npm install
   npm run build
   npm run dev
5) Admin frontend:
   cd E:\UniversalLive\apps\admin
   copy .env.example .env
   npm install
   npm run dev
6) Open http://localhost:5174
7) Development login seeded by SQL:
   admin@universallive.local
   1231231234
   CHANGE BEFORE PRODUCTION.

BACKEND ENDPOINTS:
POST /api/v1/admin/auth/login
POST /api/v1/admin/auth/refresh
GET  /api/v1/admin/auth/me
POST /api/v1/admin/auth/logout
POST /api/v1/admin/auth/forgot-password
POST /api/v1/admin/auth/verify-reset-otp
POST /api/v1/admin/auth/reset-password

IMPORTANT EMAIL NOTE:
Module 01 DB/auth/session flow is real. Forgot-password OTP is currently generated and securely hashed in DB, but delivery is DEV console output (`[DEV ADMIN OTP]`). SMTP/email delivery will be wired with the custom Universal Live mailer when the shared email backend phase is implemented; it does NOT use Supabase Auth.

PASS:
- SQL creates 4 service-only admin auth tables.
- Backend build succeeds.
- Login returns access + rotating refresh token.
- /me succeeds with Bearer access token.
- Refresh revokes old refresh token and issues a new one.
- 5 failed passwords temporarily lock account.
- Forgot creates hashed OTP; verify/reset works using DEV console OTP.
- Reset revokes prior refresh sessions.
- Frontend restores session and shows secure-access screen.
