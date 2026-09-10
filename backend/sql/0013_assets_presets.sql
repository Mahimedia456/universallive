-- Universal Live Backend Phase 10
-- Creator assets + cloud presets/templates metadata.

begin;

create table if not exists public.ul_creator_assets (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    asset_type text not null,
    name text not null,
    storage_bucket text not null default 'universal-live-assets',
    storage_path text not null,
    public_url text,
    mime_type text,
    file_size_bytes bigint,
    width integer,
    height integer,
    checksum_sha256 text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create index if not exists ul_creator_assets_user_idx
on public.ul_creator_assets(user_id, created_at desc);

create table if not exists public.ul_scene_presets (
    id uuid primary key default gen_random_uuid(),
    owner_user_id uuid references auth.users(id) on delete cascade,
    preset_key text,
    name text not null,
    description text,
    category text,
    is_system boolean not null default false,
    is_active boolean not null default true,
    thumbnail_url text,
    scene_payload jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists ul_scene_presets_key_uidx
on public.ul_scene_presets(preset_key)
where preset_key is not null;

alter table public.ul_creator_assets enable row level security;
alter table public.ul_scene_presets enable row level security;

drop policy if exists "ul_creator_assets_own_read" on public.ul_creator_assets;
create policy "ul_creator_assets_own_read"
on public.ul_creator_assets
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "ul_scene_presets_read" on public.ul_scene_presets;
create policy "ul_scene_presets_read"
on public.ul_scene_presets
for select
to authenticated
using (
    is_active = true
    and (
        is_system = true
        or owner_user_id = auth.uid()
    )
);

insert into public.ul_scene_presets(
    preset_key,
    name,
    description,
    category,
    is_system,
    is_active,
    scene_payload
)
values
('gaming-basic', 'Gaming', 'Gameplay-first broadcast scene.', 'gaming', true, true,
 '{"aspectRatio":"16:9","sources":[]}'::jsonb),
('talking-basic', 'Talking', 'Creator camera focused scene.', 'talking', true, true,
 '{"aspectRatio":"16:9","sources":[]}'::jsonb),
('tutorial-basic', 'Tutorial', 'Screen-first teaching layout.', 'tutorial', true, true,
 '{"aspectRatio":"16:9","sources":[]}'::jsonb),
('minimal-basic', 'Minimal', 'Clean broadcast canvas.', 'minimal', true, true,
 '{"aspectRatio":"16:9","sources":[]}'::jsonb)
on conflict (preset_key) do nothing;

commit;
notify pgrst, 'reload schema';
