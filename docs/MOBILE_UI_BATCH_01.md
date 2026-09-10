# UniversalLive Mobile UI Upgrade — Batch 01

## Scope

### Phase 01 — Launch + Basic Auth
1. Splash
2. Welcome
3. Sign In
4. Create Account
5. Forgot Password

### Phase 02 — Verification + Recovery
6. Verify Email OTP
7. Expired OTP
8. Create New Password
9. Password Reset Success
10. Account Created Success

### Phase 03 — Creator Setup + Permissions
11. Creator Type
12. Permission Hub
13. Microphone + Camera
14. Screen Capture Education
15. Studio Ready

## Shared foundations added
- Final black/cyan UniversalLive theme
- white/cyan wordmark treatment and restrained red LIVE signal
- reusable buttons, cards, inputs, status badges and section headers
- shared AppRoute navigation state for Android and iOS
- keyboard/safe-area aware auth/setup layouts
- 5-tab navigation: Home / Studio / LIVE / Activity / Profile
- center LIVE tab uses a compact circular cyan broadcast control

## Important implementation rule
This batch is UI-first. Sign-in, verification and permission actions are represented as local UI navigation/state. Backend/API and native permission wiring are intentionally not replaced here. Existing streaming engine files are not included in this ZIP and therefore are not overwritten when this ZIP is merged.

## Merge
Extract this ZIP directly into:

E:\UniversalLive

Allow overwrite for matching UI/theme/navigation files.

## Build

cd E:\UniversalLive
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
.\tools\mobile-ui-batch01-build-apk.ps1

## Android APK
E:\UniversalLive\androidApp\build\outputs\apk\debug\androidApp-debug.apk

## iOS
All Batch 01 screens/routes/theme code lives in composeApp/commonMain, so it is shared with the existing iOS target. Native iOS permission and streaming behavior remains a later platform integration stage and is not duplicated as a second UI project.
