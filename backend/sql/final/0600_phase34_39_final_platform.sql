-- ============================================================================
-- Universal Live — Backend Phase 34–39 FINAL
-- Support hardening, diagnostics, legal/about, global system state and QA runs.
-- Additive/idempotent migration for the custom Universal Live DB-auth stack.
-- Run AFTER 0500/0501 and 0520.
-- ============================================================================

begin;

create extension if not exists pgcrypto;

-- Phase 35: sanitized diagnostics snapshots. Never store passwords, JWTs,
-- stream keys or Firebase private keys in snapshot JSON.
create table if not exists public.ul_diagnostic_snapshots (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.ul_users(id) on delete cascade,
    snapshot jsonb not null default '{}'::jsonb,
    expires_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists ul_diagnostic_snapshots_user_created_idx
    on public.ul_diagnostic_snapshots(user_id, created_at desc);

-- Phase 36: legal/about source of truth + user acknowledgements.
create table if not exists public.ul_legal_documents (
    document_key text not null,
    version text not null,
    title text not null,
    body text not null,
    public_url text,
    effective_at timestamptz not null default now(),
    required_acceptance boolean not null default false,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    primary key (document_key, version)
);

create table if not exists public.ul_legal_acceptances (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.ul_users(id) on delete cascade,
    document_key text not null,
    version text not null,
    source text not null default 'mobile',
    accepted_at timestamptz not null default now(),
    created_at timestamptz not null default now(),
    unique(user_id, document_key, version),
    foreign key (document_key, version)
      references public.ul_legal_documents(document_key, version)
      on delete restrict
);

create index if not exists ul_legal_acceptances_user_idx
    on public.ul_legal_acceptances(user_id, accepted_at desc);

-- Phase 39: persisted authenticated smoke-test results.
create table if not exists public.ul_qa_runs (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references public.ul_users(id) on delete cascade,
    status text not null check (status in ('pass', 'review', 'fail')),
    checks jsonb not null default '[]'::jsonb,
    mobile_contract_version text not null,
    backend_phase text not null,
    created_at timestamptz not null default now()
);

create index if not exists ul_qa_runs_user_created_idx
    on public.ul_qa_runs(user_id, created_at desc);

-- Phase 34: support table already exists from earlier schema. Add final metadata
-- columns idempotently for the mobile/backend final contract.
alter table if exists public.ul_support_tickets
    add column if not exists app_version text,
    add column if not exists build_number text,
    add column if not exists diagnostic_snapshot_id uuid references public.ul_diagnostic_snapshots(id) on delete set null;

-- Public Phase 37/38 runtime flags.
insert into public.ul_system_flags(key, value, description, is_public)
values
    (
        'maintenance',
        '{"enabled":false,"message":"Universal Live is available."}'::jsonb,
        'Global maintenance state consumed by mobile bootstrap.',
        true
    ),
    (
        'minimum_version',
        '{"android":"0.40.0","ios":"0.40.0","force":false}'::jsonb,
        'Minimum supported mobile versions.',
        true
    ),
    (
        'feature_flags',
        '{"streaming":true,"studio":true,"notifications":true,"billing":true,"support":true,"diagnostics":true,"legal":true}'::jsonb,
        'Public mobile feature switches.',
        true
    ),
    (
        'navigation',
        '{"home":true,"studio":true,"live":true,"activity":true,"profile":true}'::jsonb,
        'Main navigation availability.',
        true
    ),
    (
        'backend_contract',
        '{"api_version":"v1","mobile_contract_version":"2026.09-final","phase":"34-39-final"}'::jsonb,
        'Universal Live final mobile/backend compatibility contract.',
        true
    )
on conflict (key) do update
set value = excluded.value,
    description = excluded.description,
    is_public = excluded.is_public,
    updated_at = now();

-- Final legal seed. These are app-displayed baseline texts; production public
-- URLs may be updated later without a mobile rebuild.
insert into public.ul_legal_documents(
    document_key, version, title, body, public_url, required_acceptance, is_active
)
values
    (
        'privacy', '2026-09', 'Privacy Policy',
        'Universal Live processes account, destination, stream, device and diagnostic data required to operate the product, secure accounts and provide support. Authentication secrets and stream keys are not displayed in normal diagnostics or support summaries.',
        null, true, true
    ),
    (
        'terms', '2026-09', 'Terms of Service',
        'Use Universal Live only for content and destinations you are authorized to broadcast to. Destination platform terms and content rules continue to apply.',
        null, true, true
    ),
    (
        'acceptable_use', '2026-09', 'Acceptable Use',
        'Do not use Universal Live to distribute unlawful content, compromise accounts, evade destination restrictions or misuse third-party credentials.',
        null, false, true
    ),
    (
        'open_source', '2026-09', 'Open Source Notices',
        'Universal Live uses Kotlin Multiplatform, Compose, NestJS and other open-source dependencies. Release notices should be generated from the final dependency set.',
        null, false, true
    )
on conflict (document_key, version) do update
set title = excluded.title,
    body = excluded.body,
    public_url = excluded.public_url,
    required_acceptance = excluded.required_acceptance,
    is_active = excluded.is_active,
    updated_at = now();

insert into public.ul_backend_versions(api_version, mobile_contract_version, phase, status, notes)
values (
    'v1', '2026.09-final', 'backend-34-39-final', 'active',
    'Final mobile/backend phase: support, diagnostics, legal/about, global runtime state, feature flags and persisted E2E QA.'
)
on conflict (api_version, phase) do update
set mobile_contract_version = excluded.mobile_contract_version,
    status = excluded.status,
    notes = excluded.notes,
    deployed_at = now();

alter table public.ul_diagnostic_snapshots enable row level security;
alter table public.ul_legal_documents enable row level security;
alter table public.ul_legal_acceptances enable row level security;
alter table public.ul_qa_runs enable row level security;

-- Architecture is Mobile -> NestJS -> Supabase. No direct client writes.
revoke all privileges on table public.ul_diagnostic_snapshots from anon, authenticated;
revoke all privileges on table public.ul_legal_acceptances from anon, authenticated;
revoke all privileges on table public.ul_qa_runs from anon, authenticated;
revoke all privileges on table public.ul_legal_documents from anon, authenticated;

commit;
notify pgrst, 'reload schema';

-- Verification
select key, value, is_public
from public.ul_system_flags
where key in ('maintenance','minimum_version','feature_flags','navigation','backend_contract')
order by key;

select document_key, version, title, required_acceptance, is_active
from public.ul_legal_documents
where is_active = true
order by document_key;

select api_version, mobile_contract_version, phase, status
from public.ul_backend_versions
where phase = 'backend-34-39-final';
