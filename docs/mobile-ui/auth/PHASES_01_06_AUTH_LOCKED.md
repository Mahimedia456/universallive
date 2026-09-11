# Universal Live — Locked Mobile Auth UI (Phases 01–06)

Status: LOCKED visual direction after user approval.

## Source of truth

`APPROVED_AUTH_FLOW_01_06.png` is the approved visual reference. The implementation follows its dark/navy background, cyan action color, white active-button labels, restrained cyan borders, premium creator imagery, and clear one-action-per-screen hierarchy.

## Phase 01 — Splash

- Show the complete approved splash artwork; never crop the top or bottom.
- Session restore happens behind the splash.
- Valid active broadcast -> Active Live.
- Valid normal session -> Home.
- Signed out -> Welcome.

## Phase 02 — Welcome

Three intro pages are part of the same phase:
1. Welcome to Universal Live
2. More Than Just Streaming
3. Stream Everywhere

All pages expose working navigation. Final primary action -> Create Account. Secondary action -> Sign In.

## Phase 03 — Sign In

- Real backend email/password sign-in.
- Password visibility control.
- Forgot-password navigation.
- Error state is visible and dismissible.
- First-time account -> Creator Setup; completed account -> Home.
- No unsupported social-auth buttons are exposed as dead controls.

## Phase 04 — Sign Up

- Full name
- Username
- Email
- Password
- Confirm password
- Terms acknowledgement
- Real backend registration call
- Verified/instant-session accounts -> success
- Verification-required accounts -> Phase 05

## Phase 05 — Verification / OTP

- Six digit input with six visual cells.
- Account verification uses the backend verification endpoint.
- Resend timer.
- Resend action calls the real backend.
- Success -> Email Verified success screen -> Creator Setup.

## Phase 06 — Forgot + Reset Password

1. Forgot password email entry
2. Recovery OTP verification
3. New password + confirmation + live requirements
4. Password reset success -> Sign In

All operations use the real MobileIntegrationState/backend methods.

## Locked button rule

Active primary button:
- Universal cyan background
- Pure white text and loader

Disabled primary button:
- Dark inactive surface
- Muted text

A disabled control must never look like a bright cyan active button with grey text.

## Implementation files

- `composeApp/src/commonMain/kotlin/com/universallive/app/features/auth/AuthVisualComponents.kt`
- `composeApp/src/commonMain/kotlin/com/universallive/app/features/auth/AuthScreens.kt`
- `composeApp/src/commonMain/kotlin/com/universallive/app/features/welcome/WelcomeScreen.kt`
- `composeApp/src/commonMain/kotlin/com/universallive/app/features/integration/ConnectedAuthScreens.kt`
- `composeApp/src/commonMain/kotlin/com/universallive/app/components/UlPrimitives.kt`

## Functional policy

A UI element is not added merely because it appears attractive in a concept image. Unsupported provider login controls are omitted until a backend provider exists; this prevents dead buttons while preserving the approved visual hierarchy.
