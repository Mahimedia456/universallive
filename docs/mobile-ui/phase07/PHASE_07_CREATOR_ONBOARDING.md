# Universal Live — Phase 07 Creator Onboarding

Status: LOCKED / IMPLEMENTED

Approved flow:
1. Welcome Creator
2. Content Type
3. Streaming Platforms
4. Streaming Experience
5. Main Goal
6. Setup Complete

## Product scope
Universal Live is a creator broadcast controller. It does not include a public social feed in this phase. Platform selection is preference-only during onboarding; account credentials are connected later in Connections.

Supported destination preferences:
- YouTube
- Facebook
- Twitch
- TikTok
- Custom RTMP

## Navigation
Account verified -> Phase 07 Creator Onboarding -> Phase 08 Permissions.

Returning authenticated users with `creator_setup_completed=true` but incomplete account onboarding resume at Permissions instead of replaying Phase 07.

## UX rules
- Premium near-black + cyan design system.
- Active cyan CTA text is always pure white.
- Content types and platforms are real selectable controls.
- No credential collection during onboarding.
- At least one content type and one destination preference remain selected.
- Final Continue persists `creator_setup_completed` through the existing backend onboarding endpoint.
- Primary content type is persisted to the creator profile's existing `creator_type` field.
- Permissions remain Phase 08 and are not mixed into Phase 07.

## Version
Mobile version: 0.34.0 (33)
