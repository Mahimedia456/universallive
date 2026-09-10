-- Universal Live Backend Phase 14
-- Stream summary materialization table.
-- Kept as a table so backend can finalize values reliably.

begin;

create table if not exists public.ul_stream_summaries (
    session_id uuid primary key references public.ul_broadcast_sessions(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    duration_seconds integer,
    avg_bitrate_kbps integer,
    peak_bitrate_kbps integer,
    avg_fps numeric(6,2),
    dropped_frames integer not null default 0,
    reconnect_count integer not null default 0,
    total_video_frames bigint not null default 0,
    total_audio_frames bigint not null default 0,
    destination_count integer not null default 0,
    successful_destination_count integer not null default 0,
    failed_destination_count integer not null default 0,
    final_status text,
    finalized_at timestamptz,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ul_stream_summaries_user_idx
on public.ul_stream_summaries(user_id, finalized_at desc);

alter table public.ul_stream_summaries enable row level security;

drop policy if exists "ul_stream_summaries_own_read" on public.ul_stream_summaries;
create policy "ul_stream_summaries_own_read"
on public.ul_stream_summaries
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
