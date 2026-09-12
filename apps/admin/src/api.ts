const API=(import.meta.env.VITE_API_BASE_URL||'http://localhost:3000/api/v1').replace(/\/$/,'');
export type Admin={id:string;email:string;displayName:string;role:string;permissions:string[]};
export type Session={accessToken:string;refreshToken:string;expiresIn:number;admin:Admin};
async function request<T>(path:string,init:RequestInit={}){const r=await fetch(`${API}${path}`,{...init,headers:{'Content-Type':'application/json',...(init.headers||{})}});const data=await r.json().catch(()=>({}));if(!r.ok)throw new Error(data.message||'Request failed');return data as T;}
export const authApi={
 login:(email:string,password:string)=>request<Session>('/admin/auth/login',{method:'POST',body:JSON.stringify({email,password})}),
 me:(token:string)=>request<Admin>('/admin/auth/me',{headers:{Authorization:`Bearer ${token}`}}),
 refresh:(refreshToken:string)=>request<Session>('/admin/auth/refresh',{method:'POST',body:JSON.stringify({refreshToken})}),
 forgot:(email:string)=>request<{ok:boolean}>('/admin/auth/forgot-password',{method:'POST',body:JSON.stringify({email})}),
 verifyReset:(email:string,code:string)=>request<{resetToken:string}>('/admin/auth/verify-reset-otp',{method:'POST',body:JSON.stringify({email,code})}),
 reset:(resetToken:string,password:string)=>request<{ok:boolean}>('/admin/auth/reset-password',{method:'POST',body:JSON.stringify({resetToken,password})}),
 logout:(token:string,refreshToken:string)=>request<{ok:boolean}>('/admin/auth/logout',{method:'POST',headers:{Authorization:`Bearer ${token}`},body:JSON.stringify({refreshToken})})
};
