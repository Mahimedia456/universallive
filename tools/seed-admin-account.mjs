import fs from 'node:fs';
import path from 'node:path';

const root = 'E:\\UniversalLive';
const envPath = path.join(root, 'backend', '.env');

function parseEnv(file) {
  const result = {};
  for (const raw of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    const i = line.indexOf('=');
    if (i < 1) continue;
    const key = line.slice(0, i).trim();
    let value = line.slice(i + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) ||
        (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    result[key] = value;
  }
  return result;
}

const env = { ...parseEnv(envPath), ...process.env };
const url = String(env.SUPABASE_URL || '').replace(/\/+$/, '');
const key = String(env.SUPABASE_SECRET_KEY || env.SUPABASE_SERVICE_ROLE_KEY || '').trim();

if (!url || !key) throw new Error('SUPABASE_URL/admin key missing');

const email = env.UL_ADMIN_EMAIL || 'admin.test@universallive.local';
const password = env.UL_ADMIN_PASSWORD || 'UniversalLive@Admin12345';

function headers(extra = {}) {
  const h = { apikey: key, 'Content-Type':'application/json', ...extra };
  if (key.startsWith('eyJ')) h.Authorization = `Bearer ${key}`;
  return h;
}

async function req(pathname, options = {}) {
  const res = await fetch(`${url}${pathname}`, {
    ...options,
    headers: headers(options.headers || {}),
  });
  const body = await res.json().catch(() => null);
  if (!res.ok) throw new Error(`${res.status}: ${body?.message || JSON.stringify(body)}`);
  return body;
}

async function findUser() {
  for (let page=1; page<=20; page++) {
    const data = await req(`/auth/v1/admin/users?page=${page}&per_page=100`);
    const users = data.users || [];
    const hit = users.find(u => (u.email || '').toLowerCase() === email.toLowerCase());
    if (hit) return hit;
    if (users.length < 100) break;
  }
  return null;
}

let user = await findUser();

if (!user) {
  user = await req('/auth/v1/admin/users', {
    method:'POST',
    body:JSON.stringify({
      email,
      password,
      email_confirm:true,
      user_metadata:{ full_name:'Universal Live Admin' }
    })
  });
} else {
  user = await req(`/auth/v1/admin/users/${user.id}`, {
    method:'PUT',
    body:JSON.stringify({
      password,
      email_confirm:true,
      user_metadata:{ ...(user.user_metadata || {}), full_name:'Universal Live Admin' }
    })
  });
}

const res = await fetch(`${url}/rest/v1/ul_admin_users?on_conflict=user_id`, {
  method:'POST',
  headers:headers({ Prefer:'resolution=merge-duplicates,return=representation' }),
  body:JSON.stringify({
    user_id:user.id,
    role:'owner',
    is_active:true,
    display_name:'Universal Live Owner',
    updated_at:new Date().toISOString()
  })
});

const payload = await res.json().catch(()=>null);
if (!res.ok) throw new Error(`${res.status}: ${payload?.message || JSON.stringify(payload)}`);

console.log('ADMIN ACCOUNT READY');
console.log(`EMAIL    ${email}`);
console.log(`PASSWORD ${password}`);
console.log('ROLE     owner');
