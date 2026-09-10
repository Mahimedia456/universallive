# Phase 11 — Scenes + Overlay Editor

Adds reusable saved scenes and ordered overlays to the shared Compose UI and native Android compositor.

## Delivered
- Scenes screen with active-scene switching and save/delete.
- Overlays screen with text layers, visibility, width, opacity, ordering and deletion.
- Default LIVE and UNIVERSAL LIVE brand layers.
- Active scene layer payload passed into the capture service at capture start.
- Native OpenGL bitmap overlay rendering after screen/facecam composition and before H.264 encoding.
- Text/logo bitmaps generated with Android Canvas.
- Local image-path overlay loading with graceful skip for missing files.

## Deferred
A system file/image picker, persistent database-backed scene storage, animated/browser overlays and cloud sync are later phases.
