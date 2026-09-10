# Phase 09 — Facecam Overlay Foundation

Adds the shared facecam configuration and native stream-compositor contract while preserving Phases 1–8.

## Added
- Enable/disable facecam
- Front/back lens selection model
- Circle / rounded / square frame styles
- Mirroring option
- Normalized X/Y placement
- Resizable camera box (14–42% of stream dimension)
- Camera permission declaration
- Android service extras carrying facecam configuration into the native streaming pipeline

## Boundary of this phase
The configuration and compositor contract are complete. Camera pixels are not yet blended into the MediaCodec input surface in this checkpoint; that requires the OpenGL scene compositor introduced with the overlay/scenes work. Until that compositor lands, enabling facecam stores/passes the requested layout without changing the outgoing H.264 pixels.
