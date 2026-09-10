-- Universal Live Backend Phase 09
-- Scene sources and layer ordering.

begin;

create table if not exists public.ul_scene_sources (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    scene_id uuid not null references public.ul_scenes(id) on delete cascade,
    source_type text not null,
    name text not null,
    z_index integer not null default 0,
    is_visible boolean not null default true,
    is_locked boolean not null default false,
    x numeric(8,5) not null default 0,
    y numeric(8,5) not null default 0,
    width numeric(8,5) not null default 1,
    height numeric(8,5) not null default 1,
    rotation numeric(8,3) not null default 0,
    opacity numeric(6,5) not null default 1,
    config jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ul_scene_sources_scene_idx
on public.ul_scene_sources(scene_id, z_index);

create index if not exists ul_scene_sources_user_idx
on public.ul_scene_sources(user_id, created_at);

alter table public.ul_scene_sources enable row level security;

drop policy if exists "ul_scene_sources_own_read" on public.ul_scene_sources;
create policy "ul_scene_sources_own_read"
on public.ul_scene_sources
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
