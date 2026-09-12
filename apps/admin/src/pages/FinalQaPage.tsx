import {useEffect,useState} from 'react';
import {Activity,CheckCircle2,RefreshCw,ShieldCheck,TriangleAlert,XCircle} from 'lucide-react';
import {consoleApi} from '../consoleApi';

export function FinalQaPage(){
 const[data,setData]=useState<any>(null),[error,setError]=useState('');
 const load=async()=>{setError('');try{setData(await consoleApi.finalQa())}catch(e){setError(e instanceof Error?e.message:'Unable to run admin QA checks')}};
 useEffect(()=>{load()},[]);
 const checks=data?.checks||[];
 const pass=checks.filter((x:any)=>x.status==='pass').length,warn=checks.filter((x:any)=>x.status==='warn').length,fail=checks.filter((x:any)=>x.status==='fail').length;
 return <div className="modulePage">
  <div className="moduleHero"><div><p className="eyebrow">MODULE 20 · FINAL INTEGRATION & QA</p><h1>Admin Readiness</h1><p>End-to-end control-center verification across auth, database contracts, operational modules, auditability and mobile-schema isolation.</p></div><button className="secondaryBtn" onClick={load}><RefreshCw/> Re-run checks</button></div>
  {error&&<div className="notice">{error}</div>}
  <div className="metricStrip"><Mini label="Passed" value={pass}/><Mini label="Warnings" value={warn}/><Mini label="Failed" value={fail}/><Mini label="Total checks" value={checks.length}/></div>
  <div className="panel qaSummary"><div className="panelHead"><div><h3>Release gate</h3><p>Admin is ready when required checks pass and warnings are understood.</p></div></div><div className={'releaseState '+(fail?'fail':warn?'warn':'pass')}>{fail?<XCircle/>:warn?<TriangleAlert/>:<CheckCircle2/>}<div><b>{fail?'Blocked':warn?'Ready with warnings':'Ready for final integration'}</b><span>{data?.generatedAt?new Date(data.generatedAt).toLocaleString():'—'}</span></div></div></div>
  <div className="qaGrid">{checks.map((x:any)=><div className={'qaCard '+x.status} key={x.key}><div className="qaIcon">{x.status==='pass'?<CheckCircle2/>:x.status==='warn'?<TriangleAlert/>:<XCircle/>}</div><div><small>{x.group}</small><h3>{x.name}</h3><p>{x.detail}</p>{x.hint&&<em>{x.hint}</em>}</div></div>)}</div>
 </div>
}
function Mini({label,value}:{label:string;value:number}){return <div className="miniMetric"><span>{label}</span><b>{Number(value||0).toLocaleString()}</b></div>}
