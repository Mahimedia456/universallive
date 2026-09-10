-- Universal Live Backend Phase 12
-- Broadcast sessions + destination state.

begin;

create table if not exists public.ul_broadcast_sessions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    title text,
    description text,
    status text not null default 'created',
    scene_id uuid references public.ul_scenes(id) on delete set null,
    stream_config_id uuid references public.ul_stream_configs(id) on delete set null,
    started_at timestamptz,
    ended_at timestamptz,
    last_heartbeat_at timestamptz,
    stop_reason text,
    client_session_id text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ul_broadcast_sessions_user_idx
on public.ul_broadcast_sessions(user_id, created_at desc);

create index if not exists ul_broadcast_sessions_status_idx
on public.ul_broadcast_sessions(user_id, status);

create table if not exists public.ul_broadcast_destinations (
    id uuid primary key default gen_random_uuid(),
    session_id uuid not null references public.ul_broadcast_sessions(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    connection_id uuid references public.ul_streaming_connections(id) on delete set null,
    platform text not null,
    status text not null default 'pending',
    started_at timestamptz,
    ended_at timestamptz,
    reconnect_count integer not null default 0,
    last_error_code text,
    last_error_message text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ul_broadcast_destinations_session_idx
on public.ul_broadcast_destinations(session_id);

alter table public.ul_broadcast_sessions enable row level security;
alter table public.ul_broadcast_destinations enable row level security;

drop policy if exists "ul_broadcast_sessions_own_read" on public.ul_broadcast_sessions;
create policy "ul_broadcast_sessions_own_read"
on public.ul_broadcast_sessions
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "ul_broadcast_destinations_own_read" on public.ul_broadcast_destinations;
create policy "ul_broadcast_destinations_own_read"
on public.ul_broadcast_destinations
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
