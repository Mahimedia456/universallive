# Phase 05 — Android Audio Capture

Phase 05 extends the Phase 04 MediaProjection session with native PCM audio capture.

## Added

- Runtime `RECORD_AUDIO` permission flow before MediaProjection consent when audio is enabled.
- Microphone capture using `AudioRecord` at 48 kHz PCM 16-bit mono.
- Android 10+ playback/game audio capture using `AudioPlaybackCaptureConfiguration` at 48 kHz PCM 16-bit stereo.
- Playback usage matching for GAME, MEDIA and UNKNOWN.
- Independent worker threads for microphone and playback capture.
- Live byte counters and RMS level indicators in the Go Live status card.
- Graceful handling when another app/game blocks playback capture.
- Android foreground-service `microphone` + `mediaProjection` declarations.
- Cleanup for both AudioRecord instances when the user/system stops MediaProjection.

## Important limitation

Android playback capture depends on the source app's capture policy. A game can explicitly prevent its audio from being captured. The app must treat this as a platform/source restriction rather than a crash.

## Phase 06 handoff

The captured PCM paths are verification sinks in Phase 05. Phase 06 will replace the video test surface with a MediaCodec H.264 input surface and add the first encoder pipeline; audio PCM will then feed the mixer/AAC path.
