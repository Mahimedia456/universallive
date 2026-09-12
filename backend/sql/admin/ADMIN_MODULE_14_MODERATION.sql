-- UNIVERSAL LIVE ADMIN MODULE 14
-- ADMIN-ONLY moderation table.
-- Does NOT ALTER/DROP any mobile API table.
-- Modules 15/16 reuse existing ul_system_flags / ul_backend_versions.

create table if not exists public.ul_moderation_cases (
  id uuid primary key default gen_random_uuid(),
  target_type text not null check (target_type in ('user','stream','connection')),
  target_id uuid not null,
  subject text not null,
  reason text not null,
  severity text not null default 'medium' check (severity in ('low','medium','high','critical')),
  status text not null default 'open' check (status in ('open','investigating','actioned','dismissed','resolved')),
  action text not null default 'none',
  resolution text null,
  notes text null,
  evidence jsonb not null default '{}'::jsonb,
  created_by_admin_id uuid null references public.ul_admin_users(id) on delete set null,
  resolved_at timestamptz null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists ul_moderation_cases_status_idx on public.ul_moderation_cases(status,updated_at desc);
create index if not exists ul_moderation_cases_target_idx on public.ul_moderation_cases(target_type,target_id);
alter table public.ul_moderation_cases enable row level security;

notify pgrst, 'reload schema';

-- Verification only:
select table_name from information_schema.tables
where table_schema='public'
and table_name in ('ul_moderation_cases','ul_system_flags','ul_backend_versions')
order by table_name;
