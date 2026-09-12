import {useEffect,useState} from 'react';
import {Activity,BarChart3,Clock3,Radio,RefreshCw,Signal,Users,Video} from 'lucide-react';
import {consoleApi} from '../consoleApi';
const fmt=(n:any)=>Number(n||0).toLocaleString();
const pct=(n:any)=>`${Number(n||0).toFixed(1)}%`;

export function AnalyticsPage(){
 const[days,setDays]=useState(30),[data,setData]=useState<any>(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
 const load=async()=>{setBusy(true);setError('');try{setData(await consoleApi.analytics({days}))}catch(e){setError(e instanceof Error?e.message:'Unable to load analytics')}finally{setBusy(false)}};
 useEffect(()=>{load()},[days]);
 const maxDay=Math.max(1,...(data?.daily||[]).map((x:any)=>Number(x.streams||0)));
 return <div className="modulePage">
  <div className="moduleHero"><div><p className="eyebrow">MODULE 09 · OPERATIONS ANALYTICS</p><h1>Analytics</h1><p>Platform-wide stream, creator, destination and membership signals derived from existing production tables.</p></div><div className="actionRow"><select className="periodSelect" value={days} onChange={e=>setDays(Number(e.target.value))}><option value={7}>Last 7 days</option><option value={30}>Last 30 days</option><option value={90}>Last 90 days</option></select><button className="secondaryBtn" onClick={load}><RefreshCw/> Refresh</button></div></div>
  {error&&<div className="notice">{error}</div>}
  <div className="metricStrip">
   <Kpi icon={<Video/>} label="Streams" value={fmt(data?.metrics?.streams)} note={`last ${days} days`}/>
   <Kpi icon={<Clock3/>} label="Live minutes" value={fmt(data?.metrics?.liveMinutes)} note="completed + ended sessions"/>
   <Kpi icon={<Signal/>} label="Avg bitrate" value={`${fmt(data?.metrics?.avgBitrateKbps)} Kbps`} note="latest telemetry samples"/>
   <Kpi icon={<Users/>} label="Active creators" value={fmt(data?.metrics?.activeCreators)} note="creators with streams"/>
  </div>
  <div className="analyticsGrid">
   <section className="panel chartPanel"><div className="panelHead"><div><h3>Streaming activity</h3><p>Broadcast sessions per day</p></div></div><div className="bars">{(data?.daily||[]).map((d:any)=><div className="barCol" key={d.date}><div className="barTrack"><i style={{height:`${Math.max(4,(Number(d.streams||0)/maxDay)*100)}%`}}/></div><b>{d.streams}</b><span>{String(d.date).slice(5)}</span></div>)}</div></section>
   <section className="panel"><div className="panelHead"><div><h3>Stream outcomes</h3><p>Status distribution</p></div></div>{(data?.statusDistribution||[]).map((x:any)=><Dist key={x.key} label={x.key} count={x.count} total={data?.metrics?.streams||1}/>)}</section>
  </div>
  <div className="analyticsGrid three">
   <section className="panel"><div className="panelHead"><div><h3>Destination platforms</h3><p>Connections configured by creators</p></div></div>{(data?.platformDistribution||[]).map((x:any)=><Dist key={x.key} label={x.key} count={x.count} total={data?.totals?.connections||1}/>)}</section>
   <section className="panel"><div className="panelHead"><div><h3>Membership mix</h3><p>Current active entitlements</p></div></div>{(data?.planDistribution||[]).map((x:any)=><Dist key={x.key} label={x.key} count={x.count} total={data?.totals?.entitlements||1}/>)}</section>
   <section className="panel"><div className="panelHead"><div><h3>Health snapshot</h3><p>Measured latest telemetry</p></div></div><div className="healthList"><Health label="Healthy samples" value={fmt(data?.health?.healthy)} icon={<Activity/>}/><Health label="Warning samples" value={fmt(data?.health?.warning)} icon={<Radio/>}/><Health label="Drop rate" value={pct(data?.health?.dropRatePercent)} icon={<BarChart3/>}/></div></section>
  </div>
  <section className="panel"><div className="panelHead"><div><h3>Top creators</h3><p>Ranked by streams in selected period</p></div></div><div className="analyticsTable"><div className="analyticsTr analyticsTh"><span>Creator</span><span>Streams</span><span>Live minutes</span><span>Last stream</span></div>{(data?.topCreators||[]).map((x:any)=><div className="analyticsTr" key={x.user_id}><span><b>{x.display_name||x.email||x.user_id}</b><small>{x.email||x.user_id}</small></span><span>{fmt(x.streams)}</span><span>{fmt(x.live_minutes)}</span><span>{x.last_stream_at?new Date(x.last_stream_at).toLocaleString():'—'}</span></div>)}</div></section>
 </div>
}
function Kpi({icon,label,value,note}:{icon:any;label:string;value:string;note:string}){return <div className="miniMetric diagnosticKpi"><span className="metricIcon">{icon}</span><div><span>{label}</span><b>{value}</b><small>{note}</small></div></div>}
function Dist({label,count,total}:{label:string;count:number;total:number}){const p=Math.min(100,Math.round((Number(count||0)/Math.max(1,Number(total||1)))*100));return <div className="distRow"><div><b>{label||'unknown'}</b><span>{fmt(count)} · {p}%</span></div><div className="distTrack"><i style={{width:`${p}%`}}/></div></div>}
function Health({label,value,icon}:{label:string;value:string;icon:any}){return <div className="health">{icon}<span><b>{label}</b><small>{value}</small></span></div>}
