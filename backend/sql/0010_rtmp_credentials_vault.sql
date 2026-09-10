-- Universal Live Backend Phase 07
-- Secure RTMP credential vault metadata.
-- Secret ciphertext is backend-only; mobile never receives plaintext keys
-- after initial submission except when explicitly needed by a native live-start
-- delivery contract in a later controlled integration phase.

begin;

create table if not exists public.ul_stream_credentials (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    connection_id uuid not null references public.ul_streaming_connections(id) on delete cascade,
    credential_type text not null,
    server_url_ciphertext text,
    stream_key_ciphertext text,
    oauth_access_token_ciphertext text,
    oauth_refresh_token_ciphertext text,
    token_expires_at timestamptz,
    key_version integer not null default 1,
    last_rotated_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique(connection_id, credential_type)
);

create index if not exists ul_stream_credentials_user_idx
    on public.ul_stream_credentials(user_id);

alter table public.ul_stream_credentials enable row level security;

-- No authenticated direct-read policy is intentionally created.
-- Only trusted backend/service-role access should read encrypted vault rows.

commit;
notify pgrst, 'reload schema';
