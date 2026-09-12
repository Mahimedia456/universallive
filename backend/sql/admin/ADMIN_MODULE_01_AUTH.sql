-- Universal Live Admin Module 01 - Custom DB Auth
-- Run in Supabase SQL Editor. This does NOT use Supabase Authentication.
create extension if not exists pgcrypto;
create table if not exists public.ul_admin_users (
 id uuid primary key default gen_random_uuid(), email text not null unique, password_hash text not null,
 display_name text not null, role text not null default 'ADMIN', permissions jsonb not null default '[]'::jsonb,
 is_active boolean not null default true, failed_login_count int not null default 0, locked_until timestamptz,
 last_login_at timestamptz, created_at timestamptz not null default now(), updated_at timestamptz not null default now(),
 constraint ul_admin_role_ck check(role in ('SUPER_ADMIN','ADMIN','SUPPORT','MODERATOR','FINANCE','VIEWER'))
);
create table if not exists public.ul_admin_refresh_tokens (
 id uuid primary key default gen_random_uuid(), admin_user_id uuid not null references public.ul_admin_users(id) on delete cascade,
 token_hash text not null unique, expires_at timestamptz not null, revoked_at timestamptz, created_at timestamptz not null default now()
);
create index if not exists ul_admin_refresh_user_idx on public.ul_admin_refresh_tokens(admin_user_id,expires_at desc);
create table if not exists public.ul_admin_otp_codes (
 id uuid primary key default gen_random_uuid(), admin_user_id uuid not null references public.ul_admin_users(id) on delete cascade,
 purpose text not null, code_hash text not null, attempts int not null default 0, expires_at timestamptz not null,
 consumed_at timestamptz, created_at timestamptz not null default now()
);
create index if not exists ul_admin_otp_user_idx on public.ul_admin_otp_codes(admin_user_id,purpose,created_at desc);
create table if not exists public.ul_admin_audit_log (
 id bigint generated always as identity primary key, admin_user_id uuid references public.ul_admin_users(id) on delete set null,
 action text not null, target_type text, target_id text, details jsonb not null default '{}'::jsonb, created_at timestamptz not null default now()
);
alter table public.ul_admin_users enable row level security;
alter table public.ul_admin_refresh_tokens enable row level security;
alter table public.ul_admin_otp_codes enable row level security;
alter table public.ul_admin_audit_log enable row level security;
-- No client RLS policies: these tables are service-role/backend only.
-- Initial local/dev super admin. Change email/password before production.
insert into public.ul_admin_users(email,password_hash,display_name,role,permissions)
values('admin@universallive.local',crypt('1231231234',gen_salt('bf',12)),'Universal Live Admin','SUPER_ADMIN','["*"]'::jsonb)
on conflict(email) do update set role='SUPER_ADMIN',permissions='["*"]'::jsonb,is_active=true;
-- Verification
select id,email,display_name,role,is_active,created_at from public.ul_admin_users order by created_at desc;
