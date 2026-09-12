-- Universal Live Admin Modules 08-10
-- READ-ONLY verification only.
-- This file intentionally makes ZERO schema changes.
-- It does not CREATE, ALTER, DROP, INSERT, UPDATE or DELETE mobile/backend tables.

-- 1. Verify stream telemetry tables used by Module 08/09.
select table_name
from information_schema.tables
where table_schema='public'
  and table_name in (
    'ul_broadcast_sessions',
    'ul_broadcast_destinations',
    'ul_stream_telemetry',
    'ul_stream_events',
    'ul_streaming_connections'
  )
order by table_name;

-- 2. Verify telemetry columns consumed by admin diagnostics.
select table_name,column_name,data_type
from information_schema.columns
where table_schema='public'
  and table_name='ul_stream_telemetry'
  and column_name in (
    'session_id','user_id','sampled_at','bitrate_kbps',
    'target_bitrate_kbps','fps','dropped_frames',
    'published_video_frames','published_audio_frames',
    'encoder_width','encoder_height','encoder_name',
    'network_status','publish_status','audio_status',
    'thermal_state','battery_percent'
  )
order by ordinal_position;

-- 3. Verify membership tables and current plans.
select table_name
from information_schema.tables
where table_schema='public'
  and table_name in ('ul_plans','ul_user_entitlements')
order by table_name;

select plan_key,name,is_active,sort_order,entitlements
from public.ul_plans
order by sort_order,plan_key;

-- 4. Membership distribution.
select plan_key,status,count(*) as users
from public.ul_user_entitlements
group by plan_key,status
order by plan_key,status;
