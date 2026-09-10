# Phase 08 — Mixed AAC Audio + RTMP Audio

Phase 08 completes the first audio/video publishing pipeline.

## Added
- 48 kHz PCM16 mixer.
- Mono microphone -> stereo mix.
- Stereo Android playback/game audio -> stereo mix.
- Saturating PCM16 mix to avoid integer overflow/clipping wraparound.
- Fixed 20 ms audio frames.
- AAC-LC MediaCodec encoder, 48 kHz stereo, 160 Kbps.
- AAC output sent to the active RTMP/RTMPS publisher.
- Separate encoded/published audio counters and codec visibility.
- Mic-only continuation when playback capture is unavailable/blocked.

## Pipeline
Screen -> MediaProjection -> H.264 MediaCodec -> RTMP
Mic + capturable playback -> PCM mixer -> AAC MediaCodec -> RTMP

## Notes
Some Android apps/games can disallow playback capture. In that case microphone audio can still be encoded/published. Platform ingest credentials and account eligibility remain the user's responsibility.

The known AGP 9 / Kotlin Android plugin cleanup is intentionally deferred to the final build-fix/QA phase, per project plan.
