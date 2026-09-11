# Universal Live — Phase 02 Welcome

Status: IMPLEMENTED / READY FOR DEVICE QA

## Scope

Phase 02 implements only the signed-out Welcome entry surface. The broader approved concept board contains ideas for later onboarding, connection and studio phases, but those screens are not implemented here so the one-feature-per-phase rule remains intact.

## Approved visual direction

- Premium near-black / deep blue creator UI.
- Existing Universal Live brand identity retained.
- Cyan is reserved for the primary action and important highlights.
- Gaming/creator visual is used as the Welcome hero.
- No generic card-grid landing page.
- No active cyan button may render grey text.
- Primary CTA uses cyan background with pure white foreground.
- Secondary CTA uses dark surface, cyan border and readable light text.

Reference files:

- `approved-welcome-screen-reference.png` — approved Welcome panel.
- `approved-phase02-design-board.png` — full approved concept board; non-Welcome panels are future-phase references only.

## Functional contract

### Entry

`Splash -> Welcome` when no authenticated session can be restored.

### Primary action

`Get Started -> Create Account`

This button is always actionable on the Welcome screen and uses `UlPrimaryButton`, whose foreground is explicitly `Color.White`.

### Secondary action

`Sign In -> Sign In`

No dead controls are present on this screen.

### Returning authenticated account

The Splash/session restoration logic remains unchanged from Phase 01:

- authenticated + no active broadcast -> Home
- authenticated + active broadcast -> Active Live
- signed out -> Welcome

## Responsive behavior

- System-bar safe areas are respected.
- Main hero uses remaining vertical space rather than a fixed phone screenshot.
- Hero artwork uses `ContentScale.Crop` so modern tall devices remain visually filled.
- Actions remain outside the artwork and are real Compose controls.
- The approved mockup is never used as a full-screen fake screenshot.

## Files changed

- `composeApp/src/commonMain/kotlin/com/universallive/app/features/welcome/WelcomeScreen.kt`
- `composeApp/src/commonMain/kotlin/com/universallive/app/features/auth/AuthScreens.kt`
- `composeApp/src/commonMain/kotlin/com/universallive/app/navigation/RootNavigation.kt`
- `composeApp/src/commonMain/composeResources/drawable/universallive_phase02_welcome_hero.png`
- `docs/mobile-ui/phase02/*`

## Deferred to later phases

The following are deliberately not implemented in Phase 02:

- Sign-in form redesign
- Sign-up form redesign
- OTP / verification
- creator onboarding carousel
- permissions setup
- connections/platform linking
- Studio UI
- Home UI

Those remain separate approved phases in the Phase 00 roadmap.

## Device QA checklist

1. Fresh/signed-out launch shows Splash, then Welcome.
2. Welcome does not flash before Splash completes.
3. Gaming hero fills its container without white/empty bands.
4. `Get Started` has cyan background and white text.
5. `Get Started` opens Create Account.
6. `Sign In` opens Sign In.
7. No button has grey foreground while enabled.
8. Back navigation from Sign In/Create Account returns to Welcome.
9. Existing authenticated session still skips Welcome.
10. Existing active broadcast still restores Active Live instead of Welcome/Home.

## Phase lock

After device approval, Phase 02 should be treated as locked. Later phases may consume its navigation destinations but should not redesign this screen unless there is a confirmed bug or an explicit redesign request.
