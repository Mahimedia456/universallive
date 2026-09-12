# Universal Live Admin — Phase 01 / Module 01
## Current Admin Audit + Architecture Mapping (UPDATED)

**Source baseline:** UniversalLive.zip supplied in this conversation  
**Purpose:** Establish the actual Admin baseline before UI redesign. Existing functionality is treated as canonical and must be preserved unless explicitly replaced.

### Audit principles
1. Do not delete or restructure existing working Admin features merely for visual redesign.
2. Admin UX is an enterprise control center, not a desktop copy of the mobile app.
3. Mobile is the capability/product reference; NestJS backend is the source of truth for server capabilities.
4. Every new Admin page should map to an actual backend capability or an explicitly approved Admin-only operation.
5. Mock-only data should not be introduced where a real backend contract exists.

## Required next implementation
**Phase 01 / Module 02 — Enterprise Shell + Universal Live Admin Design System**

Module 02 should implement the shell and reusable visual primitives first, while preserving all current routes and API behavior.
