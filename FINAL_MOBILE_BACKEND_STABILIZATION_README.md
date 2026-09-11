# Universal Live — Final Mobile + Backend Stabilization

This checkpoint is based on the supplied `UniversalLive(4).zip`. The existing admin workstream is intentionally untouched/locked. Mobile visual direction remains the approved black/cyan Universal Live design.

## Fixed in this checkpoint

1. **False Live screen on app open**
   - Mobile startup now opens Live only when the native publisher is actually active or the backend session is already `live` / `reconnecting`.
   - `created`, `starting`, and `connecting` backend sessions no longer hijack startup.
   - Backend expires stale publisher heartbeats and abandoned pre-live sessions.

2. **Android system Back navigation**
   - Android Back now follows the route hierarchy across auth, onboarding, connections, studio, go-live, analytics, billing, settings, support and diagnostics.
   - Bottom-nav secondary tabs return to Home.
   - Root screens require a second Back press within ~1.8 seconds to close the Activity.

3. **First-account onboarding + permissions**
   - New account: verify email -> creator onboarding -> one consolidated device-permission setup -> Home.
   - Notification, microphone and camera Android permission dialogs open directly from the consolidated onboarding page.
   - Screen-capture consent remains protected by Android and is requested only when the broadcast starts.
   - Optional permission denial no longer blocks finishing onboarding.
   - Go-Live permission fixes are requested in-place; they no longer send the user back through onboarding permission pages.

4. **`permission denied for table ul_onboarding_state`**
   - New SQL `0620_mobile_backend_stabilization.sql` restores explicit `service_role` SQL privileges for API-only `ul_*` tables/sequences while keeping `anon` and `authenticated` direct table access revoked.
   - Any remaining legacy `auth.users` foreign keys on `ul_*` tables are rebound to `public.ul_users`.
   - Missing onboarding rows are created for existing custom-auth users.

5. **Go Live / preflight backend mismatch**
   - The supplied Android project currently defaults to `https://universallive.vercel.app/api/v1`.
   - If that deployment is older than the local final Nest backend, the app can receive `Cannot PUT /api/v1/streams/draft/current`.
   - Added scripts for safe local USB testing (`adb reverse`) and switching back to production.
   - Android now gives a clear backend-deployment mismatch error instead of exposing raw `Cannot PUT/POST` routing text.

6. **Splash**
   - Pure black splash background.
   - Existing Universal Live dark logo animates in with fade + scale.
   - Cyan brand tagline fades in after the logo.

## 1. Merge

Extract/merge the ZIP into:

```text
E:\UniversalLive
```

Do not overwrite your real `backend\.env` with an example file. The delivered ZIP intentionally excludes `.env` and Firebase service-account JSON.

## 2. Run the SQL hotfix

Supabase SQL Editor:

```text
backend/sql/final/0620_mobile_backend_stabilization.sql
```

Expected verification:

```text
service_can_select_onboarding = true
service_can_insert_onboarding = true
service_can_update_onboarding = true
users_without_onboarding = 0
```

## 3. Backend local validation

```powershell
cd E:\UniversalLive\backend
npm ci
npm run typecheck
npm run build
npm run dev
```

Keep that terminal running.

Second terminal:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
cd E:\UniversalLive
.\tools\mobile-backend-final-fix-verify.ps1
```

Expected final line:

```text
MOBILE + BACKEND FINAL FIX VERIFICATION PASSED
```

## 4. Physical Android phone + local backend testing

Connect the phone by USB with USB debugging enabled, then:

```powershell
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\use-local-android-backend.ps1
```

This changes the Android build URL to:

```text
http://127.0.0.1:3000/api/v1
```

and runs:

```text
adb reverse tcp:3000 tcp:3000
```

Then rebuild/reinstall the APK.

## 5. Android final build

```powershell
cd E:\UniversalLive
.\gradlew.bat clean
.\gradlew.bat :androidApp:processDebugGoogleServices
.\gradlew.bat :composeApp:compileKotlinMetadata :androidApp:compileDebugKotlin
.\gradlew.bat :androidApp:assembleDebug
```

## 6. Switch back to production backend

Only after the final backend has been deployed to the production URL:

```powershell
cd E:\UniversalLive
.\tools\verify-mobile-backend-contract.ps1 -BaseUrl "https://universallive.vercel.app/api/v1"
.\tools\use-production-android-backend.ps1
.\gradlew.bat :androidApp:assembleDebug
```

The production backend verifier must report:

```text
authProvider = universallive-db
mobileContractVersion = 2026.09-final
```

## Required manual test flow

- Cold open with **no live stream** -> Splash -> Home (or Welcome if signed out), never Active Live.
- Actual active publisher / backend status `live` -> returning to app opens Active Live.
- Bottom-nav secondary tab -> Android Back -> Home.
- Home -> Back once -> `Press back again to exit`; Back again -> app closes.
- New account -> Verify Email -> Creator setup -> consolidated permissions -> Home.
- Deny notification/camera -> onboarding can still finish.
- Enable notification -> status updates on the same screen and Finish Setup works.
- Go Live -> microphone/camera permission buttons invoke Android dialogs in-place.
- Preflight -> final backend route resolves; no raw `Cannot PUT /api/v1/streams/draft/current` error.

## Admin

Admin is deliberately not changed in this checkpoint. After this mobile/backend stabilization passes on-device testing, the admin workstream should use this backend contract as the locked canonical base.
