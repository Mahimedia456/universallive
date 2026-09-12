-- UNIVERSAL LIVE ADMIN — MODULES 02–04
-- 02 Enterprise Shell, 03 Dashboard, 04 Users CRUD
-- Safe compatibility migration for the custom-database user model.
-- Does not use Supabase Auth from the admin frontend.

begin;
create extension if not exists pgcrypto;

-- Canonical application users table. If it already exists, preserve rows and add only missing fields.
create table if not exists public.users (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  password_hash text not null,
  first_name text not null default 'Creator',
  last_name text,
  display_name text not null default 'Creator',
  email_verified_at timestamptz,
  password_changed_at timestamptz not null default now(),
  is_active boolean not null default true,
  last_login_at timestamptz,
  failed_login_attempts integer not null default 0,
  locked_until timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.users add column if not exists email text;
alter table public.users add column if not exists password_hash text;
alter table public.users add column if not exists first_name text default 'Creator';
alter table public.users add column if not exists last_name text;
alter table public.users add column if not exists display_name text default 'Creator';
alter table public.users add column if not exists email_verified_at timestamptz;
alter table public.users add column if not exists password_changed_at timestamptz default now();
alter table public.users add column if not exists is_active boolean default true;
alter table public.users add column if not exists last_login_at timestamptz;
alter table public.users add column if not exists failed_login_attempts integer default 0;
alter table public.users add column if not exists locked_until timestamptz;
alter table public.users add column if not exists created_at timestamptz default now();
alter table public.users add column if not exists updated_at timestamptz default now();

create unique index if not exists users_email_lower_uidx on public.users(lower(email));
create index if not exists users_status_created_idx on public.users(is_active, created_at desc);

-- Creator profile compatibility used by mobile/admin detail screens.
create table if not exists public.ul_creator_profiles (
  user_id uuid primary key,
  display_name text,
  username text,
  avatar_url text,
  creator_type text,
  onboarding_completed boolean not null default false,
  default_scene_id uuid,
  default_destination_id uuid,
  locale text,
  timezone text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create unique index if not exists ul_creator_profiles_username_uidx
  on public.ul_creator_profiles(lower(username)) where username is not null;

-- Entitlement compatibility for FREE / CREATOR / PRO.
create table if not exists public.ul_user_entitlements (
  user_id uuid primary key,
  plan_key text not null default 'free',
  status text not null default 'active',
  source text not null default 'admin',
  starts_at timestamptz,
  expires_at timestamptz,
  grace_until timestamptz,
  entitlements_override jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
create index if not exists ul_user_entitlements_plan_idx on public.ul_user_entitlements(plan_key, status);

-- Per-admin UI preferences for shell density/sidebar state in later modules.
create table if not exists public.ul_admin_ui_preferences (
  admin_user_id uuid primary key references public.ul_admin_users(id) on delete cascade,
  sidebar_collapsed boolean not null default false,
  table_density text not null default 'comfortable',
  timezone text,
  preferences jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

alter table public.users enable row level security;
alter table public.ul_creator_profiles enable row level security;
alter table public.ul_user_entitlements enable row level security;
alter table public.ul_admin_ui_preferences enable row level security;

-- Backend service role owns all admin CRUD. No browser policies are added.
revoke all on public.ul_admin_ui_preferences from anon, authenticated;
grant all on public.ul_admin_ui_preferences to service_role;

notify pgrst, 'reload schema';
commit;

-- Verification
select table_name
from information_schema.tables
where table_schema='public'
  and table_name in ('users','ul_creator_profiles','ul_user_entitlements','ul_admin_ui_preferences')
order by table_name;

select id,email,display_name,is_active,email_verified_at,created_at
from public.users
order by created_at desc
limit 20;
