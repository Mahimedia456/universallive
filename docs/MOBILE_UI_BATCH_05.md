# UniversalLive Mobile UI Batch 05

Cumulative ZIP based on the approved Foundation Hotfix.

## Phase 13 — Live Resilience
61. Network Degraded
62. Reconnecting
63. Destination Failure
64. Stream Interrupted
65. End Stream flow / Live Recovery hub

## Phase 14 — Post Stream + Activity
66. Stream Processing
67. Stream Summary
68. Performance
69. Activity V2
70. Activity Detail

## Phase 15 — Subscription + Billing
71. Plans
72. Plan Comparison
73. Checkout Confirmation
74. Purchase State
75. Manage Subscription

## Routing updates
- Live Controls -> Live Recovery
- End Stream -> Stream Processing -> Stream Summary -> Activity
- Activity bottom tab -> Activity V2
- Home -> Current Plan -> Plans
- Plans -> Comparison / Checkout / Purchase State / Manage Subscription

## Architecture
- Shared Compose Multiplatform UI for Android and iOS
- Existing Android capture/RTMP implementation preserved
- Store prices are not invented/hard-coded
- Billing execution remains for the later billing/backend integration stage
- Stream telemetry screens consume existing CaptureSnapshot where appropriate

## Foundation fixes preserved
- canonical app-icon-512.png branding
- left aligned logo/header
- fixed header and fixed bottom navigation
- independently scrollable body
- keyboard-safe auth screens
