UNIVERSAL LIVE — PREFLIGHT + PUBLISH RUNTIME FINAL HARDENING
================================================================

WHAT WAS WRONG
--------------
The phone's Network Check had two separate responsibilities mixed together:

1. It fetched/decrypted the native publish secret before backend preflight.
2. Then it called backend preflight.

That meant a missing route, credential decryption problem, stale deployment, or
missing RTMP contract could all collapse into the same "Publish route needs
attention" screen. The backend also only checked that a credential ROW existed;
it did not prove the deployed service could decrypt the credential and build a
usable RTMP/RTMPS publish contract.

WHAT THIS PATCH CHANGES
-----------------------
BACKEND
- Adds GET /api/v1/streams/runtime with contractVersion=2.
- Preflight now validates the REAL decrypted publish contract through
  RtmpCredentialsService.publishConfig().
- It catches bad STREAM_SECRET_MASTER_KEY / unreadable credentials / invalid
  RTMP URL before the mobile starts capture.
- Updates connection health using the result of the real route validation.
- Returns exact failed checks without returning the stream key.

MOBILE
- Network Check runs backend preflight first.
- It now parses the backend checks array.
- The first required failed check is shown to the creator instead of a generic
  message.
- Publish secrets are fetched only AFTER server preflight passes.
- A backend 404 now says exactly that the required lifecycle route is missing.

RTMP / YOUTUBE INGEST
- RTMP waits for an H.264 keyframe after each connect/reconnect before sending
  video.
- Audio also waits until that first video keyframe.
- SPS/PPS still gets configured before ingest opens.
- This prevents non-decodable inter frames from racing ahead of the first IDR
  when YouTube says the ingest is Excellent but preview remains Preparing.

SQL
---
Run:
backend/sql/final/0650_stream_preflight_publish_runtime_hardening.sql

It is idempotent and does not replace your existing mobile tables.

DEPLOY ORDER
------------
1) Merge ZIP into E:\UniversalLive
2) Run SQL 0650 in Supabase SQL editor
3) Backend local build:
   cd E:\UniversalLive\backend
   npm run build
4) Commit + push backend
5) Make sure Vercel backend Root Directory is:
   backend
6) Redeploy/force redeploy Production.
7) Verify production:
   cd E:\UniversalLive
   powershell.exe -NoProfile -ExecutionPolicy Bypass `
     -File ".\tools\verify-stream-runtime-final.ps1" `
     -BaseUrl "https://universallive.vercel.app/api/v1"
8) Android:
   .\gradlew.bat :androidApp:assembleDebug

EXPECTED MOBILE BEHAVIOR
------------------------
Good destination:
  Checking Connection
  -> Publish route is ready
  -> Backend: <destination> publish route is ready
  -> Continue enabled

Bad credentials:
  -> "<destination> publish route error: ..."

Missing credentials:
  -> "<destination> has no RTMP credentials"

Disabled:
  -> "<destination> is disabled"

Old/wrong production deployment:
  -> explicit missing lifecycle route error

YOUTUBE TEST
------------
Use the YouTube RTMP/RTMPS server + correct stream key. Start from Universal Live.
On a successful ingest, first video sent after connection will be an IDR/keyframe.
YouTube officially recommends H.264/AAC, CBR and a 2-second keyframe interval;
Universal Live retains the locked 2-second keyframe interval.

NO ADMIN TABLE CHANGES
----------------------
This patch does not modify admin auth/schema.
