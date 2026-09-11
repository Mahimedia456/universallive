-- Universal Live — Backend Phases 17–25 QA/default repair.
-- Safe to re-run after 0400/0401.

begin;

insert into public.ul_studio_workspaces(user_id, active_scene_id, quality_config_id)
select
  u.id,
  (select s.id from public.ul_scenes s where s.user_id=u.id and s.is_archived=false order by s.is_default desc, s.sort_order asc, s.created_at asc limit 1),
  (select c.id from public.ul_stream_configs c where c.user_id=u.id order by c.is_default desc, c.created_at desc limit 1)
from public.ul_users u
on conflict (user_id) do update set
  active_scene_id = coalesce(public.ul_studio_workspaces.active_scene_id, excluded.active_scene_id),
  quality_config_id = coalesce(public.ul_studio_workspaces.quality_config_id, excluded.quality_config_id),
  updated_at = now();

commit;
notify pgrst, 'reload schema';

select u.email, w.active_scene_id, w.quality_config_id, w.updated_at
from public.ul_users u
left join public.ul_studio_workspaces w on w.user_id=u.id
where u.email in ('free.test@universallive.local','creator.test@universallive.local','pro.test@universallive.local')
order by u.email;
