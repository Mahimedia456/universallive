-- Universal Live Backend Phase 08
-- Scenes
-- Additive only.

begin;

create table if not exists public.ul_scenes (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    name text not null,
    description text,
    aspect_ratio text not null default '16:9',
    width integer not null default 1920,
    height integer not null default 1080,
    is_default boolean not null default false,
    is_archived boolean not null default false,
    sort_order integer not null default 0,
    thumbnail_url text,
    template_key text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ul_scenes_user_idx
on public.ul_scenes(user_id, sort_order, created_at);

create unique index if not exists ul_scenes_one_default_uidx
on public.ul_scenes(user_id)
where is_default = true and is_archived = false;

alter table public.ul_scenes enable row level security;

drop policy if exists "ul_scenes_own_read" on public.ul_scenes;
create policy "ul_scenes_own_read"
on public.ul_scenes
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
