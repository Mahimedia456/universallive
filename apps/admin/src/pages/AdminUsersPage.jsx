import { useEffect, useState } from 'react';
import { api } from '../api';

export default function AdminUsersPage() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try { setRows(await api('admin-console/admin-users')); }
    catch (e) { setError(e.message); }
  }

  useEffect(() => { load(); }, []);

  async function patch(id, data) {
    try {
      await api(`admin-console/admin-users/${id}`, {
        method:'PATCH',
        body:JSON.stringify(data),
      });
      await load();
    } catch (e) { setError(e.message); }
  }

  return (
    <>
      <div className="topbar"><div><div className="eyebrow">Security</div><h1>Admin Users & Roles</h1></div></div>
      {error ? <div className="error">{error}</div> : null}
      <div className="card">
        {rows.map((row) => (
          <div className="list-row" key={row.id}>
            <div style={{flex:1}}>
              <strong>{row.display_name || 'Administrator'}</strong>
              <div className="muted">{row.user_id}</div>
            </div>
            <select className="input" style={{width:130}} value={row.role}
              onChange={(e)=>patch(row.id,{role:e.target.value})}>
              <option value="owner">Owner</option>
              <option value="admin">Admin</option>
              <option value="support">Support</option>
              <option value="viewer">Viewer</option>
            </select>
            <button className="btn" style={{marginLeft:10}}
              onClick={()=>patch(row.id,{isActive:!row.is_active})}>
              {row.is_active ? 'Disable' : 'Enable'}
            </button>
          </div>
        ))}
      </div>
    </>
  );
}
