-- Universal Live Backend Phase 17
-- In-app notifications + push deliveries.

begin;

create table if not exists public.ul_notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    type text not null,
    title text not null,
    body text not null,
    severity text not null default 'info',
    action_type text,
    action_payload jsonb not null default '{}'::jsonb,
    read_at timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists ul_notifications_user_idx
on public.ul_notifications(user_id, created_at desc);

create index if not exists ul_notifications_unread_idx
on public.ul_notifications(user_id, read_at, created_at desc);

create table if not exists public.ul_push_deliveries (
    id uuid primary key default gen_random_uuid(),
    notification_id uuid references public.ul_notifications(id) on delete cascade,
    device_id uuid references public.ul_devices(id) on delete set null,
    user_id uuid not null references auth.users(id) on delete cascade,
    provider text,
    status text not null default 'queued',
    provider_message_id text,
    error_code text,
    error_message text,
    sent_at timestamptz,
    created_at timestamptz not null default now()
);

alter table public.ul_notifications enable row level security;
alter table public.ul_push_deliveries enable row level security;

drop policy if exists "ul_notifications_own_read" on public.ul_notifications;
create policy "ul_notifications_own_read"
on public.ul_notifications
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
