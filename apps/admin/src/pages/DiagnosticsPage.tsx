import {useEffect,useMemo,useState} from 'react';
import {Activity,AlertTriangle,ArrowLeft,CheckCircle2,Clock3,Gauge,RefreshCw,Signal,Video,WifiOff} from 'lucide-react';
import {consoleApi} from '../consoleApi';

const fmt=(n:any)=>Number(n||0).toLocaleString();
const age=(v?:string)=>v?new Date(v).toLocaleString():'—';
const healthClass=(s:string)=>['healthy','excellent','online','publishing','connected'].includes(String(s||'').toLowerCase())?'ok':'off';

export function DiagnosticsPage(){
 const[data,setData]=useState<any>(null),[detail,setDetail]=useState<any>(null),[busy,setBusy]=useState(false),[error,setError]=useState(''),[status,setStatus]=useState('all');
 const load=async()=>{setBusy(true);setError('');try{setData(await consoleApi.diagnostics({status:status==='all'?undefined:status}))}catch(e){setError(e instanceof Error?e.message:'Unable to load diagnostics')}finally{setBusy(false)}};
 useEffect(()=>{load()},[status]);
 const rows=useMemo(()=>data?.streams||[],[data]);

 if(detail)return <DiagnosticDetail detail={detail} close={()=>setDetail(null)} refresh={async()=>setDetail(await consoleApi.diagnosticStream(detail.session?.id))}/>;
 return <div className="modulePage">
   <div className="moduleHero">
     <div><p className="eyebrow">MODULE 08 · REAL STREAM TELEMETRY</p><h1>Stream Health & Diagnostics</h1><p>Operational view of encoder output, actual RTMP publish rate, frame delivery and destination continuity. Values shown here are measured telemetry, not configured-only numbers.</p></div>
     <button className="primarySmall" onClick={load} disabled={busy}><RefreshCw/> {busy?'Refreshing…':'Refresh health'}</button>
   </div>
   {error&&<div className="notice">{error}</div>}
   <div className="metricStrip">
    <Kpi icon={<Activity/>} label="Active sessions" value={fmt(data?.metrics?.activeStreams)} note="publisher sessions"/>
    <Kpi icon={<CheckCircle2/>} label="Healthy" value={fmt(data?.metrics?.healthyStreams)} note="within health thresholds"/>
    <Kpi icon={<AlertTriangle/>} label="Needs attention" value={fmt(data?.metrics?.warningStreams)} note="low bitrate / drops / stale"/>
    <Kpi icon={<Signal/>} label="Avg publish rate" value={`${fmt(data?.metrics?.avgBitrateKbps)} Kbps`} note="latest measured samples"/>
   </div>
   <div className="panel healthSummary">
     <div className="panelHead"><div><h3>Health thresholds</h3><p>Used only for admin diagnosis; the publisher remains the source of truth.</p></div></div>
     <div className="healthThresholds">
       <div><b>Bitrate</b><span>Warn below 65% of target</span></div>
       <div><b>FPS</b><span>Warn below 24 fps</span></div>
       <div><b>Dropped frames</b><span>Warn on material increase</span></div>
       <div><b>Freshness</b><span>Warn when last sample is stale</span></div>
     </div>
   </div>
   <div className="toolbar">
     <select value={status} onChange={e=>setStatus(e.target.value)}><option value="all">All health states</option><option value="healthy">Healthy</option><option value="warning">Needs attention</option><option value="offline">Offline / stale</option></select>
   </div>
   <div className="panel userPanel">
    <div className="panelHead"><div><h3>Publisher sessions</h3><p>Latest sample per active/recent stream</p></div></div>
    <div className="utr diagnosticRow uth"><span>Stream</span><span>Bitrate</span><span>FPS</span><span>Drops</span><span>Publish</span><span>Health</span><span></span></div>
    {rows.length===0?<div className="emptyState"><Gauge/><h3>No telemetry found</h3><p>No matching stream health samples are available.</p></div>:rows.map((r:any)=><div className="utr diagnosticRow" key={r.id}>
      <span className="userCell"><i>{String(r.title||'L')[0].toUpperCase()}</i><em><b>{r.title||'Untitled stream'}</b><small>{r.user_email||r.user_id}</small></em></span>
      <span><b>{fmt(r.telemetry?.bitrate_kbps)} Kbps</b><small>target {fmt(r.telemetry?.target_bitrate_kbps)} Kbps</small></span>
      <span><b>{r.telemetry?.fps??'—'}</b><small>{r.telemetry?.encoder_width||'—'}×{r.telemetry?.encoder_height||'—'}</small></span>
      <span><b>{fmt(r.telemetry?.dropped_frames)}</b><small>frames</small></span>
      <span><span className={'statusTag '+healthClass(r.telemetry?.publish_status)}><i/>{r.telemetry?.publish_status||'unknown'}</span><small>{age(r.telemetry?.sampled_at)}</small></span>
      <span><span className={'statusTag '+(r.health==='healthy'?'ok':'off')}><i/>{r.health}</span><small>{r.health_reason||'—'}</small></span>
      <button className="iconBtn" onClick={async()=>setDetail(await consoleApi.diagnosticStream(r.id))}><Activity/></button>
    </div>)}
   </div>
 </div>
}
function Kpi({icon,label,value,note}:{icon:any;label:string;value:string;note:string}){return <div className="miniMetric diagnosticKpi"><span className="metricIcon">{icon}</span><div><span>{label}</span><b>{value}</b><small>{note}</small></div></div>}
function DiagnosticDetail({detail,close,refresh}:{detail:any;close:()=>void;refresh:()=>void}){
 const s=detail.session||{},t=detail.latestTelemetry||{},history=detail.telemetry||[],dest=detail.destinations||[],events=detail.events||[];
 return <div className="modulePage">
  <button className="backLink" onClick={close}><ArrowLeft/> Back to stream health</button>
  <div className="moduleHero"><div><p className="eyebrow">STREAM DIAGNOSTIC DETAIL</p><h1>{s.title||'Untitled stream'}</h1><p>{s.user_email||s.user_id} · {s.status||'unknown'}</p></div><button className="secondaryBtn" onClick={refresh}><RefreshCw/> Refresh</button></div>
  <div className="metricStrip">
   <Kpi icon={<Signal/>} label="Actual bitrate" value={`${fmt(t.bitrate_kbps)} Kbps`} note={`target ${fmt(t.target_bitrate_kbps)} Kbps`}/>
   <Kpi icon={<Video/>} label="Encoded FPS" value={String(t.fps??'—')} note={`${t.encoder_width||'—'}×${t.encoder_height||'—'} ${t.encoder_name||''}`}/>
   <Kpi icon={<AlertTriangle/>} label="Dropped frames" value={fmt(t.dropped_frames)} note={`${fmt(t.published_video_frames)} video frames sent`}/>
   <Kpi icon={<Clock3/>} label="Latest sample" value={t.sampled_at?new Date(t.sampled_at).toLocaleTimeString():'—'} note={age(t.sampled_at)}/>
  </div>
  <div className="diagnosticDetailGrid">
   <section className="panel"><div className="panelHead"><div><h3>Destinations</h3><p>Per-destination publish state</p></div></div>{dest.length?dest.map((d:any)=><div className="recordLine" key={d.id}><span><b>{d.platform||d.connection_id||'Destination'}</b><small>{d.status||'unknown'} · reconnects {d.reconnect_count??0}</small></span><span className={'statusTag '+healthClass(d.status)}><i/>{d.status||'unknown'}</span></div>):<div className="emptyRow">No destination rows</div>}</section>
   <section className="panel"><div className="panelHead"><div><h3>Runtime state</h3><p>Latest publisher health fields</p></div></div>
    <div className="info"><small>Publish</small><span>{t.publish_status||'—'}</span></div><div className="info"><small>Network</small><span>{t.network_status||'—'}</span></div><div className="info"><small>Audio</small><span>{t.audio_status||'—'}</span></div><div className="info"><small>Thermal</small><span>{t.thermal_state||'—'}</span></div><div className="info"><small>Battery</small><span>{t.battery_percent??'—'}%</span></div>
   </section>
  </div>
  <div className="diagnosticDetailGrid">
   <section className="panel"><div className="panelHead"><div><h3>Recent telemetry</h3><p>Newest samples first</p></div></div>{history.slice(0,20).map((x:any)=><div className="telemetryLine" key={x.id}><span>{new Date(x.sampled_at).toLocaleTimeString()}</span><b>{fmt(x.bitrate_kbps)} Kbps</b><span>{x.fps??'—'} fps</span><span>{fmt(x.dropped_frames)} drops</span></div>)}</section>
   <section className="panel"><div className="panelHead"><div><h3>Recent events</h3><p>Reconnects, errors and publisher state changes</p></div></div>{events.length?events.slice(0,20).map((e:any)=><div className="recordLine" key={e.id}><span><b>{e.event_type}</b><small>{e.message||'No message'} · {age(e.created_at)}</small></span><span className={'severity '+String(e.severity||'info')}>{e.severity||'info'}</span></div>):<div className="emptyRow">No stream events</div>}</section>
  </div>
 </div>
}
