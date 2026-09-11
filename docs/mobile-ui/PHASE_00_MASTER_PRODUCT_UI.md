# Universal Live — Phase 00 Master Product + Mobile UI/UX

Status: CANONICAL MOBILE ROADMAP

## Product promise

Universal Live is a mobile-first creator streaming product for configuring, starting, managing and monitoring live broadcasts without requiring a desktop OBS workflow.

Core promise: **Connect once. Configure quickly. Go live reliably.**

The UI must feel like a polished creator product rather than an engineering dashboard. Technical details should be exposed only where they help the creator make a decision or troubleshoot a stream.

## Product architecture

### Launch + authentication

- Splash
- Welcome
- Sign In
- Sign Up
- Verification / OTP
- Forgot Password
- Reset Password

### First-run setup

- Creator onboarding
- Permission hub
- Microphone / camera permission
- Screen-capture education
- Optional first destination

### Main application

- Home
- Studio
- LIVE
- Activity
- Profile

### Connections

- Destinations
- Add Destination
- Destination Detail
- Edit Connection
- Test Connection
- Set Default
- Enable / Disable
- Delete Connection

### Streaming

- Go Live Setup
- Destination Selection
- Stream Details
- Stream Quality
- Audio
- Facecam
- Preflight
- Active Live
- Stream Health
- Reconnect / Recovery
- End Stream

### Studio

- Scenes
- Scene Editor
- Overlays
- Audio Mixer
- Facecam
- Presets

### Account

- Profile
- Edit Profile
- Membership
- Billing
- Notifications
- Settings
- Diagnostics
- Support
- Legal / About

## Main navigation

Normal authenticated navigation has five major destinations:

1. Home — readiness, status and quick actions.
2. Studio — scenes, overlays, audio and visual preparation.
3. LIVE — dominant center action and streaming workflow.
4. Activity — stream history, summaries and analytics.
5. Profile — account, membership, settings and support.

### Active-live priority rule

If the native streaming service is actively starting, publishing or reconnecting, Active Live is the highest-priority application workspace.

- app reopened during live -> Active Live
- Android live notification tapped -> Active Live
- normal bottom navigation should not silently abandon the active-live workspace

## Authentication lifecycle

- Splash restores local/backend session.
- Active broadcast restored -> Active Live.
- Valid account with no active broadcast -> Home.
- No valid session -> Welcome.
- New account -> verification -> creator onboarding -> permissions -> Home.

## Membership behavior

Plans currently include FREE, CREATOR and PRO.

Membership restrictions must appear at the action boundary with an explanation. Never represent a plan restriction as an unexplained grey or inert button.

A restriction state must communicate:

- what action is restricted
- current allowance / limit
- what unlocks the action
- a clear plan action where appropriate

## Source-of-truth architecture

Account and synced product data:

`Backend -> application state -> UI`

Live device pipeline:

`Native streaming engine <-> live state <-> backend broadcast session`

Connections/destinations must use one canonical backend-connected model. Legacy local RTMP profiles must not compete with backend destinations for the same user-facing workflow.

## Design system

### Direction

Premium dark creator UI:

- near-black / deep-blue background
- restrained cyan / teal accent
- high-contrast white type
- subtle depth and glass-like raised surfaces
- precise spacing
- no excessive cyan borders or decorative noise

### Core colors

Existing theme source remains authoritative:

- background: near black
- raised surfaces: dark cyan-tinted charcoal
- primary action: Universal Live cyan
- live/destructive: red
- success: green
- primary body text: near-white
- secondary copy: cool grey-blue

### Primary button contract

Enabled primary button:

- cyan container
- **pure white text**
- white icon where present
- strong readable weight

Disabled primary button:

- visually muted container
- readable white foreground at reduced emphasis
- reason shown near the control when the user needs to know why it is unavailable

A bright cyan button with grey foreground is a QA failure.

### Secondary controls

- dark raised surface or outlined treatment
- readable near-white foreground
- cyan may be used for border/focus/selected state

### Destructive controls

- red-family emphasis
- white foreground
- confirmation required for destructive account/connection/live actions

### Spacing

Use the canonical spacing rhythm:

`4 / 8 / 12 / 16 / 20 / 24 / 32 dp`

Typical horizontal screen padding: `20–24dp`.

Interactive touch targets should normally be at least `44–48dp`.

### Cards

Card semantics should be explicit. Use distinct structures for:

- action cards
- status cards
- destination cards
- metric cards
- warnings
- membership cards
- information cards

Avoid making every content section the same generic rectangle.

## Interaction requirements

The mobile product must not ship with:

- buttons that do nothing
- text styled as a button with no clear affordance
- active cyan buttons with grey text
- duplicate screens for the same workflow
- local state that says connected while the backend says otherwise
- silent membership restrictions
- generic errors with no recovery action
- successful save actions with no feedback
- live sessions that continue with no recoverable active-live workspace

## Standard screen states

Every applicable feature phase must account for:

- Loading
- Loaded
- Empty
- Offline
- Backend unavailable
- Authentication expired
- Permission blocked
- Plan restricted
- Operation in progress
- Operation failed
- Success

## Phase implementation workflow

Every UI phase follows this sequence:

1. Screen / feature contract
2. Design image or mockup
3. User approval
4. Implement the approved design
5. Connect real function/backend behavior for that phase
6. Build
7. Device screenshot / functional QA
8. Lock the phase

A later phase should not casually redesign an approved locked phase.

## Canonical mobile roadmap

- Phase 00 — Master Product + UI/UX Documentation
- Phase 01 — Splash
- Phase 02 — Welcome
- Phase 03 — Sign In
- Phase 04 — Sign Up
- Phase 05 — Verification / OTP
- Phase 06 — Forgot + Reset Password
- Phase 07 — Creator Onboarding
- Phase 08 — Permissions Setup
- Phase 09 — Home
- Phase 10 — Connections
- Phase 11 — Add Destination
- Phase 12 — Destination Details / Edit
- Phase 13 — Go Live Setup
- Phase 14 — Stream Preflight
- Phase 15 — Active Live
- Phase 16 — Disconnect / Recovery
- Phase 17 — Studio
- Phase 18 — Scenes
- Phase 19 — Scene Editor
- Phase 20 — Overlays
- Phase 21 — Audio Mixer
- Phase 22 — Facecam
- Phase 23 — Stream Quality
- Phase 24 — Stream History
- Phase 25 — Stream Details / Analytics
- Phase 26 — Notifications
- Phase 27 — Profile
- Phase 28 — Edit Profile
- Phase 29 — Membership
- Phase 30 — Billing
- Phase 31 — Settings
- Phase 32 — Streaming Settings
- Phase 33 — Account Settings
- Phase 34 — Support
- Phase 35 — Diagnostics
- Phase 36 — Legal / About
- Phase 37 — Global Empty / Error / Loading States
- Phase 38 — Navigation + Global Components
- Phase 39 — End-to-End Functional QA
- Phase 40 — Release Polish

## Phase lock policy

After device approval, a phase becomes LOCKED. It may be changed later only for:

- confirmed bugs
- required cross-phase integration fixes
- explicit user redesign requests

The native RTMP publisher, MediaProjection capture, audio engine, foreground live service, backend auth, membership, connections API and broadcast-session behavior must be preserved while UI phases are redesigned unless the specific phase requires a functional repair.
