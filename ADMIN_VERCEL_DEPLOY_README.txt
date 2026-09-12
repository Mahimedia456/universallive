UNIVERSAL LIVE — ADMIN VERCEL DEPLOYMENT

1) SQL
0631_admin_super_admin_seed_TEMPLATE.sql is intentionally a template and throws until placeholders are replaced.
Use backend/sql/final/0632_admin_super_admin_seed_READY.sql instead.
Run 0630 first, then 0632.

Ready login:
Email: admin@universallive.local
Password: ULive#OIcQR%eU3ZqRJL2JJV
Change password after first successful production login.
Use a real mailbox later if password-reset email is required.

2) ADMIN VERCEL PROJECT
Framework: Vite
Root Directory: apps/admin
Install: npm ci
Build: npm run build
Output: dist
Config: apps/admin/vercel.json

3) ADMIN ENV
VITE_API_BASE_URL=https://YOUR-BACKEND-DOMAIN.vercel.app/api/v1
Redeploy admin after changing VITE_* env values.

4) BACKEND CORS AFTER ADMIN URL EXISTS
Add admin URL to backend Vercel env, for example:
CORS_ORIGINS=https://universallive.vercel.app,https://universallive-admin.vercel.app,http://localhost:5174,http://127.0.0.1:5174
Then redeploy backend.

5) CLI DEPLOY
cd E:\UniversalLive\apps\admin
npx vercel login
npx vercel
npx vercel --prod

6) VERIFY
Open https://<ADMIN-URL>/login
Request must go to https://<BACKEND-URL>/api/v1/admin/auth/login
OPTIONS should succeed; POST should return 200 for valid credentials.
401 means auth rejection, not CORS.

DO NOT RUN backend/sql/0024_admin_console.sql.
