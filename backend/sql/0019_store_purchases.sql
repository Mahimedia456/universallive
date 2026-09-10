-- Universal Live Backend Phase 16
-- Apple App Store / Google Play purchase records.
-- Server verification state only.

begin;

create table if not exists public.ul_store_purchases (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    store text not null,
    product_id text not null,
    transaction_id text,
    original_transaction_id text,
    purchase_token_hash text,
    status text not null default 'pending',
    plan_key text,
    purchased_at timestamptz,
    expires_at timestamptz,
    auto_renew boolean,
    verification_payload jsonb not null default '{}'::jsonb,
    last_verified_at timestamptz,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create unique index if not exists ul_store_purchases_store_tx_uidx
on public.ul_store_purchases(store, transaction_id)
where transaction_id is not null;

create index if not exists ul_store_purchases_user_idx
on public.ul_store_purchases(user_id, created_at desc);

alter table public.ul_store_purchases enable row level security;

drop policy if exists "ul_store_purchases_own_read" on public.ul_store_purchases;
create policy "ul_store_purchases_own_read"
on public.ul_store_purchases
for select
to authenticated
using (auth.uid() = user_id);

commit;
notify pgrst, 'reload schema';
