-- Universal Live Admin Modules 11-13
-- READ-ONLY SCHEMA VERIFICATION.
-- ZERO schema/data mutation. Safe for the current mobile/backend database.

-- Billing / store purchases
select table_name,column_name,data_type
from information_schema.columns
where table_schema='public'
  and table_name='ul_store_purchases'
order by ordinal_position;

-- Notifications and push delivery
select table_name,column_name,data_type
from information_schema.columns
where table_schema='public'
  and table_name in ('ul_notifications','ul_push_deliveries','ul_devices')
order by table_name,ordinal_position;

-- Support
select table_name,column_name,data_type
from information_schema.columns
where table_schema='public'
  and table_name in ('ul_support_tickets','ul_support_messages')
order by table_name,ordinal_position;

-- Current record counts
select 'ul_store_purchases' as table_name,count(*) from public.ul_store_purchases
union all select 'ul_notifications',count(*) from public.ul_notifications
union all select 'ul_push_deliveries',count(*) from public.ul_push_deliveries
union all select 'ul_support_tickets',count(*) from public.ul_support_tickets
union all select 'ul_support_messages',count(*) from public.ul_support_messages;
