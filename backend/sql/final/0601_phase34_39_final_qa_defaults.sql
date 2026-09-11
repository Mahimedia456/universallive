-- ============================================================================
-- Universal Live — Phase 34–39 FINAL QA defaults / cleanup
-- Safe to rerun after 0600.
-- ============================================================================

begin;

-- Expire stale diagnostic snapshots instead of retaining diagnostic payloads forever.
delete from public.ul_diagnostic_snapshots
where expires_at is not null and expires_at < now();

-- Normalize invalid support status/priority values if historical rows exist.
update public.ul_support_tickets
set status = 'open'
where coalesce(status, '') not in ('open','in_progress','waiting_user','resolved','closed');

update public.ul_support_tickets
set priority = 'normal'
where coalesce(priority, '') not in ('low','normal','high','urgent');

-- Ensure final contract flag remains canonical even after older migration reruns.
insert into public.ul_system_flags(key, value, description, is_public)
values (
    'backend_contract',
    '{"api_version":"v1","mobile_contract_version":"2026.09-final","phase":"34-39-final"}'::jsonb,
    'Universal Live final mobile/backend compatibility contract.',
    true
)
on conflict (key) do update
set value = excluded.value,
    description = excluded.description,
    is_public = true,
    updated_at = now();

commit;
notify pgrst, 'reload schema';

-- Verification
select count(*) as active_legal_documents
from public.ul_legal_documents
where is_active = true;

select key, value
from public.ul_system_flags
where key = 'backend_contract';
