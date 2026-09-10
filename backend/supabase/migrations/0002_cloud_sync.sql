begin;
create table if not exists public.scenes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  is_active boolean not null default false,
  payload jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index if not exists scenes_user_idx on public.scenes(user_id, updated_at desc);
create unique index if not exists scenes_one_active_per_user on public.scenes(user_id) where is_active;

alter table public.stream_destinations add column if not exists label text;
alter table public.stream_destinations add column if not exists server_url text;
alter table public.stream_destinations add column if not exists secret_ref text;
alter table public.stream_destinations add column if not exists enabled boolean not null default true;

alter table public.user_settings add column if not exists stream_config jsonb not null default '{}'::jsonb;
alter table public.user_settings add column if not exists ui_config jsonb not null default '{}'::jsonb;

alter table public.scenes enable row level security;
drop policy if exists scenes_owner_all on public.scenes;
create policy scenes_owner_all on public.scenes for all using (auth.uid() = user_id) with check (auth.uid() = user_id);
commit;
