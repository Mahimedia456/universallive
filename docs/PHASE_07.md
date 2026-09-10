# Phase 07 — RTMP / RTMPS Video Publishing

## Added
- Real RTMP/RTMPS publisher backed by RootEncoder 2.8.1.
- Hardware H.264 frames from the existing MediaCodec encoder are forwarded to RTMP.
- SPS/PPS are taken from MediaCodec output format and supplied to the publisher.
- First enabled + valid RTMP profile is selected for the current live session.
- Server URL and stream key are combined only inside Android runtime; the key is not printed in status text.
- Publish states: IDLE, CONNECTING, LIVE, RECONNECTING, ERROR, DISCONNECTED.
- Three reconnect attempts with 1.5 second delay.
- Published frame/byte counters and upload bitrate status.
- RTMP and RTMPS endpoints supported.

## Current pipeline
Screen -> MediaProjection -> VirtualDisplay -> MediaCodec H.264 -> RTMP/RTMPS publisher

Mic / game audio -> PCM capture + meters (not transported yet)

## Important Phase 7 boundary
Phase 7 sends VIDEO over RTMP. Phase 8 will add the AAC audio encoder/mixer and feed AAC frames to the same RTMP session.

## Test
1. Add a valid destination under Connections.
2. Enable that destination.
3. Go to Go Live.
4. Choose resolution/FPS/bitrate.
5. Start screen capture and accept Android's screen-sharing consent.
6. Watch RTMP PUBLISH status change CONNECTING -> LIVE.
7. Confirm Published frames and Published bytes increase.
8. Confirm the target platform receives video.
