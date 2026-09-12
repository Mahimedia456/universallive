# Universal Live Admin Capability Map

| Admin area | Current UI | Current API | Backend domain/reference | Status |
|---|---|---|---|---|
| Authentication | Login | admin-console/login, me | auth-v2, admin-console | Existing |
| Dashboard | Dashboard | overview, recent-users, recent-broadcasts, open-support | home-dashboard, streams, broadcast-sessions, support | Redesign |
| Creators | Creators + detail | creators, creators/:id, membership | profiles-v2, entitlements | Redesign + expand |
| Broadcasts | List + detail | broadcasts, broadcasts/:id, stop | broadcast-sessions, streams, lifecycle, history | Redesign + expand |
| Connections | Connections | connections, connections/:id | streaming-connections | Redesign + expand |
| Notifications | Notifications | notifications, notifications/broadcast | notifications-v2, push | Redesign |
| Support | List + detail | support, support/:id, update, reply | support-v2 | Redesign |
| Plans | Plans | plans, plans/:key | billing, entitlements | Redesign + expand |
| Audit | Audit | audit | security-audit, admin | Redesign + expand |
| Admin Users | Admin users | admin-users, admin-users/:id | admin-console, admin | Redesign |
| System | System | system/flags, system/health | system-state, config, health | Redesign + expand |
| Devices | Missing | Domain APIs exist | devices | New page |
| Diagnostics | Missing | Domain APIs exist | diagnostics | New page |
| Telemetry | Missing | Domain APIs exist | stream-telemetry | New page |
| Stream lifecycle/history | Missing | Domain APIs exist | stream-lifecycle, stream-history-v2 | New page |
| RTMP credentials | Missing | Domain APIs exist | rtmp-credentials | New page |
| Platform OAuth | Missing | Domain APIs exist | platform-oauth | New page |
| Destinations | Partial via connections | Domain APIs exist | destinations | New/expanded page |
| Studio workspace | Missing | Domain APIs exist | studio-workspace | New page |
| Scenes | Missing | Domain APIs exist | scenes, scenes-v2 | New page |
| Scene sources | Missing | Domain APIs exist | scene-sources | New page |
| Billing operations | Partial via plans | Domain APIs exist | billing | New page |
| Entitlements | Partial via membership | Domain APIs exist | entitlements | New/expanded page |

## Design direction
Premium enterprise control center: deep black/charcoal surfaces, restrained cyan accent, high information density, strong hierarchy, operational status visibility, and responsive layouts. This is not a desktop copy of mobile UI.
