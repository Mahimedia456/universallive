-- Universal Live Backend Phase 11
-- Stream configuration presets/defaults.

begin;

create table if not exists public.ul_stream_configs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null default 'Default',
    resolution text not null default '1080p',
    width integer not null default 1920,
    height integer not null default 1080,
    fps integer not null default 30,
    bitrate_kbps integer not null default 6800,
    orientation text not null default 'auto',
    microphone_enabled boolean not null default true,
    internal_audio_enabled boolean not null default true,
    facecam_enabled boolean not null default false,
    scene_id uuid references public.ul_scenes(id) on delete set null,
    connection_id uuid references public.ul_streaming_connections(id) on delete set null,
    privacy text not null default 'public',
    is_default boolean not null default false,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ul_stream_configs_user_idx
on public.ul_stream_configs(user_id, created_at desc);

create unique index if not exists ul_stream_configs_one_default_uidx
on public.ul_stream_configs(user_id)
where is_default = true;

alter table public.ul_stream_configs enable row level security;

drop policy if exists "ul_stream_configs_own_read" on public.ul_stream_configs;
create policy "ul_stream_configs_own_read"
on public.ul_stream_configs
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
