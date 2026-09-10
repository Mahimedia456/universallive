# Phase 06 — H.264 Hardware Video Encoder

This phase replaces the Phase 4/5 test image surface with a real Android MediaCodec AVC/H.264 encoder surface.

## Added
- MediaCodec `video/avc` encoder
- Surface input from MediaProjection / VirtualDisplay
- Real stream resolution wiring: 480p, 720p, 1080p
- Real 30/60 FPS encoder configuration
- Real selected bitrate wiring
- CBR mode request
- 2-second keyframe interval
- Landscape / portrait / auto encoder dimensions
- Codec name visibility in UI
- Encoded frame and encoded byte counters
- Encoder lifecycle cleanup
- Existing microphone + Android playback capture retained

## Test
1. Pick a resolution, FPS and bitrate in Go Live.
2. Start Screen Capture and approve Android's consent dialog.
3. Verify H.264 ENCODER shows a codec name.
4. Encoded frames and bytes should keep increasing.
5. Open a game and verify the encoder continues while the app is backgrounded.

No network transmission is performed yet. Phase 07 will packetize the encoded H.264/AAC path and publish to RTMP/RTMPS.
