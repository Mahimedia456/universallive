import {useEffect,useState} from 'react';
import {RefreshCw,Search,ShieldCheck} from 'lucide-react';
import {consoleApi} from '../consoleApi';

export function AuditLogsPage(){
 const[data,setData]=useState<any>(null),[search,setSearch]=useState(''),[action,setAction]=useState(''),[error,setError]=useState('');
 const load=async()=>{setError('');try{setData(await consoleApi.auditLogs({search,action:action||undefined}))}catch(e){setError(e instanceof Error?e.message:'Unable to load audit logs')}};
 useEffect(()=>{load()},[]);
 return <div className="modulePage"><div className="moduleHero"><div><p className="eyebrow">MODULE 18 · SECURITY TRACEABILITY</p><h1>Audit Logs</h1><p>Immutable operational view of admin authentication, user management, moderation, billing, notifications, support, plans and system configuration actions.</p></div><button className="secondaryBtn" onClick={load}><RefreshCw/> Refresh</button></div>{error&&<div className="notice">{error}</div>}
 <div className="metricStrip"><Mini label="Rows returned" value={data?.metrics?.total||0}/><Mini label="Auth events" value={data?.metrics?.auth||0}/><Mini label="Write actions" value={data?.metrics?.writes||0}/><Mini label="Unique admins" value={data?.metrics?.admins||0}/></div>
 <div className="toolbar supportToolbar"><label className="tableSearch"><Search/><input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search admin, action, target…"/><button className="textBtn" onClick={load}>Search</button></label><input className="auditActionFilter" value={action} onChange={e=>setAction(e.target.value)} placeholder="Action prefix / exact action"/><button className="secondaryBtn" onClick={load}>Apply</button></div>
 <div className="panel auditPanel"><div className="auditRow auditHead"><span>Time</span><span>Administrator</span><span>Action</span><span>Target</span><span>Details</span></div>{(data?.items||[]).length===0?<div className="emptyState"><ShieldCheck/><h3>No audit rows</h3><p>No matching administrative events.</p></div>:(data.items||[]).map((x:any)=><div className="auditRow" key={x.id}><span>{new Date(x.created_at).toLocaleString()}</span><span><b>{x.admin_name||'System / deleted admin'}</b><small>{x.admin_email||x.admin_user_id||'—'}</small></span><span><code>{x.action}</code></span><span><b>{x.target_type||'—'}</b><small>{x.target_id||'—'}</small></span><details><summary>View</summary><pre>{JSON.stringify(x.details||{},null,2)}</pre></details></div>)}</div>
 </div>
}
function Mini({label,value}:{label:string;value:number}){return <div className="miniMetric"><span>{label}</span><b>{Number(value||0).toLocaleString()}</b></div>}
