import { useEffect, useState } from 'react';
import { api } from '../api';

export default function CreatorDetailPage({ userId, onBack }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api(`admin-console/creators/${userId}`).then(setData).catch((e)=>setError(e.message));
  }, [userId]);

  return (
    <>
      <div className="topbar">
        <div><div className="eyebrow">Creator detail</div><h1>{data?.profile?.display_name || data?.profile?.username || 'Creator'}</h1></div>
        <button className="btn" onClick={onBack}>Back</button>
      </div>
      {error ? <div className="error">{error}</div> : null}
      {!data ? <p className="muted">Loading creator…</p> : (
        <>
          <div className="grid">
            <div className="card"><div className="muted">Plan</div><div className="metric">{data.entitlement?.plan_key || 'free'}</div></div>
            <div className="card"><div className="muted">Connections</div><div className="metric">{data.connections?.length || 0}</div></div>
            <div className="card"><div className="muted">Scenes</div><div className="metric">{data.scenes?.length || 0}</div></div>
            <div className="card"><div className="muted">Broadcasts</div><div className="metric">{data.broadcasts?.length || 0}</div></div>
          </div>
          <div className="section">
            <div className="card"><div className="eyebrow">Connections</div>
              {(data.connections || []).map((r)=><div className="list-row" key={r.id}><strong>{r.display_name || r.platform}</strong><div className="status">{r.status}</div></div>)}
            </div>
            <div className="card"><div className="eyebrow">Recent broadcasts</div>
              {(data.broadcasts || []).map((r)=><div className="list-row" key={r.id}><strong>{r.title || 'Broadcast'}</strong><div className="status">{r.status}</div></div>)}
            </div>
          </div>
        </>
      )}
    </>
  );
}
