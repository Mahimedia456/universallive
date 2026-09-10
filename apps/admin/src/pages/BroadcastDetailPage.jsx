import { useEffect, useMemo, useState } from 'react';
import { api } from '../api';
import {
  ErrorBanner,
  LoadingRows,
  PageHeader,
  StatCard,
} from '../components/AdminUi';

export default function BroadcastDetailPage({ id, onBack }) {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api(`admin-console/broadcasts/${id}`)
      .then(setData)
      .catch((e)=>setError(e.message));
  }, [id]);

  const summary = useMemo(() => {
    const samples = data?.telemetry || [];
    if (!samples.length) return {};

    const bitrates = samples.map((x)=>Number(x.bitrate_kbps)).filter(Number.isFinite);
    const fps = samples.map((x)=>Number(x.fps)).filter(Number.isFinite);
    const dropped = samples.map((x)=>Number(x.dropped_frames || 0)).filter(Number.isFinite);

    const avg = (items) =>
      items.length ? Math.round(items.reduce((a,b)=>a+b,0) / items.length) : null;

    return {
      avgBitrate: avg(bitrates),
      avgFps: avg(fps),
      droppedFrames: dropped.reduce((a,b)=>a+b,0),
    };
  }, [data]);

  return (
    <>
      <PageHeader
        eyebrow="Broadcast detail"
        title={data?.session?.title || 'Broadcast'}
        subtitle={data?.session?.id || id}
        action={<button className="btn btn-dark" onClick={onBack}>Back</button>}
      />

      <ErrorBanner message={error} />

      {!data ? <LoadingRows label="Loading broadcast telemetry…" /> : (
        <>
          <div className="grid">
            <StatCard label="Status" value={data.session.status} />
            <StatCard label="Destinations" value={data.destinations?.length || 0} />
            <StatCard label="Avg Bitrate" value={summary.avgBitrate ? `${summary.avgBitrate} Kbps` : '—'} />
            <StatCard label="Avg FPS" value={summary.avgFps ?? '—'} />
            <StatCard label="Dropped Frames" value={summary.droppedFrames ?? 0} />
            <StatCard label="Telemetry Samples" value={data.telemetry?.length || 0} />
          </div>

          <div className="section admin-two-col">
            <div className="card">
              <div className="section-head">
                <div><div className="eyebrow">Telemetry</div><strong>Recent samples</strong></div>
              </div>

              {(data.telemetry || []).slice(0,30).map((t,i)=>(
                <div className="list-row" key={t.id || i}>
                  <div>
                    <strong>{t.bitrate_kbps ?? '—'} Kbps</strong>
                    <div className="muted">{t.fps ?? '—'} FPS · {t.dropped_frames ?? 0} dropped</div>
                  </div>
                  <div className="status">{t.network_status || 'sample'}</div>
                </div>
              ))}
            </div>

            <div className="card">
              <div className="section-head">
                <div><div className="eyebrow">Events</div><strong>Session timeline</strong></div>
              </div>

              {(data.events || []).slice(0,30).map((e,i)=>(
                <div className="list-row" key={e.id || i}>
                  <div>
                    <strong>{e.event_type || e.type || 'event'}</strong>
                    <div className="muted">{e.created_at || ''}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </>
      )}
    </>
  );
}
