begin;

do $$
begin
  if to_regclass('public.ul_admin_users') is not null then
    execute '
      create index if not exists ul_admin_users_active_role_idx
      on public.ul_admin_users(is_active, role)
    ';
  end if;
end
$$;

do $$
begin
  if to_regclass('public.ul_support_messages') is not null then
    execute '
      create index if not exists ul_support_messages_ticket_created_idx
      on public.ul_support_messages(ticket_id, created_at asc)
    ';
  end if;
end
$$;

do $$
begin
  if to_regclass('public.ul_stream_telemetry') is not null then
    execute '
      create index if not exists ul_stream_telemetry_admin_session_time_idx
      on public.ul_stream_telemetry(session_id, sampled_at desc)
    ';
  end if;
end
$$;

do $$
begin
  if to_regclass('public.ul_stream_events') is not null then
    execute '
      create index if not exists ul_stream_events_admin_session_time_idx
      on public.ul_stream_events(session_id, created_at desc)
    ';
  end if;
end
$$;

commit;

notify pgrst, 'reload schema';
