# UniversalLive Backend Batch 05 — Phase 11, 12, 13, 14

## Phase 11 — Stream Configuration
SQL: `0014_stream_configuration.sql`

Adds:
- resolution
- output dimensions
- FPS
- bitrate
- orientation
- microphone/internal audio
- facecam default
- scene
- destination
- privacy
- default config

## Phase 12 — Broadcast Sessions
SQL: `0015_broadcast_sessions.sql`

Adds:
- session create/start/heartbeat/end
- per-destination session state
- reconnect count
- destination errors
- client session id
- start/end timestamps

RTMP media still goes device -> streaming platform.
Backend stores control-plane/session state only.

## Phase 13 — Telemetry & Health
SQL: `0016_stream_telemetry.sql`

Adds:
- bitrate samples
- target bitrate
- FPS
- dropped frames
- video/audio frame counts
- encoder info
- network/publish/audio state
- thermal/battery slots
- stream recovery/error events

## Phase 14 — History & Summary
SQL: `0017_stream_history_summary.sql`

Adds:
- history list/detail
- final summary table
- duration
- average/peak bitrate
- average FPS
- dropped frames
- reconnect count
- destination success/failure counts
- total video/audio frames

## Mobile contract
`LiveBackendDtos.kt` matches:
- Go Live configuration
- active stream session
- telemetry
- post-stream summary/activity

Actual mobile repository/event wiring should use the existing UniversalLive networking/session layer, not a duplicate HTTP stack.
