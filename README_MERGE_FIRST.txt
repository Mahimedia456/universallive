UNIVERSAL LIVE — MOBILE INTEGRATION BATCH 01
PHASE 1 + PHASE 2

MERGE INTO:
E:\UniversalLive

IMPORTANT:
This is the first batch that replaces mock authentication/profile membership
with actual calls to the deployed NestJS backend.

API:
https://universallive.vercel.app/api/v1

STEP 1 — MERGE ZIP

STEP 2 — BUILD BACKEND + MOBILE:
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\mobile-integration-batch01-build.ps1

STEP 3 — PUSH BACKEND RECOVERY ENDPOINTS:
cd E:\UniversalLive
git add .
git commit -m "Mobile integration batch 01 auth profile membership"
git push origin main

Wait for Vercel deployment.

STEP 4 — TEST PUBLIC CONTRACT:
cd E:\UniversalLive
.\tools\test-mobile-auth-api.ps1

STEP 5 — BUILD FRESH APK AFTER VERCEL DEPLOY:
cd E:\UniversalLive
.\gradlew.bat :androidApp:assembleDebug

APK:
E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk

TEST:
- sign in with FREE account -> Profile badge must show FREE
- sign out
- sign in with CREATOR account -> Profile badge must show CREATOR
- sign out
- sign in with PRO account -> Profile badge must show PRO
- kill/reopen app -> session should restore
- Profile name/username should come from backend

SECURITY:
No SUPABASE_SECRET_KEY is compiled into mobile.
Mobile only knows the public NestJS API URL.
