# UniversalLive Phases 21–23

## Phase 21 — Backend + DB
NestJS backend, Supabase clients, health endpoint, profiles, stream session tables, metrics, settings and RLS.

## Phase 22 — Auth
Supabase bearer-token validation in NestJS, `/auth/me`, auto `auth.users -> profiles` trigger.

## Phase 23 — Stream Session API
Start/status/metrics/stop/active/history endpoints. This is control-plane state; it does not relay RTMP video.

## Next
Phase 24 should wire Supabase Auth into mobile (login/signup/session persistence) and call stream APIs from the Android streaming service. Then Phase 25 can add cloud destinations/scenes. After that, return to native compositor QA for facecam/overlay output.
