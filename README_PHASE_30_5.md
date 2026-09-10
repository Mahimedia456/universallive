# UniversalLive Phase 30.5 — Professional Entire-Screen Capture

This patch is based on the uploaded current UniversalLive source.

## What changes

- Entire-device streaming with no facecam/overlay uses the most reliable Android path: `MediaProjection -> VirtualDisplay -> MediaCodec input Surface`.
- Facecam/overlay streams still use the OpenGL compositor.
- A compositor watchdog checks whether screen frames arrive and whether the compositor is producing visible content.
- If the compositor stalls or stays blank, the existing VirtualDisplay is detached from the compositor and switched to the encoder Surface without restarting the RTMP session.
- The Go Live monitor no longer misleadingly shows `STARTING` when direct capture is active. It shows `DIRECT SCREEN` and explains that the screen is feeding the encoder directly.
- Existing recordable EGL and external OES texture fixes are included.

## First test

For the first test, use:

- Capture source: Entire device
- Facecam: OFF
- Overlays: none
- 1080p / 30 FPS initially
- YouTube RTMP profile active

This intentionally forces the direct reliability path. Once the actual phone screen appears on YouTube, enable facecam and overlays and test the compositor path.

## Merge

Extract this ZIP into `E:\UniversalLive` and overwrite matching files.

## Build / install / run

```powershell
cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\phase30_5-build-install-run.ps1
```

## Diagnostic log

While live:

```powershell
adb logcat -c
adb logcat | findstr /I "UniversalLiveCapture MediaProjection MediaCodec VirtualDisplay EGL OpenGL"
```
