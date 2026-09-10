# UniversalLive Backend Batch 06 — Phase 15, 16, 17, 18

## Important Phase 10 SQL repair
The old `0013_assets_presets.sql` can fail with PostgreSQL `42P10`.

Reason:
The old file created a partial unique index:
`UNIQUE(preset_key) WHERE preset_key IS NOT NULL`

but used:
`ON CONFLICT (preset_key)`

The repaired file replaces it with a normal unique index.
PostgreSQL still permits multiple NULL values, while `ON CONFLICT (preset_key)` can now infer the index.

Use:
`0013_assets_presets_REPAIRED.sql`

Because the failed statement occurred inside `BEGIN ... COMMIT`, PostgreSQL normally rolls back that transaction. The repaired script is also idempotent.

## Phase 15 — Plans & Entitlements
SQL: `0018_plans_entitlements.sql`

Adds:
- Free
- Creator
- Pro
- effective entitlement JSON
- simultaneous destination limits
- resolution limits
- premium scene/overlay/analytics flags

## Phase 16 — Apple / Google Billing
SQL: `0019_store_purchases.sql`

Adds:
- purchase records
- transaction IDs
- hashed purchase token
- verification state
- expiry/renewal fields
- restore endpoint

Store prices remain sourced from Apple/Google rather than hard-coded.

Provider-side App Store Server API / Google Play Developer API verification is completed once production billing credentials are supplied.

## Phase 17 — Notifications
SQL: `0020_notifications.sql`

Adds:
- in-app notifications
- unread/read state
- action payload
- device push-delivery records

## Phase 18 — Support
SQL: `0021_support.sql`

Adds:
- support tickets
- diagnostics JSON
- ticket history
- user replies

## Mobile DTOs
Added billing, notifications, and support contracts matching Mobile UI Phase 15–17.
