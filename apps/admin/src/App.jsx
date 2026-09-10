import { useEffect, useState } from 'react';
import { api, clearToken, getToken } from './api';
import AdminShell from './components/AdminShell';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import CreatorsPage from './pages/CreatorsPage';
import CreatorDetailPage from './pages/CreatorDetailPage';
import BroadcastsPage from './pages/BroadcastsPage';
import BroadcastDetailPage from './pages/BroadcastDetailPage';
import ConnectionsPage from './pages/ConnectionsPage';
import SupportPage from './pages/SupportPage';
import SupportDetailPage from './pages/SupportDetailPage';
import PlansPage from './pages/PlansPage';
import NotificationsAdminPage from './pages/NotificationsAdminPage';
import AuditPage from './pages/AuditPage';
import SystemPage from './pages/SystemPage';
import AdminUsersPage from './pages/AdminUsersPage';

export default function App() {
  const [admin, setAdmin] = useState(null);
  const [checking, setChecking] = useState(Boolean(getToken()));
  const [page, setPage] = useState('dashboard');
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    if (!getToken()) {
      setChecking(false);
      return;
    }

    api('admin-console/me')
      .then(setAdmin)
      .catch(() => clearToken())
      .finally(() => setChecking(false));
  }, []);

  if (checking) {
    return <div className="login-wrap"><div className="muted">Checking admin session…</div></div>;
  }

  if (!admin) {
    return <LoginPage onAuthenticated={setAdmin} />;
  }

  let content;

  if (detail?.type === 'creator') {
    content = <CreatorDetailPage userId={detail.id} onBack={() => setDetail(null)} />;
  } else if (detail?.type === 'broadcast') {
    content = <BroadcastDetailPage id={detail.id} onBack={() => setDetail(null)} />;
  } else if (detail?.type === 'support') {
    content = <SupportDetailPage id={detail.id} onBack={() => setDetail(null)} />;
  } else {
    content = {
      dashboard: <DashboardPage />,
      creators: <CreatorsPage onOpenCreator={(id)=>setDetail({type:'creator',id})} />,
      plans: <PlansPage />,
      broadcasts: <BroadcastsPage onOpenBroadcast={(id)=>setDetail({type:'broadcast',id})} />,
      connections: <ConnectionsPage />,
      notifications: <NotificationsAdminPage />,
      support: <SupportPage onOpenSupport={(id)=>setDetail({type:'support',id})} />,
      audit: <AuditPage />,
      admins: <AdminUsersPage />,
      system: <SystemPage />,
    }[page] || <DashboardPage />;
  }

  return (
    <AdminShell
      admin={admin}
      page={page}
      onPage={(next) => {
        setDetail(null);
        setPage(next);
      }}
      onLogout={() => {
        clearToken();
        setAdmin(null);
      }}
    >
      {content}
    </AdminShell>
  );
}
