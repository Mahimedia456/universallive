-- SizeME Phase 22.2 - Custom database authentication
-- Replaces Supabase Auth usage with public.users + backend-issued JWT/refresh tokens.
-- Run AFTER PHASE_21_DATABASE.sql. If PHASE_22_AUTH.sql was already run, this migration safely disables its trigger.

begin;

create extension if not exists pgcrypto;

-- Stop creating profiles from Supabase auth.users. SizeME now owns authentication.
drop trigger if exists on_sizeme_auth_user_created on auth.users;
drop function if exists public.sizeme_handle_new_auth_user();

create table if not exists public.users (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  password_hash text not null,
  first_name text not null,
  last_name text,
  display_name text not null,
  email_verified_at timestamptz,
  password_changed_at timestamptz not null default now(),
  is_active boolean not null default true,
  last_login_at timestamptz,
  failed_login_attempts integer not null default 0,
  locked_until timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint users_email_nonempty check (length(trim(email)) >= 5),
  constraint users_first_name_check check (length(trim(first_name)) between 1 and 80),
  constraint users_display_name_check check (length(trim(display_name)) between 1 and 120),
  constraint users_failed_login_attempts_check check (failed_login_attempts >= 0)
);

alter table public.users add column if not exists failed_login_attempts integer not null default 0;
alter table public.users add column if not exists locked_until timestamptz;

create unique index if not exists users_email_lower_uidx on public.users (lower(email));
create index if not exists users_created_at_idx on public.users(created_at desc);

alter table public.profiles add column if not exists user_id uuid;

-- Drop/recreate only the custom-auth FK so this migration can be safely re-run.
alter table public.profiles drop constraint if exists profiles_user_id_fkey;
alter table public.profiles
  add constraint profiles_user_id_fkey foreign key (user_id) references public.users(id) on delete cascade;
create unique index if not exists profiles_user_id_uidx on public.profiles(user_id) where user_id is not null;
create index if not exists idx_profiles_user_id on public.profiles(user_id);

create table if not exists public.auth_otp_codes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users(id) on delete cascade,
  email text not null,
  purpose text not null,
  code_hash text not null,
  attempt_count integer not null default 0,
  max_attempts integer not null default 5,
  expires_at timestamptz not null,
  used_at timestamptz,
  created_at timestamptz not null default now(),
  constraint auth_otp_purpose_check check (purpose in ('verify_email','password_reset')),
  constraint auth_otp_attempts_check check (attempt_count >= 0 and max_attempts between 1 and 20)
);

create index if not exists auth_otp_lookup_idx
  on public.auth_otp_codes(user_id, purpose, created_at desc);
create index if not exists auth_otp_expiry_idx on public.auth_otp_codes(expires_at);

create table if not exists public.auth_refresh_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.users(id) on delete cascade,
  token_hash text not null unique,
  user_agent text,
  expires_at timestamptz not null,
  revoked_at timestamptz,
  last_used_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists auth_refresh_user_idx
  on public.auth_refresh_tokens(user_id, created_at desc);
create index if not exists auth_refresh_expiry_idx on public.auth_refresh_tokens(expires_at);

create table if not exists public.auth_audit_log (
  id bigserial primary key,
  user_id uuid references public.users(id) on delete set null,
  email text,
  event text not null,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists auth_audit_user_idx on public.auth_audit_log(user_id, created_at desc);
create index if not exists auth_audit_event_idx on public.auth_audit_log(event, created_at desc);

-- updated_at trigger for SizeME-owned users.
drop trigger if exists trg_users_updated_at on public.users;
create trigger trg_users_updated_at
before update on public.users
for each row execute function public.sizeme_set_updated_at();

-- These tables are backend-only. Keep RLS enabled with no anon/authenticated policies.
alter table public.users enable row level security;
alter table public.auth_otp_codes enable row level security;
alter table public.auth_refresh_tokens enable row level security;
alter table public.auth_audit_log enable row level security;

revoke all on public.users from anon, authenticated;
revoke all on public.auth_otp_codes from anon, authenticated;
revoke all on public.auth_refresh_tokens from anon, authenticated;
revoke all on public.auth_audit_log from anon, authenticated;

grant all on public.users to service_role;
grant all on public.auth_otp_codes to service_role;
grant all on public.auth_refresh_tokens to service_role;
grant all on public.auth_audit_log to service_role;
grant usage, select on sequence public.auth_audit_log_id_seq to service_role;

-- Existing profile policies tied to auth.uid() are intentionally not used by the app anymore.
-- The mobile app talks only to NestJS; NestJS uses the server secret/service role.

commit;

-- Verify:
-- select id,email,email_verified_at,is_active,created_at from public.users order by created_at desc;
-- select id,user_id,display_name,onboarding_completed from public.profiles order by created_at desc;
