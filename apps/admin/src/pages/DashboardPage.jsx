import { useEffect, useState } from 'react';
import { api } from '../api';
import {
  EmptyState,
  ErrorBanner,
  LoadingRows,
  PageHeader,
  StatCard,
} from '../components/AdminUi';

export default function DashboardPage() {
  const [overview, setOverview] = useState({});
  const [users, setUsers] = useState([]);
  const [broadcasts, setBroadcasts] = useState([]);
  const [support, setSupport] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  async function load() {
    setLoading(true);
    setError('');

    try {
      const [o, u, b, s] = await Promise.all([
        api('admin-console/overview'),
        api('admin-console/recent-users'),
        api('admin-console/recent-broadcasts'),
        api('admin-console/open-support'),
      ]);

      setOverview(o || {});
      setUsers(u || []);
      setBroadcasts(b || []);
      setSupport(s || []);
    } catch (e) {
      setError(e.message || 'Dashboard failed to load');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  return (
    <>
      <PageHeader
        eyebrow="Control plane"
        title="Overview"
        subtitle="Live operational view of Universal Live."
        action={
          <button className="btn btn-dark" onClick={load}>
            Refresh
          </button>
        }
      />

      <ErrorBanner message={error} />

      <div className="grid">
        <StatCard label="Auth Users" value={overview.users} foot="Supabase accounts" />
        <StatCard label="Profiles" value={overview.profiles} foot="Creator profiles" />
        <StatCard label="Connections" value={overview.connections} foot={`${overview.activeConnections ?? 0} enabled`} />
        <StatCard label="Broadcasts" value={overview.broadcasts} foot={`${overview.liveBroadcasts ?? 0} live`} />
        <StatCard label="Scenes" value={overview.scenes} foot="Cloud scenes" />
        <StatCard label="Open Support" value={overview.supportOpen} foot="Needs attention" />
        <StatCard label="Notifications" value={overview.notifications} foot="In-app records" />
      </div>

      {loading ? <LoadingRows label="Loading operational data…" /> : null}

      {!loading ? (
        <div className="section admin-two-col">
          <div className="card">
            <div className="section-head">
              <div>
                <div className="eyebrow">Recent creators</div>
                <strong>Newest accounts</strong>
              </div>
            </div>

            {!users.length ? (
              <EmptyState title="No creators yet" description="New creator profiles will appear here." />
            ) : users.map((u) => (
              <div className="list-row" key={u.user_id}>
                <div>
                  <strong>{u.display_name || u.username || 'Creator'}</strong>
                  <div className="muted">@{u.username || 'creator'}</div>
                </div>
                <div className="status">
                  {u.onboarding_completed ? 'READY' : 'SETUP'}
                </div>
              </div>
            ))}
          </div>

          <div className="card">
            <div className="section-head">
              <div>
                <div className="eyebrow">Recent broadcasts</div>
                <strong>Latest sessions</strong>
              </div>
            </div>

            {!broadcasts.length ? (
              <EmptyState title="No broadcasts yet" description="Started stream sessions will appear here." />
            ) : broadcasts.map((b) => (
              <div className="list-row" key={b.id}>
                <div>
                  <strong>{b.title || 'Universal Live Broadcast'}</strong>
                  <div className="muted">{b.user_id}</div>
                </div>
                <div className="status">{b.status}</div>
              </div>
            ))}
          </div>

          <div className="card">
            <div className="section-head">
              <div>
                <div className="eyebrow">Support queue</div>
                <strong>Open tickets</strong>
              </div>
            </div>

            {!support.length ? (
              <EmptyState title="Queue clear" description="No open support tickets right now." />
            ) : support.map((t) => (
              <div className="list-row" key={t.id}>
                <div>
                  <strong>{t.subject}</strong>
                  <div className="muted">{t.category}</div>
                </div>
                <div className="status">{t.priority}</div>
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </>
  );
}
