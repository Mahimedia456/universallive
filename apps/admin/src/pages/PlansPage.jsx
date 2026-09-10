import { useEffect, useState } from 'react';
import { api } from '../api';

export default function PlansPage() {
  const [rows, setRows] = useState([]);
  const [error, setError] = useState('');

  async function load() {
    try { setRows(await api('admin-console/plans')); }
    catch (e) { setError(e.message); }
  }

  useEffect(() => { load(); }, []);

  async function save(row) {
    try {
      await api(`admin-console/plans/${row.plan_key}`, {
        method: 'PATCH',
        body: JSON.stringify({
          name: row.name,
          description: row.description,
          isActive: row.is_active,
          sortOrder: row.sort_order,
          entitlements: row.entitlements,
        }),
      });
      await load();
    } catch (e) { setError(e.message); }
  }

  function patch(index, next) {
    setRows((current) =>
      current.map((item, i) => i === index ? { ...item, ...next } : item)
    );
  }

  return (
    <>
      <div className="topbar">
        <div><div className="eyebrow">Commercial</div><h1>Plans & Entitlements</h1></div>
      </div>
      {error ? <div className="error">{error}</div> : null}

      <div className="section" style={{gridTemplateColumns:'1fr'}}>
        {rows.map((row, index) => (
          <div className="card" key={row.id}>
            <div className="list-row">
              <div>
                <strong>{row.name}</strong>
                <div className="muted">{row.plan_key}</div>
              </div>
              <label className="status">
                <input
                  type="checkbox"
                  checked={row.is_active}
                  onChange={(e) => patch(index,{is_active:e.target.checked})}
                /> ACTIVE
              </label>
            </div>

            <label className="label">Description</label>
            <input
              className="input"
              value={row.description || ''}
              onChange={(e) => patch(index,{description:e.target.value})}
            />

            <div className="grid" style={{marginTop:14}}>
              <div>
                <label className="label">Max destinations</label>
                <input
                  className="input"
                  type="number"
                  min="1"
                  value={row.entitlements?.max_simultaneous_destinations ?? 1}
                  onChange={(e) => patch(index,{
                    entitlements:{
                      ...row.entitlements,
                      max_simultaneous_destinations:Number(e.target.value)
                    }
                  })}
                />
              </div>

              <div>
                <label className="label">Max resolution</label>
                <select
                  className="input"
                  value={row.entitlements?.max_resolution || '720p'}
                  onChange={(e) => patch(index,{
                    entitlements:{
                      ...row.entitlements,
                      max_resolution:e.target.value
                    }
                  })}
                >
                  <option value="720p">720p</option>
                  <option value="1080p">1080p</option>
                  <option value="1440p">1440p</option>
                  <option value="4k">4K</option>
                </select>
              </div>
            </div>

            <div style={{display:'flex',gap:16,marginTop:14,flexWrap:'wrap'}}>
              {[
                ['advanced_scenes','Advanced scenes'],
                ['advanced_overlays','Advanced overlays'],
                ['advanced_analytics','Advanced analytics'],
              ].map(([key,label]) => (
                <label key={key} className="muted">
                  <input
                    type="checkbox"
                    checked={Boolean(row.entitlements?.[key])}
                    onChange={(e) => patch(index,{
                      entitlements:{
                        ...row.entitlements,
                        [key]:e.target.checked
                      }
                    })}
                  /> {label}
                </label>
              ))}
            </div>

            <button className="btn" style={{marginTop:16}} onClick={() => save(row)}>
              Save Plan
            </button>
          </div>
        ))}
      </div>
    </>
  );
}
