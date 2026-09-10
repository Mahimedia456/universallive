begin;

create table if not exists public.ul_realtime_events (
    id bigserial primary key,
    user_id uuid references auth.users(id) on delete cascade,
    session_id uuid references public.ul_broadcast_sessions(id) on delete cascade,
    topic text not null,
    event_type text not null,
    payload jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists ul_realtime_events_user_time_idx
on public.ul_realtime_events(user_id, created_at desc);

create index if not exists ul_realtime_events_session_time_idx
on public.ul_realtime_events(session_id, created_at desc);

alter table public.ul_realtime_events enable row level security;

drop policy if exists "ul_realtime_events_own_read" on public.ul_realtime_events;
create policy "ul_realtime_events_own_read"
on public.ul_realtime_events
for select
to authenticated
using (auth.uid() = user_id);

do $$
begin
  if not exists (
    select 1
    from pg_publication_tables
    where pubname = 'supabase_realtime'
      and schemaname = 'public'
      and tablename = 'ul_realtime_events'
  ) then
    alter publication supabase_realtime add table public.ul_realtime_events;
  end if;
end $$;

insert into public.ul_system_flags(key, value, description, is_public)
values (
  'backend_final_state',
  jsonb_build_object(
    'backend_phase', 20,
    'status', 'ready-for-mobile-integration',
    'api_version', 'v1'
  ),
  'Universal Live backend final-stage compatibility marker',
  true
)
on conflict (key) do update
set value = excluded.value,
    description = excluded.description,
    is_public = excluded.is_public,
    updated_at = now();

commit;
notify pgrst, 'reload schema';
