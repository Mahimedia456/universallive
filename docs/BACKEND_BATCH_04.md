# UniversalLive Backend Batch 04 — Phase 08, 09, 10

## Phase 08 — Scenes
SQL:
`0011_scenes.sql`

Backend:
- list scenes
- fetch scene
- create scene
- update scene
- duplicate scene
- archive/delete scene
- default scene support
- aspect ratio/output canvas metadata

## Phase 09 — Sources & Layers
SQL:
`0012_scene_sources_layers.sql`

Backend:
- source CRUD
- source type/config JSON
- z-index/layer ordering
- visibility
- lock
- x/y/width/height
- rotation
- opacity
- reorder endpoint

Supported source_type values are intentionally open-ended for:
screen
camera
image
logo
text
browser
chat
alert
goal
background

## Phase 10 — Cloud Assets & Presets
SQL:
`0013_assets_presets.sql`

Backend:
- creator asset metadata
- storage bucket/path
- scene presets
- system templates
- create scene from preset

Actual binary upload to Supabase Storage can use the project's existing storage integration; this phase establishes the DB/API contract without creating a duplicate upload stack.

## Mobile
Shared Studio DTOs added:
- SceneDto
- SceneSourceDto
- CreatorAssetDto
- ScenePresetDto

These contracts match the Phase 07–09 mobile Studio UI.
Repository/event wiring will reuse the project's current HTTP/session infrastructure.
