import { useEffect, useState } from 'react';
import { api } from '../api';

export default function NotificationsAdminPage() {
  const [rows, setRows] = useState([]);
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [audience, setAudience] = useState('all');
  const [result, setResult] = useState('');
  const [error, setError] = useState('');

  async function load() {
    try { setRows(await api('admin-console/notifications')); }
    catch (e) { setError(e.message); }
  }

  useEffect(() => { load(); }, []);

  async function send() {
    setError('');
    setResult('');
    try {
      const data = await api('admin-console/notifications/broadcast', {
        method:'POST',
        body:JSON.stringify({ title, body, audience }),
      });
      setResult(`Sent to ${data.recipients} account(s).`);
      setTitle('');
      setBody('');
      await load();
    } catch (e) { setError(e.message); }
  }

  return (
    <>
      <div className="topbar">
        <div><div className="eyebrow">Communication</div><h1>Notifications</h1></div>
      </div>

      {error ? <div className="error">{error}</div> : null}
      {result ? <div className="card" style={{marginBottom:16}}>{result}</div> : null}

      <div className="section">
        <div className="card">
          <div className="eyebrow">Broadcast notification</div>
          <label className="label">Audience</label>
          <select className="input" value={audience} onChange={(e) => setAudience(e.target.value)}>
            <option value="all">All creators</option>
            <option value="free">Free</option>
            <option value="creator">Creator</option>
            <option value="pro">Pro</option>
          </select>

          <label className="label">Title</label>
          <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} />

          <label className="label">Message</label>
          <textarea
            className="input"
            rows="5"
            value={body}
            onChange={(e) => setBody(e.target.value)}
          />

          <button
            className="btn"
            style={{marginTop:16}}
            disabled={!title.trim() || !body.trim()}
            onClick={send}
          >
            Send Notification
          </button>
        </div>

        <div className="card">
          <div className="eyebrow">Recent notifications</div>
          {rows.slice(0,50).map((row) => (
            <div className="list-row" key={row.id}>
              <div>
                <strong>{row.title}</strong>
                <div className="muted">{row.body}</div>
              </div>
              <div className="status">{row.is_read ? 'READ' : 'UNREAD'}</div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
}
