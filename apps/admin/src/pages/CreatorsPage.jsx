import { useEffect, useState } from 'react';
import { api } from '../api';

export default function CreatorsPage() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try {
      setRows(await api('admin-console/creators'));
    } catch (e) {
      setError(e.message);
    }
  }

  useEffect(() => { load(); }, []);

  async function changePlan(userId, planKey) {
    setError('');
    try {
      await api(`admin-console/creators/${userId}/membership`, {
        method: 'PATCH',
        body: JSON.stringify({ planKey, status: 'active' }),
      });
      await load();
    } catch (e) {
      setError(e.message);
    }
  }

  return (
    <>
      <div className="topbar">
        <div><div className="eyebrow">Accounts</div><h1>Creators & Memberships</h1></div>
      </div>
      {error ? <div className="error">{error}</div> : null}
      <div className="card">
        {rows.map((row) => (
          <div className="list-row" key={row.user_id}>
            <div>
              <strong>{row.display_name || row.username || 'Creator'}</strong>
              <div className="muted">@{row.username || 'creator'} · {row.user_id}</div>
            </div>
            <select
              className="input"
              style={{width:150}}
              value={row.entitlement?.plan_key || 'free'}
              onChange={(e) => changePlan(row.user_id, e.target.value)}
            >
              <option value="free">Free</option>
              <option value="creator">Creator</option>
              <option value="pro">Pro</option>
            </select>
          </div>
        ))}
      </div>
    </>
  );
}
