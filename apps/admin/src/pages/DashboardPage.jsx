import { useEffect, useState } from 'react';
import { api } from '../api';

function Metric({ label, value }) {
  return (
    <div className="card">
      <div className="muted">{label}</div>
      <div className="metric">{value ?? '—'}</div>
    </div>
  );
}

function Rows({ items, render }) {
  if (!items?.length) return <div className="muted">No data yet.</div>;
  return items.map(render);
}

export default function DashboardPage() {
  const [overview, setOverview] = useState({});
  const [users, setUsers] = useState([]);
  const [broadcasts, setBroadcasts] = useState([]);
  const [support, setSupport] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      api('admin-console/overview'),
      api('admin-console/recent-users'),
      api('admin-console/recent-broadcasts'),
      api('admin-console/open-support'),
    ])
      .then(([o,u,b,s]) => {
        setOverview(o || {});
        setUsers(u || []);
        setBroadcasts(b || []);
        setSupport(s || []);
      })
      .catch((e) => setError(e.message || 'Dashboard failed to load'));
  }, []);

  return (
    <>
      <div className="topbar">
        <div>
          <div className="eyebrow">Control plane</div>
          <h1>Overview</h1>
        </div>
        <div className="status">API ONLINE</div>
      </div>

      {error ? <div className="error">{error}</div> : null}

      <div className="grid">
        <Metric label="Auth Users" value={overview.users} />
        <Metric label="Connections" value={overview.connections} />
        <Metric label="Broadcasts" value={overview.broadcasts} />
        <Metric label="Open Support" value={overview.supportOpen} />
      </div>

      <div className="section">
        <div className="card">
          <div className="eyebrow">Recent creators</div>
          <Rows
            items={users}
            render={(u) => (
              <div className="list-row" key={u.user_id}>
                <div>
                  <strong>{u.display_name || u.username || 'Creator'}</strong>
                  <div className="muted">@{u.username || 'creator'}</div>
                </div>
                <div className="status">
                  {u.onboarding_completed ? 'READY' : 'SETUP'}
                </div>
              </div>
            )}
          />
        </div>

        <div className="card">
          <div className="eyebrow">Recent broadcasts</div>
          <Rows
            items={broadcasts}
            render={(b) => (
              <div className="list-row" key={b.id}>
                <div>
                  <strong>{b.title || 'Universal Live Broadcast'}</strong>
                  <div className="muted">{b.user_id}</div>
                </div>
                <div className="status">{b.status}</div>
              </div>
            )}
          />
        </div>
      </div>

      <div className="section">
        <div className="card">
          <div className="eyebrow">Open support</div>
          <Rows
            items={support}
            render={(t) => (
              <div className="list-row" key={t.id}>
                <div>
                  <strong>{t.subject}</strong>
                  <div className="muted">{t.category}</div>
                </div>
                <div className="status">{t.priority}</div>
              </div>
            )}
          />
        </div>
      </div>
    </>
  );
}
