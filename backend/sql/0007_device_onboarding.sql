-- Universal Live Backend Phase 04
-- Device registration + onboarding/preferences.
-- Native OS permissions are NOT stored as authoritative backend permissions;
-- only user/app acknowledgement state is synced.

begin;

create table if not exists public.ul_devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  device_id text not null,
  platform text not null,
  push_token text,
  app_version text,
  os_version text,
  device_model text,
  last_seen_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(user_id, device_id)
);

create table if not exists public.ul_onboarding_state (
  user_id uuid primary key references auth.users(id) on delete cascade,
  creator_setup_completed boolean not null default false,
  permission_education_completed boolean not null default false,
  first_destination_prompt_completed boolean not null default false,
  microphone_acknowledged boolean not null default false,
  camera_acknowledged boolean not null default false,
  screen_capture_acknowledged boolean not null default false,
  notifications_acknowledged boolean not null default false,
  updated_at timestamptz not null default now()
);

alter table public.ul_devices enable row level security;
alter table public.ul_onboarding_state enable row level security;

drop policy if exists "ul_devices_own_all" on public.ul_devices;
create policy "ul_devices_own_all"
on public.ul_devices
for all
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

drop policy if exists "ul_onboarding_own_all" on public.ul_onboarding_state;
create policy "ul_onboarding_own_all"
on public.ul_onboarding_state
for all
to authenticated
using (auth.uid() = user_id)
with check (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
