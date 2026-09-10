import { useEffect, useState } from 'react';
import { api } from '../api';

export default function SupportPage({ onOpenSupport }) {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try { setRows(await api('admin-console/support')); }
    catch (e) { setError(e.message); }
  }

  useEffect(() => { load(); }, []);

  async function update(id, patch) {
    try {
      await api(`admin-console/support/${id}`, {
        method:'PATCH',
        body:JSON.stringify(patch),
      });
      await load();
    } catch (e) { setError(e.message); }
  }

  return (
    <>
      <div className="topbar"><div><div className="eyebrow">Customer care</div><h1>Support</h1></div></div>
      {error ? <div className="error">{error}</div> : null}
      <div className="card">
        {rows.map((row) => (
          <div className="list-row" key={row.id}>
            <div style={{flex:1}}>
              <strong>{row.subject}</strong>
              <div className="muted">{row.category} · {row.description}</div>
            </div>
            <select
              className="input"
              style={{width:140}}
              value={row.status}
              onChange={(e) => update(row.id,{status:e.target.value})}
            >
              <option value="open">Open</option>
              <option value="in_progress">In Progress</option>
              <option value="resolved">Resolved</option>
              <option value="closed">Closed</option>
            </select>
          </div>
        ))}
      </div>
    </>
  );
}
