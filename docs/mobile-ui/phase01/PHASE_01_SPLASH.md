# Universal Live — Phase 01 Splash

Status: **Approved and implemented**

## Purpose
The splash screen establishes the Universal Live creator/broadcast identity while the app restores the local/backend session.

## Visual contract
- Premium near-black / deep-blue cinematic background.
- Universal Live cyan/white brand treatment with restrained red live accent.
- Streaming/gaming/worldwide creator imagery.
- Clean full-screen presentation with no cards or interactive controls.
- The approved mockup is stored beside this document as `approved-splash-mockup.png`.

## Startup contract
1. App launches into Splash.
2. Session restore begins immediately in parallel.
3. Splash remains visible for at least 1600 ms.
4. If the user is authenticated and has an active broadcast session, navigate to Active Live.
5. Otherwise authenticated users go to Home.
6. Signed-out users go to Welcome.

## Platform behavior
- Android's native launch splash remains dark and uses the Universal Live icon before Compose is ready.
- The Compose splash artwork removes mock-device status/navigation chrome so it can sit behind the real device system bars without duplicated UI.
- The artwork is padded for modern tall phones and rendered with `ContentScale.Crop`.

## Locked phase rule
Phase 01 is visually locked once accepted. Later phases should not redesign Splash unless there is a bug, a cross-phase dependency, or an explicit redesign request.
