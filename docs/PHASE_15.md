# Phase 15 — Platform presets and multi-destination architecture

- BroadcastPlan and BroadcastMode introduced.
- Single-destination remains the default production mode.
- Direct multi-destination is modeled but should be used cautiously because every target multiplies uplink and device load.
- Relay multi-destination is the recommended production architecture: phone sends one feed to a relay, relay fans out to YouTube/Facebook/TikTok/custom targets.
- Existing RTMP profile presets remain compatible.
