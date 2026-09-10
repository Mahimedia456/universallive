# UniversalLive Backend — Phases 21–23

NestJS + Supabase backend foundation.

## Security
- Mobile app gets only `SUPABASE_URL` + publishable key.
- `SUPABASE_SECRET_KEY` stays only in `backend/.env` on a trusted machine/server.
- Do not commit `.env`.

## 1. Configure
```powershell
cd E:\UniversalLive\backend
Copy-Item .env.example .env
notepad .env
```
Fill the Supabase URL, publishable key and server-only secret.

## 2. Run SQL
Open Supabase Dashboard -> SQL Editor and run:
`supabase/migrations/0001_universallive_core.sql`

## 3. Install and run
```powershell
npm install
npm run dev
```

Health:
`GET http://127.0.0.1:3000/api/v1/health`

## Auth model
The mobile app signs in with Supabase Auth and sends its access token to NestJS:
`Authorization: Bearer <access_token>`

Backend validates the token before all profile/stream endpoints.

## Stream endpoints
- POST `/api/v1/streams/start`
- PATCH `/api/v1/streams/:id/status`
- POST `/api/v1/streams/:id/metrics`
- POST `/api/v1/streams/:id/stop`
- GET `/api/v1/streams/active`
- GET `/api/v1/streams/history`

These APIs track the session; RTMP media still goes directly from the phone to YouTube/Facebook/TikTok/custom RTMP unless relay mode is added later.
