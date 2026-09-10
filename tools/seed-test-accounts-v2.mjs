import fs from 'node:fs';
import path from 'node:path';
import { createRequire } from 'node:module';

const root = 'E:\\UniversalLive';
const backend = path.join(root, 'backend');
const envPath = path.join(backend, '.env');

function parseEnv(file) {
  const result = {};
  for (const raw of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const line = raw.trim();
    if (!line || line.startsWith('#')) continue;
    const i = line.indexOf('=');
    if (i < 1) continue;
    const key = line.slice(0, i).trim();
    let value = line.slice(i + 1).trim();
    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) value = value.slice(1, -1);
    result[key] = value;
  }
  return result;
}

if (!fs.existsSync(envPath)) {
  throw new Error(`Missing backend .env: ${envPath}`);
}

const env = { ...parseEnv(envPath), ...process.env };

const SUPABASE_URL = String(env.SUPABASE_URL || '').trim().replace(/\/+$/, '');
if (!SUPABASE_URL) {
  throw new Error('SUPABASE_URL is missing from backend .env');
}

function decodeJwtPayload(value) {
  try {
    const parts = value.split('.');
    if (parts.length !== 3) return null;
    const b64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = b64 + '='.repeat((4 - (b64.length % 4)) % 4);
    return JSON.parse(Buffer.from(padded, 'base64').toString('utf8'));
  } catch {
    return null;
  }
}

function classify(value) {
  const v = String(value || '').trim();
  if (!v) return { validAdmin: false, type: 'empty' };

  if (v.startsWith('sb_secret_')) {
    return { validAdmin: true, type: 'Supabase secret key' };
  }

  if (v.startsWith('sb_publishable_')) {
    return { validAdmin: false, type: 'publishable key' };
  }

  const payload = decodeJwtPayload(v);
  if (payload) {
    if (payload.role === 'service_role') {
      return { validAdmin: true, type: 'legacy service_role JWT' };
    }
    if (payload.role === 'anon') {
      return { validAdmin: false, type: 'legacy anon JWT' };
    }
    return {
      validAdmin: false,
      type: `JWT with role=${payload.role || 'unknown'}`,
    };
  }

  return { validAdmin: false, type: 'unknown key format' };
}

function mask(value) {
  const v = String(value || '');
  if (!v) return '(empty)';
  if (v.length <= 12) return `${v.slice(0, 3)}...`;
  return `${v.slice(0, 6)}...${v.slice(-4)}`;
}

const candidates = [
  'SUPABASE_SECRET_KEY',
  'SUPABASE_SERVICE_ROLE_KEY',
  'SUPABASE_SERVICE_KEY',
  'SERVICE_ROLE_KEY',
  'SUPABASE_SERVICE_ROLE',
];

let adminKey = '';
let adminKeyName = '';

console.log('=== Supabase Admin Key Diagnostic ===');

for (const name of candidates) {
  if (!(name in env) || !String(env[name] || '').trim()) continue;

  const value = String(env[name]).trim();
  const info = classify(value);

  console.log(
    `${name}: ${info.type} (${mask(value)})`,
  );

  if (!adminKey && info.validAdmin) {
    adminKey = value;
    adminKeyName = name;
  }
}

const publicCandidates = [
  'SUPABASE_PUBLISHABLE_KEY',
  'SUPABASE_ANON_KEY',
];

for (const name of publicCandidates) {
  if (!(name in env) || !String(env[name] || '').trim()) continue;
  const value = String(env[name]).trim();
  const info = classify(value);
  console.log(`${name}: ${info.type} (${mask(value)})`);
}

if (!adminKey) {
  console.error('');
  console.error('NO VALID SUPABASE ADMIN KEY FOUND.');
  console.error('');
  console.error('Open Supabase Dashboard -> Project Settings -> API Keys.');
  console.error('Copy ONE of these server-side keys:');
  console.error('  1) Secret key beginning with sb_secret_');
  console.error('  OR');
  console.error('  2) Legacy service_role JWT');
  console.error('');
  console.error('Then put it in backend\\.env as ONE of:');
  console.error('SUPABASE_SECRET_KEY=sb_secret_...');
  console.error('or');
  console.error('SUPABASE_SERVICE_ROLE_KEY=<legacy service_role JWT>');
  console.error('');
  console.error('Do NOT use SUPABASE_PUBLISHABLE_KEY / anon key here.');
  process.exit(2);
}

console.log('');
console.log(`[OK] Admin key selected from ${adminKeyName}`);
console.log('');

const requireFromBackend = createRequire(path.join(backend, 'package.json'));

let createClient;
try {
  ({ createClient } = requireFromBackend('@supabase/supabase-js'));
} catch (error) {
  console.error(
    '@supabase/supabase-js was not found in backend node_modules.',
  );
  console.error(
    'Run: cd E:\\UniversalLive\\backend && npm install @supabase/supabase-js',
  );
  process.exit(3);
}

const supabase = createClient(
  SUPABASE_URL,
  adminKey,
  {
    auth: {
      autoRefreshToken: false,
      persistSession: false,
      detectSessionInUrl: false,
    },
  },
);

async function listAllUsers() {
  const result = [];
  for (let page = 1; page <= 20; page += 1) {
    const { data, error } = await supabase.auth.admin.listUsers({
      page,
      perPage: 100,
    });
    if (error) throw error;

    const users = data?.users || [];
    result.push(...users);

    if (users.length < 100) break;
  }
  return result;
}

async function ensureUser(account, password, allUsers) {
  let user = allUsers.find(
    (u) =>
      String(u.email || '').toLowerCase() === account.email.toLowerCase(),
  );

  const metadata = {
    full_name: account.displayName,
    username: account.username,
    seeded_test_account: true,
    membership_test_plan: account.planKey,
  };

  if (!user) {
    const { data, error } =
      await supabase.auth.admin.createUser({
        email: account.email,
        password,
        email_confirm: true,
        user_metadata: metadata,
      });

    if (error) throw error;
    user = data.user;

    console.log(
      `[CREATE] ${account.planKey.toUpperCase()} ${account.email}`,
    );
  } else {
    const { data, error } =
      await supabase.auth.admin.updateUserById(
        user.id,
        {
          password,
          email_confirm: true,
          user_metadata: {
            ...(user.user_metadata || {}),
            ...metadata,
          },
        },
      );

    if (error) throw error;
    user = data.user;

    console.log(
      `[UPDATE] ${account.planKey.toUpperCase()} ${account.email}`,
    );
  }

  return user;
}

async function upsertProfile(user, account) {
  const { error } = await supabase
    .from('ul_creator_profiles')
    .upsert(
      {
        user_id: user.id,
        display_name: account.displayName,
        username: account.username,
        creator_type: 'creator',
        onboarding_completed: true,
        metadata: {
          seeded_test_account: true,
          membership_test_plan: account.planKey,
        },
        updated_at: new Date().toISOString(),
      },
      { onConflict: 'user_id' },
    );

  if (error) throw error;
}

async function upsertOnboarding(user) {
  const { error } = await supabase
    .from('ul_onboarding_state')
    .upsert(
      {
        user_id: user.id,
        creator_setup_completed: true,
        permission_education_completed: true,
        first_destination_prompt_completed: true,
        microphone_acknowledged: true,
        camera_acknowledged: true,
        screen_capture_acknowledged: true,
        notifications_acknowledged: true,
        updated_at: new Date().toISOString(),
      },
      { onConflict: 'user_id' },
    );

  if (error) throw error;
}

async function upsertEntitlement(user, account) {
  const { error } = await supabase
    .from('ul_user_entitlements')
    .upsert(
      {
        user_id: user.id,
        plan_key: account.planKey,
        status: 'active',
        source: 'test-seed',
        starts_at: new Date().toISOString(),
        expires_at: null,
        grace_until: null,
        entitlements_override: {},
        updated_at: new Date().toISOString(),
      },
      { onConflict: 'user_id' },
    );

  if (error) throw error;
}

const password =
  env.UL_TEST_ACCOUNT_PASSWORD ||
  'UniversalLive@Test12345';

const accounts = [
  {
    email:
      env.UL_TEST_FREE_EMAIL ||
      'free.test@universallive.local',
    username: 'ul_free_test',
    displayName: 'Free Test Creator',
    planKey: 'free',
  },
  {
    email:
      env.UL_TEST_CREATOR_EMAIL ||
      'creator.test@universallive.local',
    username: 'ul_creator_test',
    displayName: 'Creator Test Account',
    planKey: 'creator',
  },
  {
    email:
      env.UL_TEST_PRO_EMAIL ||
      'pro.test@universallive.local',
    username: 'ul_pro_test',
    displayName: 'Pro Test Account',
    planKey: 'pro',
  },
];

let allUsers;
try {
  allUsers = await listAllUsers();
} catch (error) {
  console.error('');
  console.error('SUPABASE AUTH ADMIN CHECK FAILED.');
  console.error(error?.message || error);
  console.error('');
  console.error(
    'The selected key is not accepted as a project admin key.',
  );
  console.error(
    'Re-copy the Secret key or legacy service_role key from the SAME Supabase project as SUPABASE_URL.',
  );
  process.exit(4);
}

for (const account of accounts) {
  const user = await ensureUser(account, password, allUsers);
  await upsertProfile(user, account);
  await upsertOnboarding(user);
  await upsertEntitlement(user, account);
}

console.log('');
console.log('==============================================');
console.log('UNIVERSAL LIVE TEST ACCOUNTS READY');
console.log('==============================================');
for (const account of accounts) {
  console.log(
    `${account.planKey.toUpperCase().padEnd(8)} ${account.email}`,
  );
}
console.log(`PASSWORD ${password}`);
console.log('');
console.log('Existing users/data were not deleted.');
