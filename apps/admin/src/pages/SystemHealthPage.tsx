import {useEffect,useState} from 'react';
import {Activity,AlertTriangle,CheckCircle2,Clock3,Database,RefreshCw,Server,ShieldCheck,Wifi} from 'lucide-react';
import {consoleApi} from '../consoleApi';
const fmt=(v:any)=>Number(v||0).toLocaleString();
export function SystemHealthPage(){
 const[data,setData]=useState<any>(null),[error,setError]=useState('');
 const load=async()=>{setError('');try{setData(await consoleApi.systemHealth())}catch(e){setError(e instanceof Error?e.message:'Unable to load system health')}};
 useEffect(()=>{load();const t=setInterval(load,30000);return()=>clearInterval(t)},[]);
 const h=data?.health||{};
 return <div className="modulePage"><div className="moduleHero"><div><p className="eyebrow">MODULE 15 · PLATFORM OPERATIONS</p><h1>System Health</h1><p>Read-only operational health built from backend/database state, recent telemetry, queue signals and configured service presence.</p></div><button className="secondaryBtn" onClick={load}><RefreshCw/> Refresh</button></div>{error&&<div className="notice">{error}</div>}
 <div className="systemHero"><div className={'healthOrb '+(h.overall==='healthy'?'healthy':h.overall==='degraded'?'warning':'down')}>{h.overall==='healthy'?<CheckCircle2/>:<AlertTriangle/>}</div><div><small>Overall platform state</small><h2>{h.overall||'Unknown'}</h2><p>Last checked {data?.checkedAt?new Date(data.checkedAt).toLocaleString():'—'}</p></div></div>
 <div className="metricStrip"><Mini icon={<Server/>} label="Backend" value={h.backend||'unknown'}/><Mini icon={<Database/>} label="Database" value={h.database||'unknown'}/><Mini icon={<Wifi/>} label="Recent telemetry" value={fmt(data?.metrics?.recentTelemetry)}/><Mini icon={<Activity/>} label="Active broadcasts" value={fmt(data?.metrics?.activeBroadcasts)}/></div>
 <div className="systemGrid">
  <section className="panel"><div className="panelHead"><div><h3>Service checks</h3><p>Configuration and runtime reachability</p></div></div>{(data?.services||[]).map((x:any)=><div className="serviceCheck" key={x.key}><span className={'serviceIcon '+x.status}>{x.status==='healthy'?<CheckCircle2/>:<AlertTriangle/>}</span><span><b>{x.name}</b><small>{x.detail}</small></span><em>{x.status}</em></div>)}</section>
  <section className="panel"><div className="panelHead"><div><h3>Operational counters</h3><p>Current database snapshot</p></div></div><Info k="Open support tickets" v={fmt(data?.metrics?.openTickets)}/><Info k="Failed push deliveries" v={fmt(data?.metrics?.failedPush)}/><Info k="Locked admins" v={fmt(data?.metrics?.lockedAdmins)}/><Info k="Enabled destinations" v={fmt(data?.metrics?.enabledConnections)}/><Info k="Recent stream errors" v={fmt(data?.metrics?.streamErrors)}/></section>
 </div>
 <section className="panel"><div className="panelHead"><div><h3>Recent backend versions</h3><p>Latest recorded backend version/deployment rows</p></div></div><div className="versionTable"><div className="versionTr versionTh"><span>Version</span><span>Environment</span><span>Commit</span><span>Recorded</span></div>{(data?.versions||[]).map((x:any)=><div className="versionTr" key={x.id}><span>{x.version||'—'}</span><span>{x.environment||'—'}</span><span>{x.commit_sha||'—'}</span><span>{new Date(x.created_at).toLocaleString()}</span></div>)}</div></section>
 </div>
}
function Mini({icon,label,value}:{icon:any;label:string;value:string}){return <div className="miniMetric diagnosticKpi"><span className="metricIcon">{icon}</span><div><span>{label}</span><b>{value}</b></div></div>}
function Info({k,v}:{k:string;v:any}){return <div className="info"><small>{k}</small><span>{v}</span></div>}
