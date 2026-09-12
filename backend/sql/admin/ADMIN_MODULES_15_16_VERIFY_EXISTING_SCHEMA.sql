-- READ-ONLY verification for Admin Modules 15-16.
select table_name,column_name,data_type
from information_schema.columns
where table_schema='public'
and table_name in ('ul_system_flags','ul_backend_versions','ul_stream_telemetry','ul_stream_events')
order by table_name,ordinal_position;

select key,enabled,value from public.ul_system_flags order by key;
