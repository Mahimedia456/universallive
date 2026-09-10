# Phase 10 — OpenGL compositor + real facecam stream composition

## Added
- EGL/OpenGL ES 2.0 encoder compositor.
- MediaProjection writes into an external OES screen texture instead of directly into MediaCodec.
- Camera2 facecam writes into a second external OES texture.
- Compositor draws screen first, then facecam into the H.264 encoder surface.
- Front/back camera selection, mirroring, normalized position/size and circle/rounded/square masks are consumed by the native compositor.
- Reusable 2D bitmap overlay renderer for logo/image/text textures.
- Shared overlay layer model for later scene editor wiring.
- Camera permission is requested only when facecam is enabled.

## Deliberately deferred
The AGP 9/KMP Gradle migration remains deferred to the final build-fix/QA checkpoint, as requested. Overlay asset picking/editor UI and scene persistence are Phase 11+ work.
