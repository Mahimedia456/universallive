-- Universal Live Backend Phase 13
-- Stream health / telemetry samples and events.

begin;

create table if not exists public.ul_stream_telemetry (
    id bigserial primary key,
    session_id uuid not null references public.ul_broadcast_sessions(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    sampled_at timestamptz not null default now(),
    bitrate_kbps integer,
    target_bitrate_kbps integer,
    fps numeric(6,2),
    dropped_frames integer,
    published_video_frames bigint,
    published_audio_frames bigint,
    encoder_width integer,
    encoder_height integer,
    encoder_name text,
    network_status text,
    publish_status text,
    audio_status text,
    thermal_state text,
    battery_percent integer,
    metadata jsonb not null default '{}'::jsonb
);

create index if not exists ul_stream_telemetry_session_time_idx
on public.ul_stream_telemetry(session_id, sampled_at);

create table if not exists public.ul_stream_events (
    id bigserial primary key,
    session_id uuid not null references public.ul_broadcast_sessions(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    event_type text not null,
    severity text not null default 'info',
    destination_id uuid references public.ul_broadcast_destinations(id) on delete set null,
    message text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists ul_stream_events_session_idx
on public.ul_stream_events(session_id, created_at);

alter table public.ul_stream_telemetry enable row level security;
alter table public.ul_stream_events enable row level security;

drop policy if exists "ul_stream_telemetry_own_read" on public.ul_stream_telemetry;
create policy "ul_stream_telemetry_own_read"
on public.ul_stream_telemetry
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "ul_stream_events_own_read" on public.ul_stream_events;
create policy "ul_stream_events_own_read"
on public.ul_stream_events
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
