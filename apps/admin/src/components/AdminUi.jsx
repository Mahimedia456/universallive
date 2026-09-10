export function PageHeader({ eyebrow, title, subtitle, action }) {
  return (
    <div className="topbar">
      <div>
        <div className="eyebrow">{eyebrow}</div>
        <h1>{title}</h1>
        {subtitle ? <div className="muted page-subtitle">{subtitle}</div> : null}
      </div>
      {action || null}
    </div>
  );
}

export function SearchInput({ value, onChange, placeholder = 'Search…' }) {
  return (
    <input
      className="input"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
    />
  );
}

export function EmptyState({ title, description }) {
  return (
    <div className="empty-state">
      <strong>{title}</strong>
      <div className="muted">{description}</div>
    </div>
  );
}

export function ErrorBanner({ message }) {
  if (!message) return null;
  return <div className="error">{message}</div>;
}

export function LoadingRows({ label = 'Loading…' }) {
  return (
    <div className="empty-state">
      <div className="admin-spinner" />
      <div className="muted">{label}</div>
    </div>
  );
}

export function Pager({ page, total, pageSize, onPage }) {
  const pages = Math.max(1, Math.ceil(total / pageSize));

  if (pages <= 1) return null;

  return (
    <div className="pager">
      <button
        className="btn btn-dark"
        disabled={page <= 1}
        onClick={() => onPage(page - 1)}
      >
        Previous
      </button>

      <div className="muted">
        Page {page} of {pages}
      </div>

      <button
        className="btn btn-dark"
        disabled={page >= pages}
        onClick={() => onPage(page + 1)}
      >
        Next
      </button>
    </div>
  );
}

export function StatCard({ label, value, foot }) {
  return (
    <div className="card metric-card">
      <div className="muted">{label}</div>
      <div className="metric">{value ?? '—'}</div>
      {foot ? <div className="metric-foot">{foot}</div> : null}
    </div>
  );
}
