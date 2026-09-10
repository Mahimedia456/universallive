-- Universal Live Backend Phase 02
-- Authentication/session support tables.
-- Additive only. Supabase Auth remains the identity provider.

begin;

create table if not exists public.ul_user_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id text,
  platform text,
  app_version text,
  refresh_token_hash text,
  ip_address inet,
  user_agent text,
  last_seen_at timestamptz not null default now(),
  revoked_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists ul_user_sessions_user_idx
  on public.ul_user_sessions(user_id, created_at desc);

create index if not exists ul_user_sessions_device_idx
  on public.ul_user_sessions(user_id, device_id);

alter table public.ul_user_sessions enable row level security;

drop policy if exists "ul_user_sessions_own_read" on public.ul_user_sessions;
create policy "ul_user_sessions_own_read"
on public.ul_user_sessions
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "ul_user_sessions_own_delete" on public.ul_user_sessions;
create policy "ul_user_sessions_own_delete"
on public.ul_user_sessions
for delete
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
