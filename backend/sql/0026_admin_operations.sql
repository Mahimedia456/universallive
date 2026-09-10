begin;

create table if not exists public.ul_admin_notification_jobs (
  id uuid primary key default gen_random_uuid(),
  admin_user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  body text not null,
  audience text not null default 'all'
    check (audience in ('all','free','creator','pro')),
  status text not null default 'created'
    check (status in ('created','completed','failed')),
  created_at timestamptz not null default now(),
  completed_at timestamptz
);

create index if not exists ul_admin_notification_jobs_created_idx
  on public.ul_admin_notification_jobs(created_at desc);

alter table public.ul_admin_notification_jobs enable row level security;

commit;
