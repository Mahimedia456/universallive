-- ============================================================================
-- Universal Live — Backend Phases 13–16 optional-safe maintenance defaults.
-- Marks obviously stale, never-started sessions as interrupted so Home restore
-- does not keep resurrecting abandoned QA attempts forever.
-- Run AFTER 0300.
-- ============================================================================

begin;

update public.ul_broadcast_sessions
set
  status = 'interrupted',
  publisher_state = 'disconnected',
  recovery_state = 'expired',
  stop_reason = coalesce(stop_reason, 'stale_before_phase13_16'),
  ended_at = coalesce(ended_at, now()),
  updated_at = now()
where status in ('created','starting','connecting','reconnecting')
  and coalesce(last_heartbeat_at, created_at) < now() - interval '24 hours';

commit;
notify pgrst, 'reload schema';

select status, count(*)
from public.ul_broadcast_sessions
group by status
order by status;
