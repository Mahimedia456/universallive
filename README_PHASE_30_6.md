# UniversalLive Phase 30.6 - RTMP Real-time Timestamp/Pacing Fix

This patch addresses the YouTube Studio warning:

"Your encoder is sending data faster than realtime."

The current UniversalLive publisher was forwarding MediaCodec Surface timestamps unchanged.
Surface timestamps can be large absolute monotonic-clock values. RootEncoder itself rebases
Surface encoder timestamps before publishing; UniversalLive now does the same at the RTMP
publisher boundary and also enforces monotonic real-time pacing.

Merge into:
E:\UniversalLive

Then build:
cd E:\UniversalLive
.\gradlew.bat :androidApp:assembleDebug

APK:
E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk

Recommended first stream test:
- Entire device
- Facecam OFF
- Overlays OFF
- 1080p
- 30 fps
- 6000-8000 Kbps
