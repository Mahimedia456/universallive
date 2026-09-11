-- ============================================================================
-- Universal Live — Backend Phases 24–25
-- Rich history filtering and materialized technical analytics fields.
-- Run AFTER 0400.
-- ============================================================================

begin;

alter table public.ul_stream_summaries
  add column if not exists min_bitrate_kbps integer,
  add column if not exists avg_rtmp_upload_kbps integer,
  add column if not exists avg_encoded_fps numeric(7,2),
  add column if not exists avg_sent_fps numeric(7,2),
  add column if not exists max_video_packet_age_ms integer,
  add column if not exists max_audio_packet_age_ms integer,
  add column if not exists keyframe_interval_avg_ms integer,
  add column if not exists video_pts_monotonic boolean,
  add column if not exists audio_pts_monotonic boolean,
  add column if not exists health_grade text,
  add column if not exists warning_count integer not null default 0,
  add column if not exists error_count integer not null default 0;

create index if not exists ul_broadcast_sessions_user_status_created_idx
  on public.ul_broadcast_sessions(user_id, status, created_at desc);
create index if not exists ul_stream_events_session_severity_idx
  on public.ul_stream_events(session_id, severity, created_at);

commit;
notify pgrst, 'reload schema';

select column_name
from information_schema.columns
where table_schema='public' and table_name='ul_stream_summaries'
  and column_name in ('avg_rtmp_upload_kbps','avg_encoded_fps','avg_sent_fps','health_grade','warning_count','error_count')
order by column_name;
