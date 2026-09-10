-- Universal Live Backend Phase 15
-- Plans and user entitlements.

begin;

create table if not exists public.ul_plans (
    id uuid primary key default gen_random_uuid(),
    plan_key text not null unique,
    name text not null,
    description text,
    is_active boolean not null default true,
    sort_order integer not null default 0,
    entitlements jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.ul_user_entitlements (
    user_id uuid primary key references auth.users(id) on delete cascade,
    plan_key text not null default 'free',
    status text not null default 'active',
    source text not null default 'system',
    starts_at timestamptz,
    expires_at timestamptz,
    grace_until timestamptz,
    entitlements_override jsonb not null default '{}'::jsonb,
    updated_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

alter table public.ul_plans enable row level security;
alter table public.ul_user_entitlements enable row level security;

drop policy if exists "ul_plans_public_read" on public.ul_plans;
create policy "ul_plans_public_read"
on public.ul_plans
for select
to anon, authenticated
using (is_active = true);

drop policy if exists "ul_user_entitlements_own_read" on public.ul_user_entitlements;
create policy "ul_user_entitlements_own_read"
on public.ul_user_entitlements
for select
to authenticated
using (auth.uid() = user_id);

insert into public.ul_plans(plan_key, name, description, sort_order, entitlements)
values
(
  'free',
  'Free',
  'Core Universal Live broadcasting.',
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
  sort_order = excluded.sort_order,
  entitlements = excluded.entitlements,
  updated_at = now();

commit;
notify pgrst, 'reload schema';
