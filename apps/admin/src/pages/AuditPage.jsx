import { useEffect, useState } from 'react';
import { api } from '../api';

export default function AuditPage() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    api('admin-console/audit').then(setRows).catch((e) => setError(e.message));
  }, []);

  return (
    <>
      <div className="topbar">
        <div><div className="eyebrow">Security</div><h1>Admin Audit Log</h1></div>
      </div>
      {error ? <div className="error">{error}</div> : null}
      <div className="card">
        {rows.map((row) => (
          <div className="list-row" key={row.id}>
            <div>
              <strong>{row.action}</strong>
              <div className="muted">
                {row.target_type}{row.target_id ? ` · ${row.target_id}` : ''}
              </div>
            </div>
            <div className="status">
              {row.created_at ? new Date(row.created_at).toLocaleString() : ''}
            </div>
          </div>
        ))}
      </div>
    </>
  );
}
