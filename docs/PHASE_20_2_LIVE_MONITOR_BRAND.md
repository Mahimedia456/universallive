# Phase 20.2 — Live monitor, preview, facecam diagnostics and brand integration

- Home shows a persistent LIVE/CAPTURING container while the broadcast pipeline is active.
- Live container includes destination, publish state, stream health, network bitrate, video/audio counters, facecam state and active scene.
- Go Live includes a staged broadcast status monitor (permission → engine → RTMP → capture ready → live).
- Added real compositor preview bridge for Android. Preview is sampled only while Home/Go Live preview UI is visible, so gameplay/background streaming does not continuously pay the preview-copy cost.
- Preview is taken after the screen + facecam + overlays compositor pass, so it represents outgoing composition.
- Added facecam active state and compositor active state to runtime telemetry.
- Fixed service handling for live scene updates and runtime bitrate updates.
- Added launcher icon and brand drawable assets.
- compileSdk aligned to API 37 for Compose 1.12 dependencies; targetSdk remains 36.

Note: Android screen capture can create a recursive preview effect when UniversalLive itself is brought to the foreground because MediaProjection captures the app screen. This does not affect gameplay while the game is foregrounded.
