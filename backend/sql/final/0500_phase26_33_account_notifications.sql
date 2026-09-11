-- ============================================================================
-- Universal Live — Backend Phases 26–33
-- Notifications/push, profile, membership/billing, settings and account state.
-- Run AFTER 0400, 0401 and 0402.
-- ============================================================================

begin;

-- Phase 26 — push-capable notifications.

create table if not exists public.ul_notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.ul_users(id) on delete cascade,
  type text not null,
  title text not null,
  body text not null,
  severity text not null default 'info',
  action_type text,
  action_payload jsonb not null default '{}'::jsonb,
  read_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists ul_notifications_user_created_idx
  on public.ul_notifications(user_id, created_at desc);

create table if not exists public.ul_push_deliveries (
  id uuid primary key default gen_random_uuid(),
  notification_id uuid references public.ul_notifications(id) on delete cascade,
  device_id uuid references public.ul_devices(id) on delete set null,
  user_id uuid not null references public.ul_users(id) on delete cascade,
  provider text,
  status text not null default 'queued',
  provider_message_id text,
  error_code text,
  error_message text,
  sent_at timestamptz,
  created_at timestamptz not null default now()
);

alter table public.ul_notifications enable row level security;
alter table public.ul_push_deliveries enable row level security;
revoke all privileges on table public.ul_notifications from anon, authenticated;
revoke all privileges on table public.ul_push_deliveries from anon, authenticated;

alter table public.ul_devices
  add column if not exists push_token_updated_at timestamptz,
  add column if not exists push_enabled boolean not null default true;

alter table public.ul_push_deliveries
  add column if not exists updated_at timestamptz not null default now();

create index if not exists ul_push_deliveries_user_created_idx
  on public.ul_push_deliveries(user_id, created_at desc);
create index if not exists ul_push_deliveries_notification_idx
  on public.ul_push_deliveries(notification_id, created_at desc);

-- Phases 27–28 — richer creator profile and Supabase Storage avatar path.
alter table public.ul_creator_profiles
  add column if not exists bio text,
  add column if not exists website_url text;

-- Phase 31–32 — server-side settings/defaults.
create table if not exists public.ul_user_settings (
  user_id uuid primary key references public.ul_users(id) on delete cascade,
  notifications_enabled boolean not null default true,
  marketing_notifications_enabled boolean not null default false,
  stream_defaults jsonb not null default '{}'::jsonb,
  ui_config jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.ul_user_settings enable row level security;
revoke all privileges on table public.ul_user_settings from anon, authenticated;

insert into public.ul_user_settings(user_id)
select id from public.ul_users
on conflict (user_id) do nothing;

-- Phase 29–30 — plan/store metadata used by mobile billing verification.
alter table public.ul_plans
  add column if not exists monthly_product_id text,
  add column if not exists yearly_product_id text,
  add column if not exists display_monthly text,
  add column if not exists display_yearly text;

update public.ul_plans set
  display_monthly = case plan_key
    when 'free' then 'PKR 0'
    when 'creator' then coalesce(display_monthly, 'Store price')
    when 'pro' then coalesce(display_monthly, 'Store price')
    else coalesce(display_monthly, 'Store price') end,
  display_yearly = case plan_key
    when 'free' then 'PKR 0'
    else coalesce(display_yearly, 'Store price') end
where true;

alter table public.ul_store_purchases
  add column if not exists environment text,
  add column if not exists verified_plan_key text,
  add column if not exists verification_error text;

-- Public profile-avatar bucket. Object writes still happen through signed URLs
-- issued by the authenticated Universal Live backend.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'ul-profile-avatars',
  'ul-profile-avatars',
  true,
  5242880,
  array['image/webp','image/jpeg','image/png']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

commit;
notify pgrst, 'reload schema';

-- Verification
select to_regclass('public.ul_user_settings') as user_settings_table;
select column_name from information_schema.columns
where table_schema='public' and table_name='ul_creator_profiles'
  and column_name in ('bio','website_url') order by column_name;
select column_name from information_schema.columns
where table_schema='public' and table_name='ul_devices'
  and column_name in ('push_token','push_token_updated_at','push_enabled') order by column_name;
