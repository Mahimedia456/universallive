begin;

create index if not exists ul_admin_users_active_role_idx
  on public.ul_admin_users(is_active, role);

create index if not exists ul_support_messages_ticket_created_idx
  on public.ul_support_messages(ticket_id, created_at asc);

create index if not exists ul_stream_telemetry_session_created_idx
  on public.ul_stream_telemetry_samples(session_id, created_at desc);

commit;
