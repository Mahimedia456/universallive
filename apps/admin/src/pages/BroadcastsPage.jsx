import { useEffect, useState } from 'react';
import { api } from '../api';

export default function BroadcastsPage({ onOpenBroadcast }) {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try { setRows(await api('admin-console/broadcasts')); }
    catch (e) { setError(e.message); }
  }

  useEffect(() => { load(); }, []);

  async function stop(id) {
    try {
      await api(`admin-console/broadcasts/${id}/stop`, { method:'POST' });
      await load();
    } catch (e) { setError(e.message); }
  }

  return (
    <>
      <div className="topbar"><div><div className="eyebrow">Streaming</div><h1>Broadcasts</h1></div></div>
      {error ? <div className="error">{error}</div> : null}
      <div className="card">
        {rows.map((b) => (
          <div className="list-row" key={b.id}>
            <div>
              <strong>{b.title || 'Universal Live Broadcast'}</strong>
              <div className="muted">{b.user_id} · {b.status}</div>
            </div>
            <div style={{display:'flex',gap:8,alignItems:'center'}}>
              <button className="btn" onClick={() => onOpenBroadcast?.(b.id)}>View</button>
              {['live','active','started'].includes(b.status) ? (
              <button className="btn" onClick={() => stop(b.id)}>Stop</button>
            ) : <span className="status">{b.status}</span>}
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
