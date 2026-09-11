-- ============================================================================
-- Universal Live — Backend Phases 07–12
-- Creator onboarding persistence, device metadata, Home aggregation support,
-- connection lifecycle audit, destination health metadata.
-- Run AFTER 0100, 0101 and 0102.
-- ============================================================================

begin;

-- Phase 07: persist the choices presented by the locked creator-onboarding UI.
alter table public.ul_onboarding_state
  add column if not exists creator_content_types text[] not null default '{}'::text[],
  add column if not exists preferred_platforms text[] not null default '{}'::text[],
  add column if not exists experience_level text,
  add column if not exists primary_goal text,
  add column if not exists creator_setup_completed_at timestamptz,
  add column if not exists permission_setup_completed_at timestamptz;

-- Phase 08: device registration remains metadata only. Native OS permission
-- grants are authoritative on-device; backend stores only the last reported
-- app/device snapshot for support/readiness diagnostics.
alter table public.ul_devices
  add column if not exists permission_snapshot jsonb not null default '{}'::jsonb,
  add column if not exists permission_snapshot_at timestamptz,
  add column if not exists locale text,
  add column if not exists timezone text,
  add column if not exists metadata jsonb not null default '{}'::jsonb;

-- Phases 10–12: lifecycle/audit trail for user-managed destinations. No secret
-- plaintext is stored here; credential events only contain non-sensitive metadata.
create table if not exists public.ul_connection_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.ul_users(id) on delete cascade,
  connection_id uuid references public.ul_streaming_connections(id) on delete set null,
  event_type text not null,
  status text,
  message text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists ul_connection_events_user_created_idx
  on public.ul_connection_events(user_id, created_at desc);

create index if not exists ul_connection_events_connection_created_idx
  on public.ul_connection_events(connection_id, created_at desc);

alter table public.ul_connection_events enable row level security;
revoke all privileges on table public.ul_connection_events from anon, authenticated;

-- Health/details exposed to the mobile UI can be computed without decrypting
-- credentials. These fields are intentionally non-secret.
alter table public.ul_streaming_connections
  add column if not exists last_health_status text,
  add column if not exists last_health_checked_at timestamptz;

-- Helpful read indexes for the aggregated Home endpoint.
create index if not exists ul_onboarding_state_updated_idx
  on public.ul_onboarding_state(updated_at desc);

create index if not exists ul_devices_user_seen_idx
  on public.ul_devices(user_id, last_seen_at desc);

commit;
notify pgrst, 'reload schema';

-- Verification
select column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name = 'ul_onboarding_state'
  and column_name in (
    'creator_content_types',
    'preferred_platforms',
    'experience_level',
    'primary_goal',
    'creator_setup_completed_at',
    'permission_setup_completed_at'
  )
order by column_name;

select to_regclass('public.ul_connection_events') as connection_events_table;
