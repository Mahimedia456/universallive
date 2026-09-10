import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import {
  EmptyState,
  ErrorBanner,
  LoadingRows,
  PageHeader,
  SearchInput,
} from '../components/AdminUi';

export default function AdminUsersPage() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState('');

  async function load() {
    setLoading(true);
    setError('');
    try {
      setRows(await api('admin-console/admin-users'));
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return rows;

    return rows.filter((row) =>
      [row.display_name, row.user_id, row.role]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(q)
    );
  }, [rows, query]);

  async function patch(id, data) {
    setError('');
    try {
      await api(`admin-console/admin-users/${id}`, {
        method:'PATCH',
        body:JSON.stringify(data),
      });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Security"
        title="Admin Users & Roles"
        subtitle="Owner-only administration."
        action={<button className="btn btn-dark" onClick={load}>Refresh</button>}
      />

      <ErrorBanner message={error} />

      <div className="toolbar card">
        <SearchInput value={query} onChange={setQuery} placeholder="Search admin name, role, user ID…" />
      </div>

      {loading ? <LoadingRows label="Loading administrators…" /> : null}

      {!loading && !filtered.length ? (
        <div className="card">
          <EmptyState title="No admin users found" description="No account matches this search." />
        </div>
      ) : null}

      {!loading && filtered.length ? (
        <div className="card table-card">
          {filtered.map((row)=>(
            <div className="list-row admin-list-row" key={row.id}>
              <div className="row-main">
                <strong>{row.display_name || 'Administrator'}</strong>
                <div className="muted">{row.user_id}</div>
              </div>

              <select
                className="input compact-control"
                value={row.role}
                onChange={(e)=>patch(row.id,{role:e.target.value})}
              >
                <option value="owner">Owner</option>
                <option value="admin">Admin</option>
                <option value="support">Support</option>
                <option value="viewer">Viewer</option>
              </select>

              <button
                className={`btn ${row.is_active ? 'btn-danger-soft' : 'btn-dark'}`}
                onClick={()=>patch(row.id,{isActive:!row.is_active})}
              >
                {row.is_active ? 'Disable' : 'Enable'}
              </button>
            </div>
          ))}
        </div>
      ) : null}
    </>
  );
}
