import type { Admin } from './api';

const API=(import.meta.env.VITE_API_BASE_URL||'http://localhost:3000/api/v1').replace(/\/$/,'');
const ACCESS='ul_admin_access';

async function call<T>(path:string,init:RequestInit={}){
  const token=localStorage.getItem(ACCESS)||'';
  const r=await fetch(`${API}${path}`,{
    ...init,
    headers:{
      'Content-Type':'application/json',
      Authorization:`Bearer ${token}`,
      ...(init.headers||{})
    }
  });
  const data=await r.json().catch(()=>({}));
  if(r.status===401)window.dispatchEvent(new Event('ul-admin-session-expired'));
  if(!r.ok)throw new Error(Array.isArray(data.message)?data.message.join(', '):data.message||'Request failed');
  return data as T;
}

const qs=(p:Record<string,string|number|boolean|undefined|null>)=>
  new URLSearchParams(
    Object.entries(p)
      .filter(([,v])=>v!==''&&v!==undefined&&v!==null)
      .map(([k,v])=>[k,String(v)])
  ).toString();

export type DashboardData={
  metrics:Record<string,number>;
  recentUsers:any[];
  recentStreams:any[];
  generatedAt:string
};

export type UserList={
  items:any[];
  page:number;
  limit:number;
  total:number;
  pages:number
};

export const consoleApi={
  // 03 Dashboard
  dashboard:()=>call<DashboardData>('/admin/dashboard'),

  // 04 Users
  users:(params:Record<string,string|number|boolean|undefined>={})=>call<UserList>('/admin/users?'+qs(params)),
  user:(id:string)=>call<any>(`/admin/users/${id}`),
  createUser:(body:any)=>call<any>('/admin/users',{method:'POST',body:JSON.stringify(body)}),
  updateUser:(id:string,body:any)=>call<any>(`/admin/users/${id}`,{method:'PATCH',body:JSON.stringify(body)}),
  setStatus:(id:string,isActive:boolean)=>call<any>(`/admin/users/${id}/status`,{method:'POST',body:JSON.stringify({isActive})}),
  verifyEmail:(id:string)=>call<any>(`/admin/users/${id}/verify-email`,{method:'POST'}),
  deleteUser:(id:string,hard=false)=>call<any>(`/admin/users/${id}${hard?'?mode=hard':''}`,{method:'DELETE'}),

  // 05 Creators
  creators:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/creators?'+qs(params)),
  creator:(id:string)=>call<any>(`/admin/creators/${id}`),
  createCreator:(body:any)=>call<any>('/admin/creators',{method:'POST',body:JSON.stringify(body)}),
  updateCreator:(id:string,body:any)=>call<any>(`/admin/creators/${id}`,{method:'PATCH',body:JSON.stringify(body)}),

  // 06 Streams
  streams:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/streams?'+qs(params)),
  stream:(id:string)=>call<any>(`/admin/streams/${id}`),
  createStream:(body:any)=>call<any>('/admin/streams',{method:'POST',body:JSON.stringify(body)}),
  updateStream:(id:string,body:any)=>call<any>(`/admin/streams/${id}`,{method:'PATCH',body:JSON.stringify(body)}),
  endStream:(id:string)=>call<any>(`/admin/streams/${id}/end`,{method:'POST'}),

  // 07 Connections
  connections:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/connections?'+qs(params)),
  connection:(id:string)=>call<any>(`/admin/connections/${id}`),
  createConnection:(body:any)=>call<any>('/admin/connections',{method:'POST',body:JSON.stringify(body)}),
  updateConnection:(id:string,body:any)=>call<any>(`/admin/connections/${id}`,{method:'PATCH',body:JSON.stringify(body)}),
  setConnectionEnabled:(id:string,isEnabled:boolean)=>call<any>(`/admin/connections/${id}/enabled`,{method:'POST',body:JSON.stringify({isEnabled})}),

  // 08 Diagnostics
  diagnostics:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/diagnostics?'+qs(params)),
  diagnosticStream:(id:string)=>call<any>(`/admin/diagnostics/streams/${id}`),

  // 09 Analytics
  analytics:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/analytics?'+qs(params)),

  // 10 Plans / Membership
  plans:()=>call<any>('/admin/plans'),
  plan:(key:string)=>call<any>(`/admin/plans/${encodeURIComponent(key)}`),
  createPlan:(body:any)=>call<any>('/admin/plans',{method:'POST',body:JSON.stringify(body)}),
  updatePlan:(key:string,body:any)=>call<any>(`/admin/plans/${encodeURIComponent(key)}`,{method:'PATCH',body:JSON.stringify(body)}),
  setPlanActive:(key:string,isActive:boolean)=>call<any>(`/admin/plans/${encodeURIComponent(key)}/active`,{method:'POST',body:JSON.stringify({isActive})}),
  entitlements:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/entitlements?'+qs(params)),
  entitlement:(userId:string)=>call<any>(`/admin/entitlements/${userId}`),
  updateEntitlement:(userId:string,body:any)=>call<any>(`/admin/entitlements/${userId}`,{method:'PATCH',body:JSON.stringify(body)}),

  // 11 Billing
  billing:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/billing?'+qs(params)),
  purchase:(id:string)=>call<any>(`/admin/billing/${id}`),
  updatePurchase:(id:string,body:any)=>call<any>(`/admin/billing/${id}`,{method:'PATCH',body:JSON.stringify(body)}),

  // 12 Notifications
  notifications:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/notifications?'+qs(params)),
  notification:(id:string)=>call<any>(`/admin/notifications/${id}`),
  sendNotification:(body:any)=>call<any>('/admin/notifications',{method:'POST',body:JSON.stringify(body)}),
  deleteNotification:(id:string)=>call<any>(`/admin/notifications/${id}`,{method:'DELETE'}),

  // 13 Support
  supportTickets:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/support?'+qs(params)),
  supportTicket:(id:string)=>call<any>(`/admin/support/${id}`),
  createSupportTicket:(body:any)=>call<any>('/admin/support',{method:'POST',body:JSON.stringify(body)}),
  updateSupportTicket:(id:string,body:any)=>call<any>(`/admin/support/${id}`,{method:'PATCH',body:JSON.stringify(body)}),
  replySupportTicket:(id:string,message:string)=>call<any>(`/admin/support/${id}/reply`,{method:'POST',body:JSON.stringify({message})}),

  // 14 Moderation
  moderation:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/moderation?'+qs(params)),
  moderationCase:(id:string)=>call<any>(`/admin/moderation/${id}`),
  createModerationCase:(body:any)=>call<any>('/admin/moderation',{method:'POST',body:JSON.stringify(body)}),
  updateModerationCase:(id:string,body:any)=>call<any>(`/admin/moderation/${id}`,{method:'PATCH',body:JSON.stringify(body)}),

  // 15 System Health
  systemHealth:()=>call<any>('/admin/system-health'),

  // 16 Feature Flags
  systemFlags:()=>call<any>('/admin/system-flags'),
  systemFlag:(key:string)=>call<any>(`/admin/system-flags/${encodeURIComponent(key)}`),
  createSystemFlag:(body:any)=>call<any>('/admin/system-flags',{method:'POST',body:JSON.stringify(body)}),
  updateSystemFlag:(key:string,body:any)=>call<any>(`/admin/system-flags/${encodeURIComponent(key)}`,{method:'PATCH',body:JSON.stringify(body)}),
  deleteSystemFlag:(key:string)=>call<any>(`/admin/system-flags/${encodeURIComponent(key)}`,{method:'DELETE'}),

  // 17 Admin Users
  admins:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/admin-users?'+qs(params)),
  adminUser:(id:string)=>call<any>(`/admin/admin-users/${id}`),
  createAdminUser:(body:any)=>call<any>('/admin/admin-users',{method:'POST',body:JSON.stringify(body)}),
  updateAdminUser:(id:string,body:any)=>call<any>(`/admin/admin-users/${id}`,{method:'PATCH',body:JSON.stringify(body)}),
  setAdminActive:(id:string,isActive:boolean)=>call<any>(`/admin/admin-users/${id}/active`,{method:'POST',body:JSON.stringify({isActive})}),
  resetAdminPassword:(id:string,password:string)=>call<any>(`/admin/admin-users/${id}/password`,{method:'POST',body:JSON.stringify({password})}),

  // 18 Audit
  auditLogs:(params:Record<string,string|number|boolean|undefined>={})=>call<any>('/admin/audit?'+qs(params)),

  // 19 Admin Settings
  adminSettings:()=>call<any>('/admin/settings'),
  updateAdminProfile:(body:any)=>call<any>('/admin/settings/profile',{method:'PATCH',body:JSON.stringify(body)}),
  updateAdminPreferences:(body:any)=>call<any>('/admin/settings/preferences',{method:'PATCH',body:JSON.stringify(body)}),
  globalAdminSettings:()=>call<any>('/admin/settings/global'),
  updateGlobalAdminSettings:(body:any)=>call<any>('/admin/settings/global',{method:'PATCH',body:JSON.stringify(body)}),

  // 20 Final QA
  finalQa:()=>call<any>('/admin/final-qa')
};

export type {Admin};
