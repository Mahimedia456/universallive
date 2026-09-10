import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import {
  EmptyState,
  ErrorBanner,
  LoadingRows,
  PageHeader,
  Pager,
  SearchInput,
} from '../components/AdminUi';

const PAGE_SIZE = 12;

export default function CreatorsPage({ onOpenCreator }) {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [plan, setPlan] = useState('all');
  const [page, setPage] = useState(1);

  async function load() {
    setLoading(true);
    setError('');
    try {
      setRows(await api('admin-console/creators'));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();

    return rows.filter((row) => {
      const planKey = row.entitlement?.plan_key || 'free';
      const matchPlan = plan === 'all' || planKey === plan;
      const haystack = [
        row.display_name,
        row.username,
        row.user_id,
        row.creator_type,
      ].filter(Boolean).join(' ').toLowerCase();

      return matchPlan && (!q || haystack.includes(q));
    });
  }, [rows, query, plan]);

  useEffect(() => { setPage(1); }, [query, plan]);

  const start = (page - 1) * PAGE_SIZE;
  const visible = filtered.slice(start, start + PAGE_SIZE);

  async function changePlan(userId, planKey) {
    setError('');
    try {
      await api(`admin-console/creators/${userId}/membership`, {
        method: 'PATCH',
        body: JSON.stringify({ planKey, status: 'active' }),
      });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Accounts"
        title="Creators & Memberships"
        subtitle={`${filtered.length} matching account(s)`}
        action={<button className="btn btn-dark" onClick={load}>Refresh</button>}
      />

      <ErrorBanner message={error} />

      <div className="toolbar card">
        <SearchInput
          value={query}
          onChange={setQuery}
          placeholder="Search name, username, user ID…"
        />

        <select className="input toolbar-select" value={plan} onChange={(e) => setPlan(e.target.value)}>
          <option value="all">All plans</option>
          <option value="free">Free</option>
          <option value="creator">Creator</option>
          <option value="pro">Pro</option>
        </select>
      </div>

      {loading ? <LoadingRows label="Loading creators…" /> : null}

      {!loading && !visible.length ? (
        <div className="card">
          <EmptyState title="No creators found" description="Try a different search or plan filter." />
        </div>
      ) : null}

      {!loading && visible.length ? (
        <div className="card table-card">
          {visible.map((row) => (
            <div className="list-row admin-list-row" key={row.user_id}>
              <div className="row-main">
                <strong>{row.display_name || row.username || 'Creator'}</strong>
                <div className="muted">@{row.username || 'creator'} · {row.user_id}</div>
              </div>

              <select
                className="input compact-control"
                value={row.entitlement?.plan_key || 'free'}
                onChange={(e) => changePlan(row.user_id, e.target.value)}
              >
                <option value="free">Free</option>
                <option value="creator">Creator</option>
                <option value="pro">Pro</option>
              </select>

              <button className="btn btn-dark" onClick={() => onOpenCreator?.(row.user_id)}>
                View
              </button>
            </div>
          ))}

          <Pager
            page={page}
            total={filtered.length}
            pageSize={PAGE_SIZE}
            onPage={setPage}
          />
        </div>
      ) : null}
    </>
  );
}
