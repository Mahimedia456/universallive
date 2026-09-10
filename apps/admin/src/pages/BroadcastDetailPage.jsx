import { useEffect, useState } from 'react';
import { api } from '../api';

export default function BroadcastDetailPage({ id, onBack }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api(`admin-console/broadcasts/${id}`).then(setData).catch((e)=>setError(e.message));
  }, [id]);

  return (
    <>
      <div className="topbar">
        <div><div className="eyebrow">Broadcast detail</div><h1>{data?.session?.title || 'Broadcast'}</h1></div>
        <button className="btn" onClick={onBack}>Back</button>
      </div>
      {error ? <div className="error">{error}</div> : null}
      {!data ? <div className="muted">Loading broadcast…</div> : (
        <>
          <div className="grid">
            <div className="card"><div className="muted">Status</div><div className="metric">{data.session.status}</div></div>
            <div className="card"><div className="muted">Destinations</div><div className="metric">{data.destinations?.length || 0}</div></div>
            <div className="card"><div className="muted">Telemetry</div><div className="metric">{data.telemetry?.length || 0}</div></div>
            <div className="card"><div className="muted">Events</div><div className="metric">{data.events?.length || 0}</div></div>
          </div>
          <div className="section">
            <div className="card"><div className="eyebrow">Telemetry</div>
              {(data.telemetry || []).slice(0,20).map((t,i)=><div className="list-row" key={t.id || i}><div>{t.bitrate_kbps ?? '—'} Kbps · {t.fps ?? '—'} FPS</div><div className="status">{t.network_status || 'sample'}</div></div>)}
            </div>
            <div className="card"><div className="eyebrow">Events</div>
              {(data.events || []).slice(0,20).map((e,i)=><div className="list-row" key={e.id || i}><strong>{e.event_type || e.type || 'event'}</strong><div className="muted">{e.created_at || ''}</div></div>)}
            </div>
          </div>
        </>
      )}
    </>
  );
}
