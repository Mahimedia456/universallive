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

export default function SupportPage({ onOpenSupport }) {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('all');
  const [priority, setPriority] = useState('all');
  const [page, setPage] = useState(1);

  async function load() {
    setLoading(true);
    setError('');
    try {
      setRows(await api('admin-console/support'));
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
      const matchStatus = status === 'all' || row.status === status;
      const matchPriority = priority === 'all' || row.priority === priority;
      const haystack = [
        row.subject,
        row.description,
        row.category,
        row.user_id,
        row.id,
      ].filter(Boolean).join(' ').toLowerCase();

      return matchStatus && matchPriority && (!q || haystack.includes(q));
    });
  }, [rows, query, status, priority]);

  useEffect(() => { setPage(1); }, [query, status, priority]);

  const start = (page - 1) * PAGE_SIZE;
  const visible = filtered.slice(start, start + PAGE_SIZE);

  async function update(id, patch) {
    setError('');
    try {
      await api(`admin-console/support/${id}`, {
        method:'PATCH',
        body:JSON.stringify(patch),
      });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Customer care"
        title="Support"
        subtitle={`${filtered.length} matching ticket(s)`}
        action={<button className="btn btn-dark" onClick={load}>Refresh</button>}
      />

      <ErrorBanner message={error} />

      <div className="toolbar card toolbar-three">
        <SearchInput value={query} onChange={setQuery} placeholder="Search subject, message, user ID…" />

        <select className="input toolbar-select" value={status} onChange={(e)=>setStatus(e.target.value)}>
          <option value="all">All statuses</option>
          <option value="open">Open</option>
          <option value="in_progress">In Progress</option>
          <option value="resolved">Resolved</option>
          <option value="closed">Closed</option>
        </select>

        <select className="input toolbar-select" value={priority} onChange={(e)=>setPriority(e.target.value)}>
          <option value="all">All priorities</option>
          <option value="low">Low</option>
          <option value="normal">Normal</option>
          <option value="high">High</option>
          <option value="urgent">Urgent</option>
        </select>
      </div>

      {loading ? <LoadingRows label="Loading support queue…" /> : null}

      {!loading && !visible.length ? (
        <div className="card">
          <EmptyState title="No tickets found" description="Support queue is clear for this filter." />
        </div>
      ) : null}

      {!loading && visible.length ? (
        <div className="card table-card">
          {visible.map((row)=>(
            <div className="list-row admin-list-row support-row" key={row.id}>
              <div className="row-main">
                <strong>{row.subject}</strong>
                <div className="muted">{row.category} · {row.user_id}</div>
                <div className="ticket-preview">{row.description}</div>
              </div>

              <select
                className="input compact-control"
                value={row.priority || 'normal'}
                onChange={(e)=>update(row.id,{priority:e.target.value})}
              >
                <option value="low">Low</option>
                <option value="normal">Normal</option>
                <option value="high">High</option>
                <option value="urgent">Urgent</option>
              </select>

              <select
                className="input compact-control"
                value={row.status}
                onChange={(e)=>update(row.id,{status:e.target.value})}
              >
                <option value="open">Open</option>
                <option value="in_progress">In Progress</option>
                <option value="resolved">Resolved</option>
                <option value="closed">Closed</option>
              </select>

              <button className="btn btn-dark" onClick={()=>onOpenSupport?.(row.id)}>
                Open
              </button>
            </div>
          ))}

          <Pager page={page} total={filtered.length} pageSize={PAGE_SIZE} onPage={setPage} />
        </div>
      ) : null}
    </>
  );
}
