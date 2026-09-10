-- Universal Live Backend Phase 06
-- OAuth state + discovered platform channels.
-- Tokens are NOT stored in these discovery tables.

begin;

create table if not exists public.ul_oauth_states (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    platform text not null,
    state_hash text not null unique,
    redirect_uri text,
    expires_at timestamptz not null,
    consumed_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists ul_oauth_states_user_idx
    on public.ul_oauth_states(user_id, created_at desc);

create table if not exists public.ul_platform_channels (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    connection_id uuid references public.ul_streaming_connections(id) on delete cascade,
    platform text not null,
    external_channel_id text not null,
    channel_name text not null,
    channel_handle text,
    avatar_url text,
    can_stream boolean not null default false,
    metadata jsonb not null default '{}'::jsonb,
    discovered_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(user_id, platform, external_channel_id)
);

create index if not exists ul_platform_channels_user_platform_idx
    on public.ul_platform_channels(user_id, platform);

alter table public.ul_oauth_states enable row level security;
alter table public.ul_platform_channels enable row level security;

drop policy if exists "ul_platform_channels_own_read" on public.ul_platform_channels;
create policy "ul_platform_channels_own_read"
on public.ul_platform_channels
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
