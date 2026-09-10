# Phase 04 — Android Screen Capture

## Added
- Android MediaProjection permission flow.
- Fresh user consent for every capture session.
- Foreground service declared as `mediaProjection`.
- Android 14+ media-projection foreground-service permission.
- Native `VirtualDisplay` capture target.
- `ImageReader` test surface to verify real screen frames arrive.
- Shared capture state: idle, permission, starting, capturing, stopping, error.
- Start/stop screen-capture controls inside the Go Live screen.
- Cleanup through `MediaProjection.Callback.onStop()`.

## Intentional boundary
This phase does **not** encode or transmit video yet. The ImageReader surface proves the capture pipeline. Phase 5 adds audio. Phase 6 replaces the test surface with the H.264 encoder input surface.

## Android test
1. Connect the phone over Wi-Fi ADB.
2. Run `androidApp` from Android Studio.
3. Open **Go Live**.
4. Tap **START SCREEN CAPTURE**.
5. Accept Android's screen sharing dialog.
6. Return to Universal Live or switch to another app/game.
7. The status should become **CAPTURING** and `Frames observed` should increase.
8. Tap **STOP SCREEN CAPTURE** or stop sharing from Android's system UI.
