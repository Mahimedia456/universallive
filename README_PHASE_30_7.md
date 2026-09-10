# UniversalLive Phase 30.7

Targets:
- remove forced LIVE / UNIVERSAL LIVE watermark
- ensure legacy auto-branding does not force compositor mode
- prefer direct MediaProjection -> MediaCodec for plain entire-device streaming
- use Auto device orientation for entire-device capture
- default to 30 FPS
- enforce 6800 Kbps minimum configured video bitrate for YouTube destinations
- build APK only; no ADB detection or automatic phone install

Merge the ZIP into:
E:\UniversalLive

Then:
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\phase30_7-patch-build-apk.ps1

APK:
E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk

First validation:
- Entire device
- Facecam OFF
- no user overlays
- 1080p
- 30 FPS
- YouTube destination

Note:
YouTube's instantaneous "current bitrate" can still be lower on extremely static content because H.264 compression is content-dependent. The configured encoder target is kept at >= 6800 Kbps for YouTube, but the application should not fabricate filler packets merely to inflate the number.
