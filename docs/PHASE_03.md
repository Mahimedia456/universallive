# Phase 03 — RTMP/RTMPS Connections

Added:
- YouTube, Facebook, TikTok and Custom RTMP presets.
- RTMP/RTMPS URL validation.
- Stream key masking with optional reveal while editing.
- Saved connection profile state.
- Enable/disable and remove profile controls.
- Home and Go Live screens now show connection readiness.

Security note:
Phase 3 intentionally does not persist stream keys to disk. The state model keeps them only for the current runtime. Before release, Android Keystore / iOS Keychain persistence will be added so plain-text secrets are never written to ordinary preferences.
