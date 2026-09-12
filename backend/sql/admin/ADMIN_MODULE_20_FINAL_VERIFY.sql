-- UNIVERSAL LIVE ADMIN MODULE 20 — FINAL VERIFY
-- READ-ONLY. This script makes ZERO schema/data changes.

-- Admin-owned tables
select table_name
from information_schema.tables
where table_schema='public'
and table_name in (
 'ul_admin_users',
 'ul_admin_refresh_tokens',
 'ul_admin_audit_log',
 'ul_admin_ui_preferences',
 'ul_admin_settings',
 'ul_moderation_cases'
)
order by table_name;

-- Operational tables consumed by admin
select table_name
from information_schema.tables
where table_schema='public'
and table_name in (
 'ul_creator_profiles',
 'ul_streaming_connections',
 'ul_stream_credentials',
 'ul_broadcast_sessions',
 'ul_broadcast_destinations',
 'ul_stream_telemetry',
 'ul_stream_events',
 'ul_stream_summaries',
 'ul_plans',
 'ul_user_entitlements',
 'ul_notifications',
 'ul_push_deliveries',
 'ul_devices',
 'ul_support_tickets',
 'ul_support_messages',
 'ul_system_flags',
 'ul_backend_versions'
)
order by table_name;

-- Critical Module 16 compatibility check.
select key,value,description,is_public,updated_at,created_at
from public.ul_system_flags
order by key;

-- Admin role distribution.
select role,is_active,count(*) as admins
from public.ul_admin_users
group by role,is_active
order by role,is_active;

-- Recent audit activity.
select created_at,admin_user_id,action,target_type,target_id
from public.ul_admin_audit_log
order by created_at desc
limit 30;
