# UniversalLive Backend Batch 03 — Phase 05, 06, 07

## Includes Batch 02 repair
The original `backend-batch02-apply.ps1` parser issue is bypassed.

Use either:
- `tools/backend-batch02-apply-FIXED.ps1` to repair only Batch 02, or
- `tools/backend-batch03-apply.ps1` once to register Batch 02 + Batch 03 modules together.

Since SQL 0005–0007 were already run, do not rerun them.

## Phase 05 — Streaming Connections
SQL: `0008_streaming_connections.sql`

Backend:
- list connections
- create connection
- edit/default/enable
- delete/disconnect record
- basic test-state endpoint

## Phase 06 — OAuth + Channel Discovery
SQL: `0009_platform_oauth_channels.sql`

Backend:
- short-lived OAuth state records
- YouTube/Facebook/Twitch provider-neutral OAuth start contract
- channel discovery storage
- channel selection

Actual provider authorization URLs/callback token exchange require the provider client IDs/secrets and are completed when those credentials are configured.

## Phase 07 — Custom RTMP Credential Vault
SQL: `0010_rtmp_credentials_vault.sql`

Backend:
- custom RTMP save
- AES-256-GCM encryption
- `STREAM_SECRET_MASTER_KEY`
- masked response
- credential status
- encrypted credential rows are not exposed by RLS to mobile clients

## Mobile contract
`StreamingConnectionDtos.kt` defines the shared Compose API contract for:
- connection list
- create connection
- platform channels
- custom RTMP credentials
- credential status

Connection screen event/repository wiring is done against the project's existing HTTP/session layer; do not create a second networking stack.
