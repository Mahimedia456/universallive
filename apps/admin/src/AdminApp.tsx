import {FormEvent,useEffect,useState} from 'react';
import {Activity,ArrowLeft,Eye,EyeOff,LockKeyhole,Mail,Radio,ShieldCheck} from 'lucide-react';
import {authApi,Admin,Session} from './api';
import {AdminShell,AdminPage} from './components/AdminShell';
import {DashboardPage} from './pages/DashboardPage';
import {UsersPage} from './pages/UsersPage';
import {CreatorsPage} from './pages/CreatorsPage';
import {StreamsPage} from './pages/StreamsPage';
import {ConnectionsPage} from './pages/ConnectionsPage';
import {DiagnosticsPage} from './pages/DiagnosticsPage';
import {AnalyticsPage} from './pages/AnalyticsPage';
import {MembershipPage} from './pages/MembershipPage';
import {BillingPage} from './pages/BillingPage';
import {NotificationsPage} from './pages/NotificationsPage';
import {SupportPage} from './pages/SupportPage';
import {ModerationPage} from './pages/ModerationPage';
import {SystemHealthPage} from './pages/SystemHealthPage';
import {FeatureFlagsPage} from './pages/FeatureFlagsPage';
import {AdminUsersPage} from './pages/AdminUsersPage';
import {AuditLogsPage} from './pages/AuditLogsPage';
import {AdminSettingsPage} from './pages/AdminSettingsPage';
import {FinalQaPage} from './pages/FinalQaPage';
import {PAGE_PERMISSION,hasPermission} from './adminPermissions';

type View='login'|'forgot'|'otp'|'reset'|'ready';
const ACCESS='ul_admin_access',REFRESH='ul_admin_refresh';

export function AdminApp(){
  const[view,setView]=useState<View>('login'),[email,setEmail]=useState(''),[password,setPassword]=useState(''),[code,setCode]=useState(''),[resetToken,setResetToken]=useState(''),[show,setShow]=useState(false),[busy,setBusy]=useState(true),[error,setError]=useState(''),[admin,setAdmin]=useState<Admin|null>(null),[page,setPage]=useState<AdminPage>('dashboard');
  useEffect(()=>{const expired=()=>logout();window.addEventListener('ul-admin-session-expired',expired);(async()=>{const a=localStorage.getItem(ACCESS),r=localStorage.getItem(REFRESH);try{if(a){const me=await authApi.me(a);setAdmin(me);setView('ready');return}if(r){save(await authApi.refresh(r));return}}catch{}finally{setBusy(false)}})();return()=>window.removeEventListener('ul-admin-session-expired',expired)},[]);
  function save(s:Session){localStorage.setItem(ACCESS,s.accessToken);localStorage.setItem(REFRESH,s.refreshToken);setAdmin(s.admin);setView('ready');setBusy(false)}
  async function run(fn:()=>Promise<void>){setError('');setBusy(true);try{await fn()}catch(e){setError(e instanceof Error?e.message:'Something went wrong')}finally{setBusy(false)}}
  const submitLogin=(e:FormEvent)=>{e.preventDefault();run(async()=>save(await authApi.login(email,password)))};
  const forgot=(e:FormEvent)=>{e.preventDefault();run(async()=>{await authApi.forgot(email);setView('otp')})};
  const verify=(e:FormEvent)=>{e.preventDefault();run(async()=>{const x=await authApi.verifyReset(email,code);setResetToken(x.resetToken);setView('reset')})};
  const reset=(e:FormEvent)=>{e.preventDefault();run(async()=>{await authApi.reset(resetToken,password);setPassword('');setView('login')})};
  async function logout(){const a=localStorage.getItem(ACCESS)||'',r=localStorage.getItem(REFRESH)||'';try{if(a&&r)await authApi.logout(a,r)}catch{}finally{localStorage.removeItem(ACCESS);localStorage.removeItem(REFRESH);setAdmin(null);setView('login');setPage('dashboard')}}
  if(busy&&view==='login'&&!error)return <div className="boot"><Radio/><span>Securing admin session…</span></div>;
  if(view==='ready'&&admin){let body:React.ReactNode;if(page==='dashboard')body=<DashboardPage openUsers={()=>setPage('users')}/>;else if(page==='users')body=<UsersPage/>;else if(page==='creators')body=<CreatorsPage/>;else if(page==='streams')body=<StreamsPage/>;else if(page==='connections')body=<ConnectionsPage/>;else if(page==='diagnostics')body=<DiagnosticsPage/>;else if(page==='analytics')body=<AnalyticsPage/>;else if(page==='membership')body=<MembershipPage/>;else if(page==='billing')body=<BillingPage/>;else if(page==='notifications')body=<NotificationsPage/>;else if(page==='support')body=<SupportPage/>;else if(page==='moderation')body=<ModerationPage/>;else if(page==='system')body=<SystemHealthPage/>;else if(page==='flags')body=<FeatureFlagsPage/>;else if(page==='admins')body=<AdminUsersPage/>;else if(page==='audit')body=<AuditLogsPage/>;else if(page==='settings')body=<AdminSettingsPage/>;else if(page==='qa')body=<FinalQaPage/>;else body=<ComingSoon name={page}/>;return <AdminShell admin={admin} page={page} onPage={setPage} onLogout={logout}>{body}</AdminShell>}
  const titles={login:['Admin Console','Sign in to manage Universal Live'],forgot:['Reset access','Enter your admin email'],otp:['Verify reset','Enter the 6-digit code sent to your email'],reset:['Create new password','Use a strong password for admin access']} as const;
  return <main className="shell"><section className="brand"><div className="logo"><span><Radio/></span>Universal Live</div><div className="brandCopy"><p className="eyebrow">CREATOR STREAMING CONTROL</p><h1>Operate every live experience from one secure command center.</h1><p>Manage creators, destinations, stream health, support, plans and platform operations with role-protected access.</p><div className="status"><Activity/><div><b>Administration gateway</b><small>Protected by session rotation and audit logging</small></div></div></div><footer>Universal Live · Enterprise Administration</footer></section><section className="auth"><div className="card">{view!=='login'&&<button className="back" onClick={()=>{setError('');setView(view==='forgot'?'login':view==='otp'?'forgot':'otp')}}><ArrowLeft/> Back</button>}<div className="lock"><LockKeyhole/></div><p className="eyebrow">SECURE ADMIN ACCESS</p><h2>{titles[view as Exclude<View,'ready'>][0]}</h2><p className="sub">{titles[view as Exclude<View,'ready'>][1]}</p>{error&&<div className="error">{error}</div>}{view==='login'&&<form onSubmit={submitLogin}><Field label="Admin email"><Mail/><input type="email" required value={email} onChange={e=>setEmail(e.target.value)} placeholder="admin@universallive.com"/></Field><Field label="Password"><LockKeyhole/><input type={show?'text':'password'} required value={password} onChange={e=>setPassword(e.target.value)} placeholder="Enter password"/><button type="button" className="eye" onClick={()=>setShow(!show)}>{show?<EyeOff/>:<Eye/>}</button></Field><button className="link" type="button" onClick={()=>setView('forgot')}>Forgot password?</button><button className="primary" disabled={busy}>{busy?'Signing in…':'Sign in securely'}</button></form>}{view==='forgot'&&<form onSubmit={forgot}><Field label="Admin email"><Mail/><input type="email" required value={email} onChange={e=>setEmail(e.target.value)}/></Field><button className="primary" disabled={busy}>Send verification code</button></form>}{view==='otp'&&<form onSubmit={verify}><label className="plain">Verification code<input className="otp" inputMode="numeric" maxLength={6} pattern="[0-9]{6}" required value={code} onChange={e=>setCode(e.target.value.replace(/\D/g,''))}/></label><button className="primary" disabled={busy}>Verify code</button></form>}{view==='reset'&&<form onSubmit={reset}><Field label="New password"><LockKeyhole/><input type={show?'text':'password'} minLength={10} required value={password} onChange={e=>setPassword(e.target.value)}/><button type="button" className="eye" onClick={()=>setShow(!show)}>{show?<EyeOff/>:<Eye/>}</button></Field><button className="primary" disabled={busy}>Reset password</button></form>}<div className="security"><ShieldCheck/> Role protected · audited · encrypted sessions</div></div></section></main>
}
function Field({label,children}:{label:string;children:React.ReactNode}){return <label className="field"><span>{label}</span><div>{children}</div></label>}
function ComingSoon({name}:{name:string}){return <div className="coming"><ShieldCheck/><p className="eyebrow">ADMIN MODULE</p><h1>{name.replace(/\b\w/g,x=>x.toUpperCase())}</h1><p>This navigation destination is reserved for a later module. No fake data is rendered.</p></div>}
