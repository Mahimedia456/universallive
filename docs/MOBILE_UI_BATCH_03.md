# UniversalLive Mobile UI Batch 03

Cumulative mobile UI overlay including Batch 01 + Batch 02 + Batch 03.

## Phase 07 — Studio + Scenes
31. Studio Home
32. Scene Library
33. Create Scene
34. Scene Templates
35. Scene Options

## Phase 08 — Core Scene Editor
36. Scene Editor Canvas
37. Add Source
38. Layers
39. Transform Inspector
40. Source Properties

## Phase 09 — Visual Source Editors
41. Face Camera Editor
42. Image / Logo Editor
43. Text Editor
44. Browser Source Editor
45. Background Editor

## Navigation
The Studio bottom tab now opens the premium Studio workspace rather than the legacy scene list.
All Phase 07–09 screens are routed through AppRoute.

## Architecture
- Shared Compose Multiplatform UI: Android + iOS
- Existing Android capture/RTMP engine is preserved
- Existing SceneState is reused where safe
- New editor screens are UI-first and prepared for backend/native binding in later integration stages
- Approved Universal Live logo/theme from Batch 02 remains included

## Merge
Extract over:
E:\UniversalLive

Then run:
.\tools\mobile-ui-batch03-build-apk.ps1
