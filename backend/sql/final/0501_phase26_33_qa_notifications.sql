-- Universal Live Phases 26–33 QA defaults.
begin;

insert into public.ul_user_settings(user_id)
select id from public.ul_users
on conflict (user_id) do nothing;

insert into public.ul_notifications(user_id, type, title, body, severity, action_type, action_payload)
select
  u.id,
  'welcome',
  'Notifications are ready',
  'Universal Live in-app notifications and Android push registration are enabled for this account.',
  'info',
  'notifications',
  '{"route":"notifications"}'::jsonb
from public.ul_users u
where u.email in (
  'free.test@universallive.local',
  'creator.test@universallive.local',
  'pro.test@universallive.local'
)
and not exists (
  select 1 from public.ul_notifications n
  where n.user_id = u.id and n.type = 'welcome'
);

commit;
notify pgrst, 'reload schema';

select u.email, count(n.id) notification_count
from public.ul_users u
left join public.ul_notifications n on n.user_id=u.id
where u.email like '%.test@universallive.local'
group by u.email order by u.email;
