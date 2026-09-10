import { useEffect, useState } from 'react';
import { api, API_BASE } from '../api';

export default function SystemPage() {
  const [health, setHealth] = useState(null);
  const [flags, setFlags] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try {
      const [h,f] = await Promise.all([
        api('admin-console/system/health'),
        api('admin-console/system/flags'),
      ]);
      setHealth(h);
      setFlags(f);
    } catch (e) { setError(e.message); }
  }

  useEffect(() => { load(); }, []);

  return (
    <>
      <div className="topbar">
        <div><div className="eyebrow">Operations</div><h1>System</h1></div>
        <div className="status">{health?.ok ? 'ONLINE' : 'CHECKING'}</div>
      </div>

      {error ? <div className="error">{error}</div> : null}

      <div className="grid">
        <div className="card"><div className="muted">API</div><div className="metric">{health?.backend || '—'}</div></div>
        <div className="card"><div className="muted">Database</div><div className="metric" style={{fontSize:18}}>{health?.database || '—'}</div></div>
        <div className="card"><div className="muted">Broadcasts</div><div className="metric">{health?.overview?.broadcasts ?? '—'}</div></div>
        <div className="card"><div className="muted">Open Support</div><div className="metric">{health?.overview?.supportOpen ?? '—'}</div></div>
      </div>

      <div className="section">
        <div className="card">
          <div className="eyebrow">Backend endpoint</div>
          <p>{API_BASE}</p>
          <div className="muted">Checked: {health?.checkedAt || '—'}</div>
        </div>

        <div className="card">
          <div className="eyebrow">System flags</div>
          {flags.length ? flags.map((flag,index) => (
            <div className="list-row" key={flag.id || flag.flag_key || index}>
              <strong>{flag.key || 'flag'}</strong>
              <div className="status">{flag.is_public ? 'PUBLIC' : 'PRIVATE'}</div>
            </div>
          )) : <div className="muted">No system flags returned.</div>}
        </div>
      </div>
    </>
  );
}
