-- UNIVERSAL LIVE ADMIN MODULES 05-07
-- READ-ONLY SCHEMA VERIFICATION
-- IMPORTANT: This file makes ZERO schema/data changes.
-- It exists specifically so Admin work does not alter the locked mobile API tables.

select table_name
from information_schema.tables
where table_schema = 'public'
  and table_name in (
    'ul_creator_profiles',
    'ul_streaming_connections',
    'ul_stream_credentials',
    'ul_broadcast_sessions',
    'ul_broadcast_destinations',
    'ul_stream_telemetry',
    'ul_stream_events',
    'ul_stream_summaries',
    'ul_scenes'
  )
order by table_name;

select 'ul_creator_profiles' as object, count(*) as rows from public.ul_creator_profiles
union all select 'ul_streaming_connections', count(*) from public.ul_streaming_connections
union all select 'ul_broadcast_sessions', count(*) from public.ul_broadcast_sessions
union all select 'ul_stream_telemetry', count(*) from public.ul_stream_telemetry;
