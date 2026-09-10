import { useEffect, useState } from 'react';
import { api } from '../api';

export default function SupportDetailPage({ id, onBack }) {
  const [data, setData] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  async function load() {
    try { setData(await api(`admin-console/support/${id}`)); }
    catch (e) { setError(e.message); }
  }

  useEffect(() => { load(); }, [id]);

  async function reply() {
    if (!message.trim()) return;
    try {
      await api(`admin-console/support/${id}/reply`, {
        method:'POST',
        body:JSON.stringify({message}),
      });
      setMessage('');
      await load();
    } catch (e) { setError(e.message); }
  }

  return (
    <>
      <div className="topbar">
        <div><div className="eyebrow">Support conversation</div><h1>{data?.ticket?.subject || 'Ticket'}</h1></div>
        <button className="btn" onClick={onBack}>Back</button>
      </div>
      {error ? <div className="error">{error}</div> : null}
      <div className="card">
        {(data?.messages || []).map((row)=>(
          <div className="list-row" key={row.id}>
            <div><strong>{row.sender_type === 'admin' ? 'Universal Live Support' : 'Creator'}</strong><div>{row.message}</div><div className="muted">{row.created_at || ''}</div></div>
          </div>
        ))}
        <label className="label">Reply</label>
        <textarea className="input" rows="4" value={message} onChange={(e)=>setMessage(e.target.value)} />
        <button className="btn" style={{marginTop:12}} onClick={reply} disabled={!message.trim()}>Send Reply</button>
      </div>
    </>
  );
}
