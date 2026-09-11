-- ============================================================================
-- Universal Live — Backend Phase 00/01
-- Custom database authentication foundation.
-- Supabase is PostgreSQL/Storage/Realtime infrastructure only; Supabase Auth is
-- not used by the mobile/backend login flow after this migration.
-- ============================================================================

begin;

create extension if not exists pgcrypto;

create or replace function public.ul_set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create table if not exists public.ul_users (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  password_hash text not null,
  full_name text,
  username text,
  email_verified_at timestamptz,
  password_changed_at timestamptz not null default now(),
  is_active boolean not null default true,
  last_login_at timestamptz,
  failed_login_attempts integer not null default 0,
  locked_until timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint ul_users_email_check check (length(trim(email)) between 5 and 254),
  constraint ul_users_failed_login_attempts_check check (failed_login_attempts >= 0)
);

create unique index if not exists ul_users_email_lower_uidx
  on public.ul_users(lower(email));

create index if not exists ul_users_created_at_idx
  on public.ul_users(created_at desc);

create table if not exists public.ul_auth_otp_codes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.ul_users(id) on delete cascade,
  email text not null,
  purpose text not null,
  code_hash text not null,
  attempt_count integer not null default 0,
  max_attempts integer not null default 5,
  expires_at timestamptz not null,
  used_at timestamptz,
  created_at timestamptz not null default now(),
  constraint ul_auth_otp_purpose_check
    check (purpose in ('verify_email', 'password_reset')),
  constraint ul_auth_otp_attempts_check
    check (attempt_count >= 0 and max_attempts between 1 and 20)
);

create index if not exists ul_auth_otp_lookup_idx
  on public.ul_auth_otp_codes(user_id, purpose, created_at desc);

create index if not exists ul_auth_otp_expiry_idx
  on public.ul_auth_otp_codes(expires_at);

create table if not exists public.ul_auth_refresh_tokens (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.ul_users(id) on delete cascade,
  token_hash text not null unique,
  user_agent text,
  expires_at timestamptz not null,
  revoked_at timestamptz,
  last_used_at timestamptz,
  created_at timestamptz not null default now()
);

create index if not exists ul_auth_refresh_user_idx
  on public.ul_auth_refresh_tokens(user_id, created_at desc);

create index if not exists ul_auth_refresh_expiry_idx
  on public.ul_auth_refresh_tokens(expires_at);

create table if not exists public.ul_auth_audit_log (
  id bigserial primary key,
  user_id uuid references public.ul_users(id) on delete set null,
  email text,
  event text not null,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists ul_auth_audit_user_idx
  on public.ul_auth_audit_log(user_id, created_at desc);

create index if not exists ul_auth_audit_event_idx
  on public.ul_auth_audit_log(event, created_at desc);

-- Preserve IDs of existing Supabase Auth users before foreign keys are rebound.
-- Their password is intentionally replaced by an unknown random bcrypt value;
-- they must use Universal Live password recovery before custom-auth login.
insert into public.ul_users (
  id,
  email,
  password_hash,
  full_name,
  username,
  email_verified_at,
  is_active,
  created_at,
  updated_at
)
select
  au.id,
  case
    when nullif(lower(trim(au.email)), '') is not null
      and not exists (
        select 1 from public.ul_users eu
        where eu.id <> au.id
          and lower(eu.email) = lower(trim(au.email))
      )
      then lower(trim(au.email))
    else au.id::text || '@legacy.invalid'
  end,
  crypt(gen_random_uuid()::text, gen_salt('bf', 12)),
  nullif(trim(coalesce(au.raw_user_meta_data->>'full_name', '')), ''),
  case
    when nullif(lower(trim(coalesce(au.raw_user_meta_data->>'username', ''))), '') is not null
      and not exists (
        select 1 from public.ul_users uu
        where uu.id <> au.id
          and uu.username is not null
          and lower(uu.username) = lower(trim(au.raw_user_meta_data->>'username'))
      )
      then lower(trim(au.raw_user_meta_data->>'username'))
    else null
  end,
  au.email_confirmed_at,
  true,
  coalesce(au.created_at, now()),
  now()
from auth.users au
on conflict do nothing;

create unique index if not exists ul_users_username_lower_uidx
  on public.ul_users(lower(username))
  where username is not null;

-- Trigger is Universal Live-owned; no copied external-project trigger is used.
drop trigger if exists trg_ul_users_updated_at on public.ul_users;
create trigger trg_ul_users_updated_at
before update on public.ul_users
for each row execute function public.ul_set_updated_at();

-- Backend-only tables. Mobile clients never read/write these directly.
alter table public.ul_users enable row level security;
alter table public.ul_auth_otp_codes enable row level security;
alter table public.ul_auth_refresh_tokens enable row level security;
alter table public.ul_auth_audit_log enable row level security;

revoke all on public.ul_users from anon, authenticated;
revoke all on public.ul_auth_otp_codes from anon, authenticated;
revoke all on public.ul_auth_refresh_tokens from anon, authenticated;
revoke all on public.ul_auth_audit_log from anon, authenticated;

-- Legacy projects expose service_role explicitly; new sb_secret_* server keys
-- also execute with service privileges through Supabase API.
grant all on public.ul_users to service_role;
grant all on public.ul_auth_otp_codes to service_role;
grant all on public.ul_auth_refresh_tokens to service_role;
grant all on public.ul_auth_audit_log to service_role;
grant usage, select on sequence public.ul_auth_audit_log_id_seq to service_role;

commit;

notify pgrst, 'reload schema';

-- Verification
select id, email, email_verified_at, is_active, created_at
from public.ul_users
order by created_at desc
limit 20;
