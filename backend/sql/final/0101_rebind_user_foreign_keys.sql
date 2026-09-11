-- ============================================================================
-- Universal Live — Backend Phase 00/01
-- Rebind existing public-table foreign keys from auth.users(id) to ul_users(id).
-- Safe for the locked Universal Live schema because 0100 mirrors existing
-- auth.users IDs into ul_users before this script runs.
-- ============================================================================

begin;

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
  loop
    new_definition := replace(
      r.definition,
      'REFERENCES auth.users(id)',
      'REFERENCES public.ul_users(id)'
    );

    if new_definition = r.definition then
      raise exception
        'Could not safely rewrite foreign key %.% / %: %',
        r.schema_name,
        r.table_name,
        r.constraint_name,
        r.definition;
    end if;

    execute format(
      'alter table %I.%I drop constraint %I',
      r.schema_name,
      r.table_name,
      r.constraint_name
    );

    execute format(
      'alter table %I.%I add constraint %I %s',
      r.schema_name,
      r.table_name,
      r.constraint_name,
      new_definition
    );
  end loop;
end
$$;

-- Universal Live mobile/backend architecture is API-only. Prevent anon or
-- Supabase-authenticated clients from bypassing NestJS on ul_* tables.
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
  end loop;
end
$$;

commit;
notify pgrst, 'reload schema';

-- Verification: should return zero rows after the migration.
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
  and n.nspname = 'public';
