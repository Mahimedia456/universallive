import { useEffect, useState } from 'react';
import { api } from '../api';

export default function ConnectionsPage() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try { setRows(await api('admin-console/connections')); }
    catch (e) { setError(e.message); }
  }

  useEffect(() => { load(); }, []);

  async function toggle(row) {
    try {
      await api(`admin-console/connections/${row.id}`, {
        method:'PATCH',
        body:JSON.stringify({ isEnabled: !row.is_enabled }),
      });
      await load();
    } catch (e) { setError(e.message); }
  }

  return (
    <>
      <div className="topbar"><div><div className="eyebrow">Destinations</div><h1>Connections</h1></div></div>
      {error ? <div className="error">{error}</div> : null}
      <div className="card">
        {rows.map((row) => (
          <div className="list-row" key={row.id}>
            <div>
              <strong>{row.display_name || row.platform}</strong>
              <div className="muted">{row.platform} · {row.user_id}</div>
            </div>
            <button className="btn" onClick={() => toggle(row)}>
              {row.is_enabled ? 'Disable' : 'Enable'}
            </button>
          </div>
        ))}
      </div>
    </>
  );
}
