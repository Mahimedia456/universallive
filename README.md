# Universal Live Admin — Phase 01 / Module 01
## Current Admin Audit + Architecture Mapping

Baseline: UniversalLive.zip (provided project snapshot)

This module is an audit/mapping deliverable. No existing production code is intentionally modified in this module.

## Existing Admin application
Location: `apps/admin`
Stack: React 19 + Vite 7 + lucide-react

Existing routes/pages:
- Login
- Dashboard
- Creators
- Creator Detail
- Broadcasts
- Broadcast Detail
- Connections
- Notifications
- Support
- Support Detail
- Plans
- Audit
- Admin Users
- System

Existing shared UI:
- `AdminShell.jsx`
- `AdminUi.jsx`
- `styles.css`

## Existing Admin API surface
The NestJS `admin-console` controller currently exposes:
- login / me / runtime-status
- overview
- recent-users
- recent-broadcasts
- open-support
- creators + membership update
- connections + update
- broadcasts + stop
- support + update + detail + reply
- plans + update
- notifications + broadcast
- audit
- system flags + update
- system health
- creator detail
- broadcast detail
- admin users + update

## Capability gaps identified for later phases
The backend contains additional operational domains that are not represented as dedicated Admin pages yet, including:
- devices
- diagnostics
- stream telemetry
- stream lifecycle/history
- RTMP credentials
- platform OAuth
- destinations
- scenes / scene sources
- studio workspace
- billing
- entitlements
- security audit expansion
- system state expansion
- richer account/profile operations

These will be introduced in later phases rather than duplicating existing screens.

## Architecture rule
Admin UI must use the existing backend capabilities and real data. Avoid mock-only dashboards. Preserve existing Admin functionality while redesigning presentation and filling missing operational pages.

## Next module
Phase 01 / Module 02 — Enterprise Shell + Universal Live Admin Design System.
