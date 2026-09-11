# Universal Live - Locked Phases 13-16

Approved UI flow:

- Phase 13: Go Live Setup
- Phase 14: Stream Preflight
- Phase 15: Active Live
- Phase 16: Disconnect / Recovery

## Functional boundary

The current Android streaming engine publishes one direct RTMP output. The UI intentionally does not pretend that multiple destinations are already receiving video simultaneously. Saved destinations remain visible and the screen architecture is ready for the later backend multistream relay / platform OAuth API phase.

## Phase 13

1. Overview
2. Destination selection
3. Stream info
4. Stream settings
5. Audio and camera
6. Setup review

## Phase 14

1. Preflight introduction
2. Backend publish-route check
3. Device / permission check
4. Destination check
5. Preflight complete
6. Going Live / backend session + MediaProjection start

## Phase 15

- Active Live control room
- Live stats
- Live chat API boundary
- Live controls
- Stream health
- Live destinations

## Phase 16

- End confirmation
- Safe ending
- Connection lost
- Reconnecting
- Recovered
- Recovery failed

All active cyan CTA labels are explicit white. Universal Live uses the canonical `universallive_app_icon.png`. Platform badges are standardized visual brand badges; exact platform OAuth/channel metadata is connected in the later backend API phase.
