-- Universal Live Backend Phase 05
-- Streaming connections / destinations.
-- Additive migration; no existing tables are dropped.

begin;

create extension if not exists pgcrypto;

create table if not exists public.ul_streaming_connections (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    platform text not null,
    display_name text not null,
    external_account_id text,
    external_channel_id text,
    external_channel_name text,
    status text not null default 'disconnected',
    is_default boolean not null default false,
    is_enabled boolean not null default true,
    last_tested_at timestamptz,
    last_success_at timestamptz,
    last_error_code text,
    last_error_message text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ul_streaming_connections_user_idx
    on public.ul_streaming_connections(user_id, created_at desc);

create index if not exists ul_streaming_connections_user_platform_idx
    on public.ul_streaming_connections(user_id, platform);

create unique index if not exists ul_streaming_connections_one_default_uidx
    on public.ul_streaming_connections(user_id)
    where is_default = true;

alter table public.ul_streaming_connections enable row level security;

drop policy if exists "ul_streaming_connections_own_read" on public.ul_streaming_connections;
create policy "ul_streaming_connections_own_read"
on public.ul_streaming_connections
for select
to authenticated
using (auth.uid() = user_id);

-- Writes are backend mediated so encrypted credential relationships
-- and entitlement checks are never trusted to direct mobile writes.

commit;
notify pgrst, 'reload schema';
