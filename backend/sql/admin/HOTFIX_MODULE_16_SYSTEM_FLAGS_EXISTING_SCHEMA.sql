-- UNIVERSAL LIVE ADMIN MODULE 16 HOTFIX
-- Existing ul_system_flags schema is:
-- key, value, description, is_public, updated_at, created_at
-- There is NO "enabled" column.

select key,value,description,is_public,updated_at,created_at
from public.ul_system_flags
order by key;

-- No schema mutation required.
