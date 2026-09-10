UNIVERSAL LIVE — GITHUB + VERCEL BACKEND PREP

MERGE INTO:
E:\UniversalLive

FILES:
.gitignore
backend/vercel.json
backend/src/server.ts
backend/.env.vercel.example
tools/git-first-push.ps1
tools/generate-production-secrets.ps1

FIRST PUSH:
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\git-first-push.ps1

REPOSITORY:
https://github.com/Mahimedia456/universallive

VERCEL:
Import repository
Root Directory = backend
Node.js = 24.x
Framework = Other / No Framework

ENV:
Use backend/.env.vercel.example as the checklist.
Never commit backend/.env.

After deployment, verify:
https://YOUR-DOMAIN.vercel.app/api/v1/health
https://YOUR-DOMAIN.vercel.app/api/v1/foundation

Then send the final Vercel domain.
The next patch will wire the mobile app to that API base URL.
