-- ============================================================================
-- Universal Live — Backend Phase 00/01 QA accounts
-- Requested local/QA accounts. Password for all three: 1231231234
-- Remove or rotate these accounts before a public production launch.
-- Requires 0100 + 0101 and the locked ul_creator_profiles / ul_plans /
-- ul_user_entitlements / ul_onboarding_state schema already present.
-- ============================================================================

begin;

-- Ensure the three locked plan definitions are present.
insert into public.ul_plans(plan_key, name, description, sort_order, entitlements)
values
(
  'free', 'Free', 'Core Universal Live broadcasting.', 10,
  '{
    "max_simultaneous_destinations": 1,
    "max_resolution": "720p",
    "advanced_scenes": false,
    "advanced_overlays": false,
    "advanced_analytics": false
  }'::jsonb
),
(
  'creator', 'Creator', 'Professional creator streaming tools.', 20,
  '{
    "max_simultaneous_destinations": 3,
    "max_resolution": "1080p",
    "advanced_scenes": true,
    "advanced_overlays": true,
    "advanced_analytics": true
  }'::jsonb
),
(
  'pro', 'Pro', 'Expanded production and analytics limits.', 30,
  '{
    "max_simultaneous_destinations": 5,
    "max_resolution": "1080p",
    "advanced_scenes": true,
    "advanced_overlays": true,
    "advanced_analytics": true
  }'::jsonb
)
on conflict (plan_key) do update
set name = excluded.name,
    description = excluded.description,
    sort_order = excluded.sort_order,
    entitlements = excluded.entitlements,
    is_active = true,
    updated_at = now();

do $$
declare
  uid uuid;
  account record;
begin
  for account in
    select * from (values
      ('free.test@universallive.local',    'Universal Free Test',    'free_test',    'free'),
      ('creator.test@universallive.local', 'Universal Creator Test', 'creator_test', 'creator'),
      ('pro.test@universallive.local',     'Universal Pro Test',     'pro_test',     'pro')
    ) as x(email, full_name, username, plan_key)
  loop
    select id into uid
    from public.ul_users
    where lower(email) = lower(account.email)
    limit 1;

    if uid is null then
      uid := gen_random_uuid();
      insert into public.ul_users(
        id,
        email,
        password_hash,
        full_name,
        username,
        email_verified_at,
        is_active,
        failed_login_attempts,
        locked_until
      ) values (
        uid,
        lower(account.email),
        crypt('1231231234', gen_salt('bf', 12)),
        account.full_name,
        account.username,
        now(),
        true,
        0,
        null
      );
    else
      update public.ul_users
      set password_hash = crypt('1231231234', gen_salt('bf', 12)),
          full_name = account.full_name,
          username = account.username,
          email_verified_at = coalesce(email_verified_at, now()),
          is_active = true,
          failed_login_attempts = 0,
          locked_until = null,
          password_changed_at = now(),
          updated_at = now()
      where id = uid;
    end if;

    insert into public.ul_creator_profiles(
      user_id,
      display_name,
      username,
      onboarding_completed,
      updated_at
    ) values (
      uid,
      account.full_name,
      account.username,
      true,
      now()
    )
    on conflict (user_id) do update
    set display_name = excluded.display_name,
        username = excluded.username,
        onboarding_completed = true,
        updated_at = now();

    insert into public.ul_user_entitlements(
      user_id,
      plan_key,
      status,
      source,
      starts_at,
      updated_at
    ) values (
      uid,
      account.plan_key,
      'active',
      'qa-seed',
      now(),
      now()
    )
    on conflict (user_id) do update
    set plan_key = excluded.plan_key,
        status = 'active',
        source = 'qa-seed',
        starts_at = coalesce(public.ul_user_entitlements.starts_at, now()),
        expires_at = null,
        grace_until = null,
        updated_at = now();

    insert into public.ul_onboarding_state(
      user_id,
      creator_setup_completed,
      permission_education_completed,
      first_destination_prompt_completed,
      microphone_acknowledged,
      camera_acknowledged,
      screen_capture_acknowledged,
      notifications_acknowledged,
      updated_at
    ) values (
      uid, true, true, true, true, true, true, true, now()
    )
    on conflict (user_id) do update
    set creator_setup_completed = true,
        permission_education_completed = true,
        first_destination_prompt_completed = true,
        microphone_acknowledged = true,
        camera_acknowledged = true,
        screen_capture_acknowledged = true,
        notifications_acknowledged = true,
        updated_at = now();
  end loop;
end
$$;

-- Public splash/bootstrap defaults used by GET /api/v1/app/bootstrap.
insert into public.ul_system_flags(key, value, description, is_public)
values
  ('maintenance', '{"enabled": false}'::jsonb, 'Mobile maintenance gate.', true),
  ('minimum_version', '{"android": null, "ios": null}'::jsonb, 'Minimum supported mobile app versions.', true)
on conflict (key) do update
set value = excluded.value,
    description = excluded.description,
    is_public = true,
    updated_at = now();

commit;
notify pgrst, 'reload schema';

-- Verification
select
  u.email,
  u.email_verified_at,
  u.is_active,
  e.plan_key,
  e.status
from public.ul_users u
left join public.ul_user_entitlements e on e.user_id = u.id
where u.email in (
  'free.test@universallive.local',
  'creator.test@universallive.local',
  'pro.test@universallive.local'
)
order by u.email;
