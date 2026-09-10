import {
  Activity,
  CircleUserRound,
  LayoutDashboard,
  LifeBuoy,
  LogOut,
  RadioTower,
  Settings,
} from 'lucide-react';

export default function AdminShell({
  admin,
  page,
  onPage,
  onLogout,
  children,
}) {
  const nav = [
    ['dashboard', 'Dashboard', LayoutDashboard],
    ['creators', 'Creators', CircleUserRound],
    ['broadcasts', 'Broadcasts', RadioTower],
    ['connections', 'Connections', Activity],
    ['support', 'Support', LifeBuoy],
  ];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">UL</div>
          <div>
            <strong>Universal <span>Live</span></strong>
            <div className="muted">Admin Console</div>
          </div>
        </div>

        {nav.map(([key,label,Icon]) => (
          <button
            key={key}
            className={`nav-item ${page === key ? 'active' : ''}`}
            onClick={() => onPage(key)}
          >
            <Icon size={18}/> {label}
          </button>
        ))}

        <button className="nav-item">
          <Settings size={18}/> Settings
        </button>

        <div style={{marginTop:24,padding:12,borderTop:'1px solid #11232b'}}>
          <div style={{fontWeight:700}}>{admin?.displayName || 'Administrator'}</div>
          <div className="muted" style={{fontSize:12}}>{admin?.role || 'admin'}</div>
          <button className="nav-item" onClick={onLogout} style={{marginTop:8}}>
            <LogOut size={18}/> Sign Out
          </button>
        </div>
      </aside>

      <main className="main">{children}</main>
    </div>
  );
}
