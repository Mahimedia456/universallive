# Phase 08 - Permissions Setup - LOCKED

Approved visual: `APPROVED_PHASE_08_PERMISSIONS.png`

Flow:
1. Permissions welcome
2. Notifications
3. Microphone
4. Camera (optional)
5. Screen/audio capture education
6. Permissions complete

Implementation rules:
- Android notification, microphone and camera prompts are real runtime permission requests.
- Screen capture consent is NOT requested during onboarding because Android MediaProjection consent is session-scoped. The user is educated here and the real system prompt is requested at Go Live.
- Camera remains optional.
- Permission acknowledgements and completion are persisted through the existing onboarding backend contract.
- Completion also marks profile onboarding complete so returning users land on Home.
- Primary cyan CTAs always render white text.
