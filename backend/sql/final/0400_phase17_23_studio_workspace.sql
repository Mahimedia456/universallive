-- ============================================================================
-- Universal Live — Backend Phases 17–23
-- Studio workspace, scene/editor persistence, overlays, audio, facecam and
-- quality defaults for the locked mobile UI.
-- Run AFTER 0300 and 0301.
-- ============================================================================

begin;

create table if not exists public.ul_studio_workspaces (
  user_id uuid primary key references public.ul_users(id) on delete cascade,
  active_scene_id uuid references public.ul_scenes(id) on delete set null,
  quality_config_id uuid references public.ul_stream_configs(id) on delete set null,
  audio_config jsonb not null default jsonb_build_object(
    'microphoneEnabled', true,
    'internalAudioEnabled', true,
    'microphoneGain', 1.0,
    'internalAudioGain', 1.0,
    'monitoringEnabled', false,
    'preset', 'Streaming'
  ),
  facecam_config jsonb not null default jsonb_build_object(
    'enabled', false,
    'lens', 'front',
    'shape', 'rounded',
    'size', 0.25,
    'x', 0.72,
    'y', 0.05,
    'mirror', true,
    'background', 'None'
  ),
  adaptive_bitrate_enabled boolean not null default true,
  autosave_enabled boolean not null default true,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.ul_studio_workspaces enable row level security;
revoke all privileges on table public.ul_studio_workspaces from anon, authenticated;

alter table public.ul_scene_sources
  add column if not exists source_key text,
  add column if not exists source_category text,
  add column if not exists blend_mode text not null default 'normal';

create unique index if not exists ul_scene_sources_scene_key_uidx
  on public.ul_scene_sources(scene_id, source_key)
  where source_key is not null;

alter table public.ul_stream_configs
  add column if not exists keyframe_interval_seconds integer not null default 2,
  add column if not exists audio_bitrate_kbps integer not null default 160,
  add column if not exists audio_sample_rate_hz integer not null default 48000,
  add column if not exists adaptive_bitrate_enabled boolean not null default true,
  add column if not exists audio_config jsonb not null default '{}'::jsonb,
  add column if not exists facecam_config jsonb not null default '{}'::jsonb;

-- Make sure each existing creator has a workspace without changing their scenes.
insert into public.ul_studio_workspaces(user_id, active_scene_id, quality_config_id)
select
  u.id,
  coalesce(
    (select s.id from public.ul_scenes s where s.user_id = u.id and s.is_archived = false order by s.is_default desc, s.sort_order asc, s.created_at asc limit 1),
    null
  ),
  (select c.id from public.ul_stream_configs c where c.user_id = u.id order by c.is_default desc, c.created_at desc limit 1)
from public.ul_users u
on conflict (user_id) do nothing;

commit;
notify pgrst, 'reload schema';

-- Verification
select to_regclass('public.ul_studio_workspaces') as studio_workspace_table;
select column_name, data_type
from information_schema.columns
where table_schema='public' and table_name='ul_stream_configs'
  and column_name in ('keyframe_interval_seconds','audio_bitrate_kbps','audio_sample_rate_hz','adaptive_bitrate_enabled','audio_config','facecam_config')
order by column_name;
