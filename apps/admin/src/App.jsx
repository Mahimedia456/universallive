import { useEffect, useState } from 'react';
import { api, clearToken, getToken } from './api';
import AdminShell from './components/AdminShell';
import DashboardPage from './pages/DashboardPage';
import LoginPage from './pages/LoginPage';
import CreatorsPage from './pages/CreatorsPage';
import BroadcastsPage from './pages/BroadcastsPage';
import ConnectionsPage from './pages/ConnectionsPage';
import SupportPage from './pages/SupportPage';

export default function App() {
  const [admin, setAdmin] = useState(null);
  const [checking, setChecking] = useState(Boolean(getToken()));
  const [page, setPage] = useState('dashboard');

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

  const content = {
    dashboard: <DashboardPage />,
    creators: <CreatorsPage />,
    broadcasts: <BroadcastsPage />,
    connections: <ConnectionsPage />,
    support: <SupportPage />,
  }[page] || <DashboardPage />;

  return (
    <AdminShell
      admin={admin}
      page={page}
      onPage={setPage}
      onLogout={() => {
        clearToken();
        setAdmin(null);
      }}
    >
      {content}
    </AdminShell>
  );
}
