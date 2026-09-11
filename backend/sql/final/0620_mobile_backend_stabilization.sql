-- ============================================================================
-- Universal Live — Mobile + Backend Final Stabilization
-- 2026-09-11
-- Safe/idempotent hotfix after Phase 34–39.
--
-- Fixes:
--   * service-role access to API-only ul_* tables after anon/authenticated revoke
--   * ul_onboarding_state ownership/FK compatibility with custom ul_users auth
--   * missing onboarding rows for existing/newly migrated users
--   * stale pre-live sessions that should never hijack mobile startup
-- ============================================================================

begin;

-- The NestJS backend is the only trusted data path. anon/authenticated remain
-- blocked from direct PostgREST table access, while service_role receives the
-- SQL privileges required in addition to its RLS bypass capability.
grant usage on schema public to service_role;

do $$
declare
  r record;
begin
  for r in
    select schemaname, tablename
    from pg_tables
    where schemaname = 'public'
      and tablename like 'ul\_%' escape '\'
  loop
    execute format(
      'revoke all privileges on table %I.%I from anon, authenticated',
      r.schemaname,
      r.tablename
    );
    execute format(
      'grant select, insert, update, delete, truncate, references, trigger on table %I.%I to service_role',
      r.schemaname,
      r.tablename
    );
  end loop;
end
$$;

do $$
declare
  r record;
begin
  for r in
    select sequence_schema, sequence_name
    from information_schema.sequences
    where sequence_schema = 'public'
      and sequence_name like 'ul\_%' escape '\'
  loop
    execute format(
      'grant usage, select, update on sequence %I.%I to service_role',
      r.sequence_schema,
      r.sequence_name
    );
  end loop;
end
$$;

-- Rebind any remaining legacy ul_* foreign keys from auth.users to ul_users.
do $$
declare
  r record;
  new_definition text;
begin
  for r in
    select
      n.nspname as schema_name,
      c.relname as table_name,
      con.conname as constraint_name,
      pg_get_constraintdef(con.oid) as definition
    from pg_constraint con
    join pg_class c on c.oid = con.conrelid
    join pg_namespace n on n.oid = c.relnamespace
    where con.contype = 'f'
      and con.confrelid = 'auth.users'::regclass
      and n.nspname = 'public'
      and c.relname like 'ul\_%' escape '\'
  loop
    new_definition := replace(
      r.definition,
      'REFERENCES auth.users(id)',
      'REFERENCES public.ul_users(id)'
    );

    if new_definition <> r.definition then
      execute format('alter table %I.%I drop constraint %I', r.schema_name, r.table_name, r.constraint_name);
      execute format('alter table %I.%I add constraint %I %s', r.schema_name, r.table_name, r.constraint_name, new_definition);
    end if;
  end loop;
end
$$;

-- Ensure every custom-auth account has an onboarding state row. This keeps
-- first-login/first-create onboarding deterministic and avoids permission-flow
-- writes depending on historical seed scripts.
insert into public.ul_onboarding_state (
  user_id,
  creator_setup_completed,
  permission_education_completed,
  first_destination_prompt_completed,
  microphone_acknowledged,
  camera_acknowledged,
  screen_capture_acknowledged,
  notifications_acknowledged,
  creator_content_types,
  preferred_platforms,
  updated_at
)
select
  u.id,
  false,
  false,
  false,
  false,
  false,
  false,
  false,
  '{}'::text[],
  '{}'::text[],
  now()
from public.ul_users u
where not exists (
  select 1 from public.ul_onboarding_state o where o.user_id = u.id
)
on conflict (user_id) do nothing;

-- Clean obviously abandoned non-live sessions. Actual live/reconnecting
-- sessions are intentionally untouched here.
update public.ul_broadcast_sessions
set
  status = 'interrupted',
  publisher_state = 'disconnected',
  recovery_state = 'expired',
  ended_at = coalesce(ended_at, now()),
  stop_reason = coalesce(stop_reason, 'abandoned_before_live'),
  updated_at = now()
where status in ('created', 'starting', 'connecting')
  and created_at < now() - interval '2 minutes';

commit;

select pg_notify('pgrst', 'reload schema');

-- Verification
select
  has_table_privilege('service_role', 'public.ul_onboarding_state', 'SELECT') as service_can_select_onboarding,
  has_table_privilege('service_role', 'public.ul_onboarding_state', 'INSERT') as service_can_insert_onboarding,
  has_table_privilege('service_role', 'public.ul_onboarding_state', 'UPDATE') as service_can_update_onboarding;

select
  count(*) as users_without_onboarding
from public.ul_users u
left join public.ul_onboarding_state o on o.user_id = u.id
where o.user_id is null;

select status, count(*)
from public.ul_broadcast_sessions
group by status
order by status;
