-- ============================================================================
-- Universal Live — ONE-TIME SUPER ADMIN SEED TEMPLATE
-- 2026-09-12
--
-- IMPORTANT:
--   1) Replace BOTH values below before running.
--   2) Use a strong password (12+ chars recommended).
--   3) Do not commit your edited password into Git.
-- ============================================================================

create extension if not exists pgcrypto;

do $$
declare
  v_email text := 'CHANGE_ME@example.com';
  v_password text := 'CHANGE_ME_STRONG_PASSWORD';
begin
  if v_email like 'CHANGE_ME%' or v_password like 'CHANGE_ME%' then
    raise exception 'Replace v_email and v_password before running this seed.';
  end if;

  if length(v_password) < 10 then
    raise exception 'Admin password must contain at least 10 characters.';
  end if;

  insert into public.ul_admin_users (
    email,
    password_hash,
    display_name,
    role,
    permissions,
    is_active,
    failed_login_count,
    locked_until,
    updated_at
  )
  values (
    lower(trim(v_email)),
    crypt(v_password, gen_salt('bf', 12)),
    'Universal Live Admin',
    'SUPER_ADMIN',
    '["*"]'::jsonb,
    true,
    0,
    null,
    now()
  )
  on conflict (email) do update set
    password_hash = excluded.password_hash,
    role = 'SUPER_ADMIN',
    permissions = '["*"]'::jsonb,
    is_active = true,
    failed_login_count = 0,
    locked_until = null,
    updated_at = now();
end
$$;

select id, email, display_name, role, is_active, locked_until
from public.ul_admin_users
order by created_at desc;
