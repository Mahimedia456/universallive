# UniversalLive Phase 30.4 - Screen Pipeline Fix

This patch targets the black-video problem where YouTube reports an excellent RTMP connection but the video canvas stays black.

Main fixes:
- Wait for the first real MediaProjection screen frame before rendering.
- Explicit sampler binding for `samplerExternalOES`.
- OpenGL validation around external texture creation/draw.
- Use an EGL config with `EGL_RECORDABLE_ANDROID=1` for MediaCodec Surface rendering.
- Validate `eglSwapBuffers`.
- Do not release the encoder's target Surface from `EglWindow`; ScreenCaptureService owns it.

Merge into:
E:\UniversalLive

Then run:
powershell -ExecutionPolicy Bypass -File E:\UniversalLive\tools\phase30_4-screen-pipeline-build.ps1

After build:
adb devices
adb uninstall com.universallive.app
adb install "E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk"
adb shell am start -n com.universallive.app/.MainActivity

If YouTube is still black, capture:
adb logcat -c
adb logcat | findstr /I "UniversalLive OpenGL EGL SurfaceTexture MediaProjection MediaCodec"
