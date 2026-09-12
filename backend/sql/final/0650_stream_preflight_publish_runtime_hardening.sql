-- ============================================================================
-- Universal Live — Stream Preflight / Publish Runtime Hardening
-- Safe idempotent companion for final Phase 13–16 runtime.
-- Run AFTER 0300_phase13_16_stream_lifecycle.sql.
-- Does not replace or redesign existing mobile operational tables.
-- ============================================================================

begin;

create extension if not exists pgcrypto;

create table if not exists public.ul_stream_preflights (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.ul_users(id) on delete cascade,
  status text not null default 'pending',
  title text,
  description text,
  scene_id uuid references public.ul_scenes(id) on delete set null,
  destination_connection_ids uuid[] not null default '{}'::uuid[],
  requested_config jsonb not null default '{}'::jsonb,
  checks jsonb not null default '[]'::jsonb,
  warnings jsonb not null default '[]'::jsonb,
  expires_at timestamptz not null default (now() + interval '5 minutes'),
  consumed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.ul_stream_preflights
  add column if not exists status text not null default 'pending',
  add column if not exists title text,
  add column if not exists description text,
  add column if not exists scene_id uuid references public.ul_scenes(id) on delete set null,
  add column if not exists destination_connection_ids uuid[] not null default '{}'::uuid[],
  add column if not exists requested_config jsonb not null default '{}'::jsonb,
  add column if not exists checks jsonb not null default '[]'::jsonb,
  add column if not exists warnings jsonb not null default '[]'::jsonb,
  add column if not exists expires_at timestamptz not null default (now() + interval '5 minutes'),
  add column if not exists consumed_at timestamptz,
  add column if not exists updated_at timestamptz not null default now();

create index if not exists ul_stream_preflights_user_created_idx
  on public.ul_stream_preflights(user_id, created_at desc);

create index if not exists ul_stream_preflights_user_status_idx
  on public.ul_stream_preflights(user_id, status, expires_at);

alter table public.ul_stream_preflights enable row level security;
revoke all privileges on table public.ul_stream_preflights from anon, authenticated;
grant select, insert, update, delete on table public.ul_stream_preflights to service_role;

alter table public.ul_streaming_connections
  add column if not exists last_health_status text,
  add column if not exists last_health_checked_at timestamptz;

-- Keep a stale pre-live session from permanently blocking a new preflight.
update public.ul_broadcast_sessions
set
  status = 'interrupted',
  publisher_state = 'disconnected',
  recovery_state = 'expired',
  ended_at = coalesce(ended_at, now()),
  stop_reason = coalesce(stop_reason, 'stale_preflight_cleanup'),
  updated_at = now()
where status in ('created','starting','connecting')
  and created_at < now() - interval '10 minutes';

commit;

notify pgrst, 'reload schema';

-- Verification
select
  to_regclass('public.ul_stream_preflights') as preflight_table,
  to_regclass('public.ul_streaming_connections') as connections_table,
  to_regclass('public.ul_stream_credentials') as credentials_table;

select column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name = 'ul_stream_preflights'
order by ordinal_position;
