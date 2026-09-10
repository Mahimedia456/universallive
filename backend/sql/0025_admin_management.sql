begin;

create table if not exists public.ul_admin_actions (
  id uuid primary key default gen_random_uuid(),
  admin_user_id uuid not null references auth.users(id) on delete cascade,
  action text not null,
  target_type text not null,
  target_id text,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists ul_admin_actions_admin_created_idx
  on public.ul_admin_actions(admin_user_id, created_at desc);

create index if not exists ul_admin_actions_target_idx
  on public.ul_admin_actions(target_type, target_id);

alter table public.ul_admin_actions enable row level security;

-- Backend service-role only; no browser/client table policies.

commit;
