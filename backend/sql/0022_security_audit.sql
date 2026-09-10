begin;

create table if not exists public.ul_security_events (
    id bigserial primary key,
    user_id uuid references auth.users(id) on delete set null,
    event_type text not null,
    severity text not null default 'info',
    request_id text,
    route text,
    ip_hash text,
    user_agent_hash text,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists ul_security_events_user_time_idx
on public.ul_security_events(user_id, created_at desc);

create index if not exists ul_security_events_type_time_idx
on public.ul_security_events(event_type, created_at desc);

create table if not exists public.ul_rate_limit_buckets (
    bucket_key text primary key,
    window_started_at timestamptz not null,
    request_count integer not null default 0,
    blocked_until timestamptz,
    updated_at timestamptz not null default now()
);

alter table public.ul_security_events enable row level security;
alter table public.ul_rate_limit_buckets enable row level security;

commit;
notify pgrst, 'reload schema';
