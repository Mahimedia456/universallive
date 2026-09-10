begin;

create extension if not exists pgcrypto;

create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  display_name text,
  bio text,
  avatar_url text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.stream_destinations (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  platform text not null default 'custom',
  server_url text not null,
  secret_ref text,
  is_enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.stream_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  destination_id uuid references public.stream_destinations(id) on delete set null,
  title text,
  platform text not null default 'custom',
  status text not null default 'starting' check (status in ('starting','connecting','live','reconnecting','stopping','ended','error')),
  status_message text,
  target_bitrate_kbps integer,
  fps integer,
  width integer,
  height integer,
  started_at timestamptz,
  live_at timestamptz,
  ended_at timestamptz,
  last_seen_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists stream_sessions_user_created_idx on public.stream_sessions(user_id, created_at desc);
create index if not exists stream_sessions_user_status_idx on public.stream_sessions(user_id, status);

create table if not exists public.stream_metrics (
  id bigint generated always as identity primary key,
  stream_session_id uuid not null references public.stream_sessions(id) on delete cascade,
  bitrate_kbps integer,
  published_video_frames bigint,
  published_audio_frames bigint,
  dropped_frames bigint,
  reconnect_count integer,
  health text,
  created_at timestamptz not null default now()
);

create index if not exists stream_metrics_session_created_idx on public.stream_metrics(stream_session_id, created_at desc);

create table if not exists public.user_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  default_resolution text not null default '720p',
  default_fps integer not null default 60,
  default_bitrate_kbps integer not null default 6000,
  internal_audio_enabled boolean not null default true,
  microphone_enabled boolean not null default true,
  adaptive_bitrate_enabled boolean not null default true,
  updated_at timestamptz not null default now()
);

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer set search_path = public
as $$
begin
  insert into public.profiles(id, display_name)
  values (new.id, coalesce(new.raw_user_meta_data->>'display_name', split_part(coalesce(new.email, 'Streamer'), '@', 1)))
  on conflict (id) do nothing;

  insert into public.user_settings(user_id)
  values (new.id)
  on conflict (user_id) do nothing;

  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.stream_destinations enable row level security;
alter table public.stream_sessions enable row level security;
alter table public.stream_metrics enable row level security;
alter table public.user_settings enable row level security;

drop policy if exists profiles_own_select on public.profiles;
create policy profiles_own_select on public.profiles for select using (auth.uid() = id);
drop policy if exists profiles_own_update on public.profiles;
create policy profiles_own_update on public.profiles for update using (auth.uid() = id) with check (auth.uid() = id);

drop policy if exists destinations_own_all on public.stream_destinations;
create policy destinations_own_all on public.stream_destinations for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

drop policy if exists sessions_own_select on public.stream_sessions;
create policy sessions_own_select on public.stream_sessions for select using (auth.uid() = user_id);

drop policy if exists metrics_own_select on public.stream_metrics;
create policy metrics_own_select on public.stream_metrics for select using (
  exists (select 1 from public.stream_sessions s where s.id = stream_session_id and s.user_id = auth.uid())
);

drop policy if exists settings_own_all on public.user_settings;
create policy settings_own_all on public.user_settings for all using (auth.uid() = user_id) with check (auth.uid() = user_id);

commit;
