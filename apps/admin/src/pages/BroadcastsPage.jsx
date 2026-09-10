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

export default function BroadcastsPage({ onOpenBroadcast }) {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('all');
  const [page, setPage] = useState(1);

  async function load() {
    setLoading(true);
    setError('');
    try {
      setRows(await api('admin-console/broadcasts'));
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
      const live = ['live','active','started'].includes(row.status);
      const matchStatus =
        status === 'all' ||
        (status === 'live' && live) ||
        row.status === status;

      const haystack = [
        row.title,
        row.user_id,
        row.id,
        row.status,
      ].filter(Boolean).join(' ').toLowerCase();

      return matchStatus && (!q || haystack.includes(q));
    });
  }, [rows, query, status]);

  useEffect(() => { setPage(1); }, [query, status]);

  const start = (page - 1) * PAGE_SIZE;
  const visible = filtered.slice(start, start + PAGE_SIZE);

  async function stop(id) {
    setError('');
    try {
      await api(`admin-console/broadcasts/${id}/stop`, { method:'POST' });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Streaming"
        title="Broadcasts"
        subtitle={`${filtered.length} matching session(s)`}
        action={<button className="btn btn-dark" onClick={load}>Refresh</button>}
      />

      <ErrorBanner message={error} />

      <div className="toolbar card">
        <SearchInput value={query} onChange={setQuery} placeholder="Search title, user ID, session ID…" />
        <select className="input toolbar-select" value={status} onChange={(e)=>setStatus(e.target.value)}>
          <option value="all">All statuses</option>
          <option value="live">Live now</option>
          <option value="completed">Completed</option>
          <option value="ended_by_admin">Ended by admin</option>
          <option value="failed">Failed</option>
        </select>
      </div>

      {loading ? <LoadingRows label="Loading broadcasts…" /> : null}

      {!loading && !visible.length ? (
        <div className="card">
          <EmptyState title="No broadcasts found" description="Adjust filters or wait for creator activity." />
        </div>
      ) : null}

      {!loading && visible.length ? (
        <div className="card table-card">
          {visible.map((row) => {
            const live = ['live','active','started'].includes(row.status);

            return (
              <div className="list-row admin-list-row" key={row.id}>
                <div className="row-main">
                  <strong>{row.title || 'Universal Live Broadcast'}</strong>
                  <div className="muted">{row.user_id} · {row.id}</div>
                </div>

                <div className={`status ${live ? 'status-live' : ''}`}>
                  {row.status}
                </div>

                <button className="btn btn-dark" onClick={() => onOpenBroadcast?.(row.id)}>
                  View
                </button>

                {live ? (
                  <button className="btn btn-danger" onClick={() => stop(row.id)}>
                    Stop
                  </button>
                ) : null}
              </div>
            );
          })}

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
