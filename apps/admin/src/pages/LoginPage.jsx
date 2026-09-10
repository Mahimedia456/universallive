import { useState } from 'react';
import { login } from '../api';

export default function LoginPage({ onAuthenticated }) {
  const [email, setEmail] = useState('admin.test@universallive.local');
  const [password, setPassword] = useState('UniversalLive@Admin12345');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');

    try {
      const admin = await login(email.trim(), password);
      onAuthenticated(admin);
    } catch (e) {
      setError(e.message || 'Unable to sign in');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="login-wrap">
      <form className="login-card" onSubmit={submit}>
        <div className="brand">
          <div className="brand-mark">UL</div>
          <div>
            <strong>Universal <span>Live</span></strong>
            <div className="muted">Admin Console</div>
          </div>
        </div>

        <div className="eyebrow">Secure access</div>
        <h1>Admin sign in</h1>
        <p className="muted">
          Use an authorized Universal Live admin account.
        </p>

        {error ? <div className="error">{error}</div> : null}

        <label className="label">Email</label>
        <input
          className="input"
          type="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />

        <label className="label">Password</label>
        <input
          className="input"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
        />

        <button className="btn" style={{width:'100%',marginTop:18}} disabled={busy}>
          {busy ? 'Signing in…' : 'Sign In'}
        </button>
      </form>
    </div>
  );
}
