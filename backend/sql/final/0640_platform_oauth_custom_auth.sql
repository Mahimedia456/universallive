-- Universal Live — Platform OAuth foundation for custom ul_users auth.
-- Safe scope: OAuth-owned tables only. Existing streaming connection/RTMP tables are reused.
-- Run AFTER final custom auth + streaming schema migrations.

begin;

create extension if not exists pgcrypto;

create table if not exists public.ul_oauth_states (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    platform text not null,
    state_hash text not null unique,
    redirect_uri text,
    status text not null default 'pending',
    error_message text,
    connection_id uuid,
    expires_at timestamptz not null,
    consumed_at timestamptz,
    created_at timestamptz not null default now()
);

alter table public.ul_oauth_states
  add column if not exists status text not null default 'pending',
  add column if not exists error_message text,
  add column if not exists connection_id uuid;

create table if not exists public.ul_platform_channels (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    connection_id uuid,
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

create table if not exists public.ul_oauth_credentials (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.ul_users(id) on delete cascade,
    connection_id uuid references public.ul_streaming_connections(id) on delete cascade,
    platform text not null,
    provider_user_id text not null,
    provider_display_name text,
    access_token_ciphertext text not null,
    refresh_token_ciphertext text,
    expires_at timestamptz,
    scope text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(user_id, platform, provider_user_id)
);

-- Remove stale legacy OAuth discovery rows that do not belong to the current
-- custom-auth user table before replacing old auth.users foreign keys.
delete from public.ul_platform_channels c
where not exists (select 1 from public.ul_users u where u.id = c.user_id);

delete from public.ul_oauth_states s
where not exists (select 1 from public.ul_users u where u.id = s.user_id);

-- Replace any legacy auth.users foreign keys on OAuth-owned tables with ul_users.
do $$
declare
  r record;
begin
  for r in
    select conname
    from pg_constraint
    where conrelid = 'public.ul_oauth_states'::regclass
      and contype = 'f'
  loop
    execute format('alter table public.ul_oauth_states drop constraint %I', r.conname);
  end loop;

  for r in
    select conname
    from pg_constraint
    where conrelid = 'public.ul_platform_channels'::regclass
      and contype = 'f'
  loop
    execute format('alter table public.ul_platform_channels drop constraint %I', r.conname);
  end loop;
end
$$;

alter table public.ul_oauth_states
  add constraint ul_oauth_states_user_fk
  foreign key (user_id) references public.ul_users(id) on delete cascade;

alter table public.ul_oauth_states
  add constraint ul_oauth_states_connection_fk
  foreign key (connection_id) references public.ul_streaming_connections(id) on delete set null;

alter table public.ul_platform_channels
  add constraint ul_platform_channels_user_fk
  foreign key (user_id) references public.ul_users(id) on delete cascade;

alter table public.ul_platform_channels
  add constraint ul_platform_channels_connection_fk
  foreign key (connection_id) references public.ul_streaming_connections(id) on delete cascade;

create index if not exists ul_oauth_states_user_idx
    on public.ul_oauth_states(user_id, created_at desc);
create index if not exists ul_platform_channels_user_platform_idx
    on public.ul_platform_channels(user_id, platform);
create index if not exists ul_oauth_credentials_user_platform_idx
    on public.ul_oauth_credentials(user_id, platform, updated_at desc);

alter table public.ul_oauth_states enable row level security;
alter table public.ul_platform_channels enable row level security;
alter table public.ul_oauth_credentials enable row level security;

-- OAuth state and token storage are backend/service-role only.
revoke all on public.ul_oauth_states from anon, authenticated;
revoke all on public.ul_oauth_credentials from anon, authenticated;
revoke all on public.ul_platform_channels from anon, authenticated;

grant select, insert, update, delete on public.ul_oauth_states to service_role;
grant select, insert, update, delete on public.ul_oauth_credentials to service_role;
grant select, insert, update, delete on public.ul_platform_channels to service_role;

commit;
notify pgrst, 'reload schema';

-- Verification
select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in ('ul_oauth_states','ul_platform_channels','ul_oauth_credentials')
order by table_name;
