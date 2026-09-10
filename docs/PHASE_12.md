# Phase 12 — Live Scene Switching
- Switch saved scenes while capture is already live.
- Scene payload is sent to the Android foreground service via ACTION_UPDATE_SCENE.
- OpenGL compositor replaces overlay textures on its render thread.
- H.264 encoder, AAC encoder, MediaProjection and RTMP session stay running.
- "TAKE LIVE" UX appears while capture is active.
