-- ============================================================================
-- Universal Live — Final Admin Runtime Hardening
-- 2026-09-12
--
-- SAFE SCOPE:
--   * Admin-owned tables only.
--   * Does NOT alter mobile user, streaming, scene, broadcast, telemetry,
--     connection, entitlement, or support schemas.
--   * Does NOT create a weak/default production password.
--
-- Run once in Supabase SQL Editor after the existing mobile/backend final SQL.
-- ============================================================================

begin;

create extension if not exists pgcrypto;

grant usage on schema public to service_role;

create table if not exists public.ul_admin_users (
  id uuid primary key default gen_random_uuid(),
  email text not null unique,
  password_hash text not null,
  display_name text not null,
  role text not null default 'ADMIN',
  permissions jsonb not null default '[]'::jsonb,
  is_active boolean not null default true,
  failed_login_count integer not null default 0,
  locked_until timestamptz,
  last_login_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ul_admin_role_ck check (
    role in ('SUPER_ADMIN','ADMIN','SUPPORT','MODERATOR','FINANCE','VIEWER')
  )
);

-- Fail safely if a legacy ul_admin_users table exists with the old Supabase-auth
-- shape. We deliberately do NOT destructively rewrite it inside a final migration.
do $$
declare
  missing_cols text;
begin
  select string_agg(req.col, ', ' order by req.col)
  into missing_cols
  from (
    values
      ('email'), ('password_hash'), ('display_name'), ('role'), ('permissions'),
      ('is_active'), ('failed_login_count'), ('locked_until'), ('last_login_at')
  ) as req(col)
  where not exists (
    select 1
    from information_schema.columns c
    where c.table_schema = 'public'
      and c.table_name = 'ul_admin_users'
      and c.column_name = req.col
  );

  if missing_cols is not null then
    raise exception
      'ul_admin_users has a legacy/incompatible schema. Missing canonical columns: %. Do not run old 0024_admin_console.sql. Migrate/reset only the admin-auth tables before continuing.',
      missing_cols;
  end if;
end
$$;

create table if not exists public.ul_admin_refresh_tokens (
  id uuid primary key default gen_random_uuid(),
  admin_user_id uuid not null references public.ul_admin_users(id) on delete cascade,
  token_hash text not null unique,
  expires_at timestamptz not null,
  revoked_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists ul_admin_refresh_user_idx
  on public.ul_admin_refresh_tokens(admin_user_id, expires_at desc);

create table if not exists public.ul_admin_otp_codes (
  id uuid primary key default gen_random_uuid(),
  admin_user_id uuid not null references public.ul_admin_users(id) on delete cascade,
  purpose text not null,
  code_hash text not null,
  attempts integer not null default 0,
  expires_at timestamptz not null,
  consumed_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists ul_admin_otp_user_idx
  on public.ul_admin_otp_codes(admin_user_id, purpose, created_at desc);

create table if not exists public.ul_admin_audit_log (
  id bigint generated always as identity primary key,
  admin_user_id uuid references public.ul_admin_users(id) on delete set null,
  action text not null,
  target_type text,
  target_id text,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.ul_admin_ui_preferences (
  admin_user_id uuid primary key references public.ul_admin_users(id) on delete cascade,
  sidebar_collapsed boolean not null default false,
  table_density text not null default 'comfortable'
    check (table_density in ('comfortable','compact')),
  timezone text,
  preferences jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now()
);

create table if not exists public.ul_admin_settings (
  key text primary key,
  value jsonb not null default 'null'::jsonb,
  updated_by_admin_id uuid references public.ul_admin_users(id) on delete set null,
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

-- Admin API only: browser/mobile users never get direct PostgREST table access.
alter table public.ul_admin_users enable row level security;
alter table public.ul_admin_refresh_tokens enable row level security;
alter table public.ul_admin_otp_codes enable row level security;
alter table public.ul_admin_audit_log enable row level security;
alter table public.ul_admin_ui_preferences enable row level security;
alter table public.ul_admin_settings enable row level security;

revoke all on public.ul_admin_users from anon, authenticated;
revoke all on public.ul_admin_refresh_tokens from anon, authenticated;
revoke all on public.ul_admin_otp_codes from anon, authenticated;
revoke all on public.ul_admin_audit_log from anon, authenticated;
revoke all on public.ul_admin_ui_preferences from anon, authenticated;
revoke all on public.ul_admin_settings from anon, authenticated;

grant select, insert, update, delete on public.ul_admin_users to service_role;
grant select, insert, update, delete on public.ul_admin_refresh_tokens to service_role;
grant select, insert, update, delete on public.ul_admin_otp_codes to service_role;
grant select, insert, update, delete on public.ul_admin_audit_log to service_role;
grant select, insert, update, delete on public.ul_admin_ui_preferences to service_role;
grant select, insert, update, delete on public.ul_admin_settings to service_role;

grant usage, select on sequence public.ul_admin_audit_log_id_seq to service_role;

insert into public.ul_admin_settings(key, value)
values
  ('support_sla_hours', '24'::jsonb),
  ('session_warning_minutes', '5'::jsonb),
  ('default_page_size', '50'::jsonb),
  ('maintenance_banner_enabled', 'false'::jsonb),
  ('maintenance_banner_text', '""'::jsonb)
on conflict (key) do nothing;

commit;

select pg_notify('pgrst', 'reload schema');

-- Verification ---------------------------------------------------------------
select
  has_table_privilege('service_role', 'public.ul_admin_users', 'SELECT') as service_can_read_admins,
  has_table_privilege('service_role', 'public.ul_admin_users', 'UPDATE') as service_can_update_admins,
  has_table_privilege('anon', 'public.ul_admin_users', 'SELECT') as anon_can_read_admins,
  has_table_privilege('authenticated', 'public.ul_admin_users', 'SELECT') as authenticated_can_read_admins;

select id, email, display_name, role, is_active, locked_until, last_login_at
from public.ul_admin_users
order by created_at desc;
