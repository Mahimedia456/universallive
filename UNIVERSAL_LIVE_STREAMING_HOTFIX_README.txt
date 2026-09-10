UNIVERSAL LIVE — STREAMING/LIVE HOTFIX
======================================

EXTRACT/MERGE TARGET
--------------------
Extract this ZIP directly into:
E:\UniversalLive

IMPORTANT: androidApp, composeApp, iosApp and tools belong directly under
E:\UniversalLive. Do NOT extract this package into E:\UniversalLive\mobile.

Backend files merge into:
E:\UniversalLive\backend

Your backend .env is intentionally NOT included. Keep your existing .env.

INCLUDED HOTFIXES
-----------------
- Default 1080p streaming bitrate aligned to 6800 Kbps.
- RTMP video/audio timestamps normalized to monotonic realtime pacing.
- Prevents buffered frames from being uploaded multiple seconds faster than realtime.
- Direct MediaProjection -> MediaCodec screen capture path for normal streaming to address black output.
- Compositor remains available when facecam/overlay composition is required.
- Android ongoing LIVE notification opens the Active Live workspace.
- While capture/publish is active, navigation is constrained to the Live workspace until stream end.
- Active backend broadcast restoration endpoint/client support.
- Primary cyan and LIVE/red button content uses explicit white text.
- Backend-connected Connections/Go Live flow remains the active UI.

AFTER MERGE
-----------
1) Safe cleanup preview:
   .\tools\cleanup-unused-legacy.ps1

2) If preview is correct:
   .\tools\cleanup-unused-legacy.ps1 -Apply

3) Build and print exact APK path:
   .\tools\build-debug-print-apk.ps1

4) Build + install to connected Android device:
   .\tools\build-debug-print-apk.ps1 -Install

Expected APK is normally under:
E:\UniversalLive\androidApp\build\outputs\apk\debug\
