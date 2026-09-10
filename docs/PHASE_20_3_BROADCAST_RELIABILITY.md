# UniversalLive Phase 20.3 — Broadcast Reliability Patch

This cumulative patch focuses on real-device streaming QA before backend work.

## Fixes
- Adds capture-source choice: Entire device vs Choose app/screen.
- Android 14+ uses MediaProjectionConfig for the requested capture mode; Android may still enforce its privacy chooser.
- RTMP connection is delayed until H.264 SPS/PPS is available, preventing ingest sessions that connect but wait indefinitely for decodable video.
- Requests a fresh H.264 sync frame when RTMP ingest becomes live.
- Keeps the latest live snapshot in-process so reopening UniversalLive while the foreground service is streaming restores the Home live monitor instead of showing an idle screen.
- Rich foreground notification shows LIVE/CONNECTING/RECONNECTING state, target, bitrate/media counters, opens the app, and provides STOP LIVE.
- Requests Android notification permission on Android 13+ without making it a prerequisite for capture.
- Home live card includes source, encoder, ingest, facecam, scene, network and live activity messages.
- Go Live screen shows explicit ingest diagnostics.

## YouTube QA
YouTube should move past "process will begin shortly" once the RTMP connection is accepted and the app begins publishing H.264 keyframes/AAC packets. Verify published video frames and network bitrate increase in UniversalLive.
