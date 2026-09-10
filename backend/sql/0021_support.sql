-- Universal Live Backend Phase 18
-- Support tickets and messages.

begin;

create table if not exists public.ul_support_tickets (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    category text not null,
    subject text not null,
    description text not null,
    status text not null default 'open',
    priority text not null default 'normal',
    diagnostics jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    closed_at timestamptz
);

create index if not exists ul_support_tickets_user_idx
on public.ul_support_tickets(user_id, created_at desc);

create table if not exists public.ul_support_messages (
    id uuid primary key default gen_random_uuid(),
    ticket_id uuid not null references public.ul_support_tickets(id) on delete cascade,
    user_id uuid references auth.users(id) on delete set null,
    sender_type text not null,
    message text not null,
    metadata jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

create index if not exists ul_support_messages_ticket_idx
on public.ul_support_messages(ticket_id, created_at);

alter table public.ul_support_tickets enable row level security;
alter table public.ul_support_messages enable row level security;

drop policy if exists "ul_support_tickets_own_read" on public.ul_support_tickets;
create policy "ul_support_tickets_own_read"
on public.ul_support_tickets
for select
to authenticated
using (auth.uid() = user_id);

drop policy if exists "ul_support_messages_own_read" on public.ul_support_messages;
create policy "ul_support_messages_own_read"
on public.ul_support_messages
for select
to authenticated
using (
  exists (
    select 1
    from public.ul_support_tickets t
    where t.id = ticket_id
      and t.user_id = auth.uid()
  )
);

commit;
notify pgrst, 'reload schema';
