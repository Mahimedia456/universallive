-- ============================================================================
-- Universal Live — Backend Phases 13–16
-- Go-live draft/preflight, real publisher lifecycle, telemetry diagnostics,
-- disconnect/recovery state, and automatic stream finalization support.
-- Run AFTER 0100, 0101, 0102, 0200 and 0201.
-- ============================================================================

begin;

-- Phase 13: persist the creator's current go-live setup before preflight.
create table if not exists public.ul_stream_drafts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null unique references public.ul_users(id) on delete cascade,
  title text,
  description text,
  category text,
  privacy text not null default 'public',
  connection_id uuid references public.ul_streaming_connections(id) on delete set null,
  scene_id uuid references public.ul_scenes(id) on delete set null,
  stream_config jsonb not null default '{}'::jsonb,
  status text not null default 'draft',
  last_saved_at timestamptz not null default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists ul_stream_drafts_user_status_idx
  on public.ul_stream_drafts(user_id, status, updated_at desc);

alter table public.ul_stream_drafts enable row level security;
revoke all privileges on table public.ul_stream_drafts from anon, authenticated;

-- Phase 13/14: every real go-live attempt receives a short-lived preflight record.
create table if not exists public.ul_stream_preflights (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.ul_users(id) on delete cascade,
  status text not null default 'pending',
  title text,
  description text,
  scene_id uuid references public.ul_scenes(id) on delete set null,
  destination_connection_ids uuid[] not null default '{}'::uuid[],
  requested_config jsonb not null default '{}'::jsonb,
  checks jsonb not null default '[]'::jsonb,
  warnings jsonb not null default '[]'::jsonb,
  expires_at timestamptz not null default (now() + interval '5 minutes'),
  consumed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists ul_stream_preflights_user_created_idx
  on public.ul_stream_preflights(user_id, created_at desc);
create index if not exists ul_stream_preflights_user_status_idx
  on public.ul_stream_preflights(user_id, status, expires_at);

alter table public.ul_stream_preflights enable row level security;
revoke all privileges on table public.ul_stream_preflights from anon, authenticated;

-- Phase 15/16: backend state mirrors the native publisher instead of pretending
-- that a session is LIVE as soon as the user taps the button.
alter table public.ul_broadcast_sessions
  add column if not exists preflight_id uuid references public.ul_stream_preflights(id) on delete set null,
  add column if not exists publisher_state text not null default 'idle',
  add column if not exists publisher_instance_id text,
  add column if not exists last_publisher_event_at timestamptz,
  add column if not exists current_bitrate_kbps integer,
  add column if not exists current_fps numeric(7,2),
  add column if not exists last_video_packet_at timestamptz,
  add column if not exists last_audio_packet_at timestamptz,
  add column if not exists recovery_state text not null default 'none',
  add column if not exists recovery_attempts integer not null default 0,
  add column if not exists recoverable_until timestamptz,
  add column if not exists transition_seq bigint not null default 0;

create index if not exists ul_broadcast_sessions_recovery_idx
  on public.ul_broadcast_sessions(user_id, recovery_state, recoverable_until);

alter table public.ul_broadcast_destinations
  add column if not exists publisher_state text not null default 'idle',
  add column if not exists publisher_instance_id text,
  add column if not exists last_state_at timestamptz,
  add column if not exists last_video_packet_at timestamptz,
  add column if not exists last_audio_packet_at timestamptz,
  add column if not exists current_bitrate_kbps integer,
  add column if not exists current_fps numeric(7,2),
  add column if not exists rtmp_upload_kbps integer,
  add column if not exists socket_write_latency_ms integer,
  add column if not exists publisher_enqueue_latency_ms integer,
  add column if not exists rtmp_queue_depth integer;

-- Rich device telemetry. NULL means the native publisher/library cannot expose
-- that metric; the UI must show unknown rather than inventing a value.
alter table public.ul_stream_telemetry
  add column if not exists encoder_bitrate_kbps integer,
  add column if not exists rtmp_upload_kbps integer,
  add column if not exists encoded_fps numeric(7,2),
  add column if not exists sent_fps numeric(7,2),
  add column if not exists rtmp_queue_depth integer,
  add column if not exists socket_write_latency_ms integer,
  add column if not exists publisher_enqueue_latency_ms integer,
  add column if not exists last_video_packet_age_ms integer,
  add column if not exists last_audio_packet_age_ms integer,
  add column if not exists capture_frame_age_ms integer,
  add column if not exists keyframe_interval_ms integer,
  add column if not exists video_pts_monotonic boolean,
  add column if not exists audio_pts_monotonic boolean,
  add column if not exists reconnect_count integer,
  add column if not exists publisher_instance_id text;

-- Helpful for state-change/event de-duplication and diagnostics.
alter table public.ul_stream_events
  add column if not exists correlation_id text;

-- Ensure historical active sessions are recoverable for a bounded period after
-- this migration instead of becoming permanently stuck.
update public.ul_broadcast_sessions
set
  recovery_state = case
    when status in ('created','starting','connecting','live','reconnecting') then 'available'
    else recovery_state
  end,
  recoverable_until = case
    when status in ('created','starting','connecting','live','reconnecting')
      and recoverable_until is null then now() + interval '10 minutes'
    else recoverable_until
  end
where status in ('created','starting','connecting','live','reconnecting');

commit;
notify pgrst, 'reload schema';

-- Verification
select to_regclass('public.ul_stream_drafts') as draft_table,
       to_regclass('public.ul_stream_preflights') as preflight_table;

select column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name = 'ul_broadcast_sessions'
  and column_name in (
    'preflight_id','publisher_state','publisher_instance_id',
    'last_publisher_event_at','current_bitrate_kbps','current_fps',
    'last_video_packet_at','last_audio_packet_at','recovery_state',
    'recovery_attempts','recoverable_until','transition_seq'
  )
order by column_name;

select column_name, data_type
from information_schema.columns
where table_schema = 'public'
  and table_name = 'ul_stream_telemetry'
  and column_name in (
    'encoder_bitrate_kbps','rtmp_upload_kbps','encoded_fps','sent_fps',
    'rtmp_queue_depth','socket_write_latency_ms','publisher_enqueue_latency_ms','last_video_packet_age_ms',
    'last_audio_packet_age_ms','capture_frame_age_ms','keyframe_interval_ms',
    'video_pts_monotonic','audio_pts_monotonic','reconnect_count',
    'publisher_instance_id'
  )
order by column_name;
