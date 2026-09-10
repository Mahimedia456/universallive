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

export default function ConnectionsPage() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [platform, setPlatform] = useState('all');
  const [enabled, setEnabled] = useState('all');
  const [page, setPage] = useState(1);

  async function load() {
    setLoading(true);
    setError('');
    try {
      setRows(await api('admin-console/connections'));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  const platforms = useMemo(
    () => Array.from(new Set(rows.map((r)=>r.platform).filter(Boolean))).sort(),
    [rows]
  );

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();

    return rows.filter((row) => {
      const matchPlatform = platform === 'all' || row.platform === platform;
      const matchEnabled =
        enabled === 'all' ||
        (enabled === 'enabled' && row.is_enabled) ||
        (enabled === 'disabled' && !row.is_enabled);

      const haystack = [
        row.display_name,
        row.platform,
        row.user_id,
        row.status,
        row.id,
      ].filter(Boolean).join(' ').toLowerCase();

      return matchPlatform && matchEnabled && (!q || haystack.includes(q));
    });
  }, [rows, query, platform, enabled]);

  useEffect(() => { setPage(1); }, [query, platform, enabled]);

  const start = (page - 1) * PAGE_SIZE;
  const visible = filtered.slice(start, start + PAGE_SIZE);

  async function toggle(row) {
    setError('');
    try {
      await api(`admin-console/connections/${row.id}`, {
        method:'PATCH',
        body:JSON.stringify({ isEnabled: !row.is_enabled }),
      });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Destinations"
        title="Connections"
        subtitle={`${filtered.length} matching destination(s)`}
        action={<button className="btn btn-dark" onClick={load}>Refresh</button>}
      />

      <ErrorBanner message={error} />

      <div className="toolbar card toolbar-three">
        <SearchInput value={query} onChange={setQuery} placeholder="Search destination, user, status…" />

        <select className="input toolbar-select" value={platform} onChange={(e)=>setPlatform(e.target.value)}>
          <option value="all">All platforms</option>
          {platforms.map((item)=><option key={item} value={item}>{item}</option>)}
        </select>

        <select className="input toolbar-select" value={enabled} onChange={(e)=>setEnabled(e.target.value)}>
          <option value="all">Enabled + disabled</option>
          <option value="enabled">Enabled</option>
          <option value="disabled">Disabled</option>
        </select>
      </div>

      {loading ? <LoadingRows label="Loading connections…" /> : null}

      {!loading && !visible.length ? (
        <div className="card">
          <EmptyState title="No connections found" description="Try a different filter." />
        </div>
      ) : null}

      {!loading && visible.length ? (
        <div className="card table-card">
          {visible.map((row)=>(
            <div className="list-row admin-list-row" key={row.id}>
              <div className="row-main">
                <strong>{row.display_name || row.platform}</strong>
                <div className="muted">{row.platform} · {row.user_id}</div>
                {row.last_error_message ? <div className="danger-text">{row.last_error_message}</div> : null}
              </div>

              <div className="status">{row.status}</div>

              <button
                className={`btn ${row.is_enabled ? 'btn-danger-soft' : 'btn-dark'}`}
                onClick={()=>toggle(row)}
              >
                {row.is_enabled ? 'Disable' : 'Enable'}
              </button>
            </div>
          ))}

          <Pager page={page} total={filtered.length} pageSize={PAGE_SIZE} onPage={setPage} />
        </div>
      ) : null}
    </>
  );
}
