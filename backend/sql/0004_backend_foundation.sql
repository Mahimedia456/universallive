-- ============================================================================
-- Universal Live — Backend Upgrade Phase 01
-- Foundation / API contract metadata
-- Safe additive migration. Does NOT drop existing Universal Live tables.
-- Run in Supabase SQL Editor.
-- ============================================================================

begin;

create extension if not exists pgcrypto;

create table if not exists public.ul_backend_versions (
    id uuid primary key default gen_random_uuid(),
    api_version text not null,
    mobile_contract_version text not null,
    phase text not null,
    status text not null default 'active',
    deployed_at timestamptz not null default now(),
    notes text,
    created_at timestamptz not null default now()
);

create unique index if not exists ul_backend_versions_api_phase_uidx
    on public.ul_backend_versions(api_version, phase);

create table if not exists public.ul_api_audit_events (
    id uuid primary key default gen_random_uuid(),
    request_id text,
    user_id uuid,
    event_type text not null,
    route text,
    method text,
    status_code integer,
    duration_ms integer,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists ul_api_audit_events_created_at_idx
    on public.ul_api_audit_events(created_at desc);

create index if not exists ul_api_audit_events_user_created_idx
    on public.ul_api_audit_events(user_id, created_at desc);

create table if not exists public.ul_system_flags (
    key text primary key,
    value jsonb not null default '{}'::jsonb,
    description text,
    is_public boolean not null default false,
    updated_at timestamptz not null default now(),
    created_at timestamptz not null default now()
);

insert into public.ul_system_flags(key, value, description, is_public)
values
    (
        'backend_contract',
        jsonb_build_object(
            'api_version', 'v1',
            'mobile_contract_version', '2026.09',
            'phase', 'backend-01'
        ),
        'Universal Live public API compatibility metadata',
        true
    )
on conflict (key) do update
set value = excluded.value,
    description = excluded.description,
    is_public = excluded.is_public,
    updated_at = now();

insert into public.ul_backend_versions(
    api_version,
    mobile_contract_version,
    phase,
    status,
    notes
)
values (
    'v1',
    '2026.09',
    'backend-01',
    'active',
    'Foundation phase: API contract, health, version metadata and additive audit schema.'
)
on conflict (api_version, phase) do update
set mobile_contract_version = excluded.mobile_contract_version,
    status = excluded.status,
    notes = excluded.notes,
    deployed_at = now();

-- RLS: metadata is backend-managed.
alter table public.ul_backend_versions enable row level security;
alter table public.ul_api_audit_events enable row level security;
alter table public.ul_system_flags enable row level security;

-- Public clients may only read explicitly public system flags.
drop policy if exists "ul_system_flags_public_read" on public.ul_system_flags;
create policy "ul_system_flags_public_read"
on public.ul_system_flags
for select
to anon, authenticated
using (is_public = true);

-- No anon/authenticated insert/update/delete policies are created.
-- Service-role backend retains administrative access.

commit;

notify pgrst, 'reload schema';

-- Verification
select key, value, is_public
from public.ul_system_flags
where key = 'backend_contract';

select api_version, mobile_contract_version, phase, status, deployed_at
from public.ul_backend_versions
order by deployed_at desc
limit 5;
