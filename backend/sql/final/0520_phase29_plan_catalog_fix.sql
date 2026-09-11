-- Universal Live Phase 29 hotfix: ensure FREE / CREATOR / PRO catalog exists and is active.
begin;

insert into public.ul_plans(
  plan_key,
  name,
  description,
  is_active,
  sort_order,
  entitlements
)
values
(
  'free',
  'Free',
  'Core Universal Live broadcasting.',
  true,
  10,
  '{
    "max_simultaneous_destinations": 1,
    "max_resolution": "720p",
    "advanced_scenes": false,
    "advanced_overlays": false,
    "advanced_analytics": false
  }'::jsonb
),
(
  'creator',
  'Creator',
  'Professional creator streaming tools.',
  true,
  20,
  '{
    "max_simultaneous_destinations": 3,
    "max_resolution": "1080p",
    "advanced_scenes": true,
    "advanced_overlays": true,
    "advanced_analytics": true
  }'::jsonb
),
(
  'pro',
  'Pro',
  'Expanded production and analytics limits.',
  true,
  30,
  '{
    "max_simultaneous_destinations": 5,
    "max_resolution": "1080p",
    "advanced_scenes": true,
    "advanced_overlays": true,
    "advanced_analytics": true
  }'::jsonb
)
on conflict (plan_key) do update
set
  name = excluded.name,
  description = excluded.description,
  is_active = true,
  sort_order = excluded.sort_order,
  entitlements = excluded.entitlements,
  updated_at = now();

-- Billing display metadata added by Phase 26-33 migration.
update public.ul_plans
set
  display_monthly = case
    when plan_key = 'free' then 'PKR 0'
    else coalesce(display_monthly, 'Store price')
  end,
  display_yearly = case
    when plan_key = 'free' then 'PKR 0'
    else coalesce(display_yearly, 'Store price')
  end,
  updated_at = now()
where plan_key in ('free','creator','pro');

commit;
notify pgrst, 'reload schema';

-- PASS criteria: exactly 3 rows, all is_active = true.
select
  plan_key,
  name,
  is_active,
  sort_order,
  display_monthly,
  display_yearly
from public.ul_plans
where plan_key in ('free','creator','pro')
order by sort_order;
