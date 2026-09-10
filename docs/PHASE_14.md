# Phase 14 — Recovery + Background Stability
- RTMP retry budget increased from 3 to 8 attempts.
- Capture/encoder stay alive while RTMP reconnects.
- Recovery policy model added for reconnect delay/keyframe strategy.
- Foreground capture service remains the owner of the native pipeline.
- Scene/bitrate commands are service actions so UI navigation does not tear down live capture.
- Final OEM/background/Doze behavior will be validated on the real phone in Phase 20.
