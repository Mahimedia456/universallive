begin;

create table if not exists public.stream_destination_secrets (
  destination_id uuid primary key references public.stream_destinations(id) on delete cascade,
  user_id uuid not null references auth.users(id) on delete cascade,
  ciphertext text not null,
  iv text not null,
  auth_tag text not null,
  key_version integer not null default 1,
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
create index if not exists stream_destination_secrets_user_idx on public.stream_destination_secrets(user_id);

alter table public.stream_destination_secrets enable row level security;
-- Backend/service-role only. No anon/authenticated policies are intentionally created.
revoke all on public.stream_destination_secrets from anon, authenticated;
grant all on public.stream_destination_secrets to service_role;

alter table public.profiles add column if not exists last_seen_at timestamptz;
alter table public.profiles add column if not exists account_status text not null default 'active';

create table if not exists public.admin_audit_log (
  id bigint generated always as identity primary key,
  actor text not null,
  action text not null,
  target_type text,
  target_id text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);
create index if not exists admin_audit_created_idx on public.admin_audit_log(created_at desc);
alter table public.admin_audit_log enable row level security;
revoke all on public.admin_audit_log from anon, authenticated;
grant all on public.admin_audit_log to service_role;

grant usage, select on sequence public.admin_audit_log_id_seq to service_role;

commit;
