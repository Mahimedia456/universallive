-- Universal Live Backend Phase 03
-- Creator profile / onboarding data.

begin;

create table if not exists public.ul_creator_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  username text,
  avatar_url text,
  creator_type text,
  onboarding_completed boolean not null default false,
  default_scene_id uuid,
  default_destination_id uuid,
  locale text,
  timezone text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists ul_creator_profiles_username_uidx
  on public.ul_creator_profiles(lower(username))
  where username is not null;

alter table public.ul_creator_profiles enable row level security;

drop policy if exists "ul_creator_profiles_own_read" on public.ul_creator_profiles;
create policy "ul_creator_profiles_own_read"
on public.ul_creator_profiles
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "ul_creator_profiles_own_insert" on public.ul_creator_profiles;
create policy "ul_creator_profiles_own_insert"
on public.ul_creator_profiles
for insert
to authenticated
with check (auth.uid() = user_id);

drop policy if exists "ul_creator_profiles_own_update" on public.ul_creator_profiles;
create policy "ul_creator_profiles_own_update"
on public.ul_creator_profiles
for update
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
