UNIVERSAL LIVE — ADD DESTINATION OAUTH + CUSTOM RTMP
====================================================

GOAL
----
For YouTube / Facebook / Twitch / TikTok, Add Destination now presents two
explicit paths:

1) Connect with <Platform> (OAuth / third-party login)
2) Use Custom RTMP (manual Stream URL + Stream Key)

Custom RTMP itself goes directly to the Stream URL / Stream Key form.
Manual RTMP fields are NOT shown in the OAuth path.

IMPLEMENTED BACKEND FLOW
------------------------
POST /api/v1/streaming/oauth/:platform/start
  Authenticated Universal Live user starts OAuth.
  Backend creates anti-CSRF state and returns provider authorizationUrl.

GET /api/v1/streaming/oauth/:platform/callback
  Public provider callback.
  Backend validates one-time state, exchanges authorization code server-side,
  discovers provider account/channel, encrypts tokens, creates/updates the
  Universal Live destination and returns a branded completion page.

GET /api/v1/streaming/oauth/:platform/status
  Authenticated mobile app checks whether provider connection completed.

POST /api/v1/streaming/oauth/:platform/disconnect
  Removes Universal Live OAuth token material and marks connection disconnected.

Provider behavior:
- YouTube: OAuth + channel discovery + attempts to create a reusable YouTube
  Live stream ingest route. When available, encrypted RTMP publish credentials
  are provisioned automatically.
- Twitch: OAuth + broadcaster discovery + Get Stream Key. Encrypted Twitch RTMP
  route is provisioned automatically.
- Facebook: OAuth account linking is wired. Live publishing permissions/routes
  remain dependent on Meta app permissions/review; Custom RTMP stays available.
- TikTok: Login Kit OAuth account linking is wired. TikTok Login Kit does not by
  itself guarantee third-party LIVE ingest access; Custom RTMP stays available.

DATABASE
--------
Run:
  backend/sql/final/0640_platform_oauth_custom_auth.sql

It only hardens/creates OAuth-owned state/channel/token tables for the current
custom public.ul_users auth system. It does not replace mobile operational
streaming tables.

BACKEND ENVIRONMENT
-------------------
Set these in local backend .env and production Vercel Environment Variables:

OAUTH_CALLBACK_BASE_URL=https://YOUR-BACKEND/api/v1/streaming/oauth

YOUTUBE_OAUTH_CLIENT_ID=
YOUTUBE_OAUTH_CLIENT_SECRET=

TWITCH_OAUTH_CLIENT_ID=
TWITCH_OAUTH_CLIENT_SECRET=

FACEBOOK_OAUTH_CLIENT_ID=
FACEBOOK_OAUTH_CLIENT_SECRET=
FACEBOOK_GRAPH_VERSION=v23.0

TIKTOK_OAUTH_CLIENT_KEY=
TIKTOK_OAUTH_CLIENT_SECRET=

STREAM_SECRET_MASTER_KEY must already be present because OAuth and RTMP secrets
are encrypted with the existing Universal Live credential crypto service.

REGISTER THESE CALLBACK URLS
----------------------------
Replace BACKEND with your actual deployed backend host:

YouTube:
https://BACKEND/api/v1/streaming/oauth/youtube/callback

Twitch:
https://BACKEND/api/v1/streaming/oauth/twitch/callback

Facebook:
https://BACKEND/api/v1/streaming/oauth/facebook/callback

TikTok:
https://BACKEND/api/v1/streaming/oauth/tiktok/callback

The URL configured in each provider console MUST exactly match the generated
backend callback URL.

MOBILE UX
---------
Add Destination -> YouTube/Facebook/Twitch/TikTok
  -> Choose connection method
     -> Connect with Platform
        -> provider browser login/consent
        -> branded backend completion page
        -> return to Universal Live
        -> Check Connection
     OR
     -> Use Custom RTMP
        -> Connection name
        -> Stream URL
        -> Stream key
        -> Save Custom RTMP

Add Destination -> Custom RTMP
  -> manual Stream URL / Stream Key directly

No provider password is collected by Universal Live.

VERIFY
------
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\tools\verify-platform-oauth.ps1"

Expected:
PASS: OAuth source contract
PASS: backend production build
PASS: Android debug build
PASS: OAuth patch verification complete

DEPLOY ORDER
------------
1. Merge ZIP into E:\UniversalLive
2. Run SQL 0640 in Supabase SQL Editor
3. Configure provider developer apps + exact callback URLs
4. Add backend OAuth env vars in Vercel
5. Redeploy backend
6. Build/install Android debug APK
7. Test provider OAuth one provider at a time

DO NOT put provider CLIENT SECRET values in the Android app.
