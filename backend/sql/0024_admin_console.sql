begin;

create table if not exists public.ul_admin_users (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null unique references auth.users(id) on delete cascade,
  role text not null default 'admin'
    check (role in ('owner', 'admin', 'support', 'viewer')),
  is_active boolean not null default true,
  display_name text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists ul_admin_users_role_idx
  on public.ul_admin_users(role);

alter table public.ul_admin_users enable row level security;

-- Backend service-role only. No client CRUD policies intentionally created.

create or replace function public.ul_admin_dashboard_overview()
returns jsonb
language sql
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'users', (
      select count(*) from auth.users
    ),
    'profiles', (
      select count(*) from public.ul_creator_profiles
    ),
    'connections', (
      select count(*) from public.ul_streaming_connections
    ),
    'activeConnections', (
      select count(*) from public.ul_streaming_connections
      where coalesce(is_enabled, true) = true
    ),
    'scenes', (
      select count(*) from public.ul_scenes
    ),
    'broadcasts', (
      select count(*) from public.ul_broadcast_sessions
    ),
    'liveBroadcasts', (
      select count(*) from public.ul_broadcast_sessions
      where status in ('live', 'active', 'started')
    ),
    'supportOpen', (
      select count(*) from public.ul_support_tickets
      where status not in ('closed', 'resolved')
    ),
    'notifications', (
      select count(*) from public.ul_notifications
    )
  );
$$;

revoke all on function public.ul_admin_dashboard_overview() from public;
grant execute on function public.ul_admin_dashboard_overview() to service_role;

commit;
