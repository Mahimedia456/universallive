-- UNIVERSAL LIVE ADMIN MODULES 17-19
-- Admin-only support tables / compatibility.
-- Does NOT ALTER mobile user/stream tables.

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
  updated_by_admin_id uuid null references public.ul_admin_users(id) on delete set null,
  updated_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

alter table public.ul_admin_ui_preferences enable row level security;
alter table public.ul_admin_settings enable row level security;

revoke all on public.ul_admin_ui_preferences from anon, authenticated;
revoke all on public.ul_admin_settings from anon, authenticated;
grant all on public.ul_admin_ui_preferences to service_role;
grant all on public.ul_admin_settings to service_role;

insert into public.ul_admin_settings(key,value)
values
 ('support_sla_hours','24'::jsonb),
 ('session_warning_minutes','5'::jsonb),
 ('default_page_size','50'::jsonb),
 ('maintenance_banner_enabled','false'::jsonb),
 ('maintenance_banner_text','""'::jsonb)
on conflict(key) do nothing;

notify pgrst, 'reload schema';

select table_name
from information_schema.tables
where table_schema='public'
  and table_name in (
    'ul_admin_users',
    'ul_admin_audit_log',
    'ul_admin_ui_preferences',
    'ul_admin_settings'
  )
order by table_name;
