# Backend Phase 27–29

1. Run `supabase/migrations/0003_secure_credentials_admin.sql` in Supabase SQL Editor.
2. Add `STREAM_SECRET_MASTER_KEY` and `ADMIN_API_KEY` to `backend/.env`.
3. Run `npm install` (if not already done), then `npm run build` and `npm run dev`.
4. Admin endpoints require header `x-admin-key: <ADMIN_API_KEY>`.
5. Saved stream credentials are not returned in plaintext by the API.
