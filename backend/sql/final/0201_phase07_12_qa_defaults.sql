-- ============================================================================
-- Universal Live — Backend Phases 07–12 QA defaults
-- Enriches the three locked QA accounts with deterministic onboarding choices
-- and a default stream configuration. Safe to rerun.
-- ============================================================================

begin;

do $$
declare
  r record;
  uid uuid;
begin
  for r in
    select * from (values
      ('free.test@universallive.local',    array['Gaming']::text[], array['youtube']::text[], 'new',         'gaming'),
      ('creator.test@universallive.local', array['Gaming','IRL']::text[], array['youtube','twitch']::text[], 'some', 'multiplatform'),
      ('pro.test@universallive.local',     array['Gaming','Events']::text[], array['youtube','facebook','twitch']::text[], 'experienced', 'professional')
    ) as x(email, content_types, platforms, experience, goal)
  loop
    select id into uid
    from public.ul_users
    where lower(email) = lower(r.email)
    limit 1;

    if uid is null then
      continue;
    end if;

    insert into public.ul_onboarding_state(
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
      experience_level,
      primary_goal,
      creator_setup_completed_at,
      permission_setup_completed_at,
      updated_at
    ) values (
      uid,
      true, true, true, true, true, true, true,
      r.content_types,
      r.platforms,
      r.experience,
      r.goal,
      now(),
      now(),
      now()
    )
    on conflict (user_id) do update
    set creator_setup_completed = true,
        permission_education_completed = true,
        first_destination_prompt_completed = true,
        microphone_acknowledged = true,
        camera_acknowledged = true,
        screen_capture_acknowledged = true,
        notifications_acknowledged = true,
        creator_content_types = excluded.creator_content_types,
        preferred_platforms = excluded.preferred_platforms,
        experience_level = excluded.experience_level,
        primary_goal = excluded.primary_goal,
        creator_setup_completed_at = coalesce(public.ul_onboarding_state.creator_setup_completed_at, now()),
        permission_setup_completed_at = coalesce(public.ul_onboarding_state.permission_setup_completed_at, now()),
        updated_at = now();

    if not exists (
      select 1 from public.ul_stream_configs
      where user_id = uid and is_default = true
    ) then
      insert into public.ul_stream_configs(
        user_id,
        name,
        resolution,
        width,
        height,
        fps,
        bitrate_kbps,
        orientation,
        microphone_enabled,
        internal_audio_enabled,
        facecam_enabled,
        privacy,
        is_default,
        metadata
      ) values (
        uid,
        'Default',
        case when r.email = 'free.test@universallive.local' then '720p' else '1080p' end,
        case when r.email = 'free.test@universallive.local' then 1280 else 1920 end,
        case when r.email = 'free.test@universallive.local' then 720 else 1080 end,
        30,
        case when r.email = 'free.test@universallive.local' then 4500 else 6800 end,
        'auto',
        true,
        true,
        false,
        'public',
        true,
        '{"seeded_for":"phase07_12_qa"}'::jsonb
      );
    end if;
  end loop;
end
$$;

commit;
notify pgrst, 'reload schema';

-- Verification
select
  u.email,
  o.creator_content_types,
  o.preferred_platforms,
  o.experience_level,
  o.primary_goal,
  c.resolution,
  c.fps,
  c.bitrate_kbps
from public.ul_users u
left join public.ul_onboarding_state o on o.user_id = u.id
left join public.ul_stream_configs c on c.user_id = u.id and c.is_default = true
where u.email in (
  'free.test@universallive.local',
  'creator.test@universallive.local',
  'pro.test@universallive.local'
)
order by u.email;
