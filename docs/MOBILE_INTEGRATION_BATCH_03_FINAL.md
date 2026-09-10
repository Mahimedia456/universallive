# UniversalLive Mobile Integration Batch 03 FINAL

## Phase 5 — Go Live + Streaming Session
This batch adds the backend control-plane wiring around the existing Android streaming engine:

- create broadcast session
- start session
- 15-second heartbeat
- telemetry submission contract
- end session
- selected saved destination
- selected cloud scene
- plan entitlement information in live flow

Important architecture remains unchanged:
Android media still publishes directly to YouTube/Facebook/Twitch/Custom RTMP.
NestJS/Supabase are the control plane, not the video relay.

## Phase 6 — Activity + Billing + Notifications + Support
- Activity backed by real stream history
- membership remains fetched from backend
- notifications list + read state
- support ticket create/list
- session restoration/logout from prior batches
- final API/build scripts

## Important provider limitation
Real YouTube/Facebook/Twitch OAuth still requires each provider's production app credentials and callback configuration.

## Important billing limitation
The mobile can read real plan entitlements now.
Actual Google Play/App Store purchase verification still requires store credentials and native billing integration.

## Android streaming engine
No encoder/capture/RTMP engine source is replaced by this batch.
