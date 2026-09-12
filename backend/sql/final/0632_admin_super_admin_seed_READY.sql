-- Universal Live — READY ONE-TIME SUPER ADMIN SEED
-- Run 0630_admin_runtime_hardening.sql first.
-- Login after running:
--   Email:    admin@universallive.local
--   Password: ULive#OIcQR%eU3ZqRJL2JJV
-- Change the password after first successful production login.
-- Do NOT run legacy backend/sql/0024_admin_console.sql.

create extension if not exists pgcrypto;

do $$
declare
  v_email text := 'admin@universallive.local';
  v_password text := '1231231234';
begin
  if length(v_password) < 12 then
    raise exception 'Admin password must contain at least 12 characters.';
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
    display_name = excluded.display_name,
    role = 'SUPER_ADMIN',
    permissions = '["*"]'::jsonb,
    is_active = true,
    failed_login_count = 0,
    locked_until = null,
    updated_at = now();

  delete from public.ul_admin_refresh_tokens
  where admin_user_id = (
    select id
    from public.ul_admin_users
    where email = lower(trim(v_email))
    limit 1
  );
end
$$;

select id,email,display_name,role,is_active,failed_login_count,locked_until,last_login_at,created_at,updated_at
from public.ul_admin_users
where email = 'admin@universallive.local';
