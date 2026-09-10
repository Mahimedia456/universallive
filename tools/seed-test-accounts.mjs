import fs from 'node:fs';
import path from 'node:path';

const root = 'E:\\UniversalLive';
const envPath = path.join(root, 'backend', '.env');

function parseEnv(file) {
  const result = {};

  for (const raw of fs.readFileSync(file, 'utf8').split(/\r?\n/)) {
    const line = raw.trim();

    if (!line || line.startsWith('#')) continue;

    const index = line.indexOf('=');
    if (index < 1) continue;

    const key = line.slice(0, index).trim();
    let value = line.slice(index + 1).trim();

    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }

    result[key] = value;
  }

  return result;
}

if (!fs.existsSync(envPath)) {
  throw new Error(`Backend .env not found: ${envPath}`);
}

const env = {
  ...parseEnv(envPath),
  ...process.env,
};

const SUPABASE_URL = String(env.SUPABASE_URL || '').replace(/\/+$/, '');

const SERVICE_KEY = String(
  env.SUPABASE_SECRET_KEY ||
  env.SUPABASE_SERVICE_ROLE_KEY ||
  '',
).trim();

if (!SUPABASE_URL) {
  throw new Error('SUPABASE_URL is missing from backend .env');
}

if (!SERVICE_KEY) {
  throw new Error(
    'SUPABASE_SECRET_KEY or SUPABASE_SERVICE_ROLE_KEY is missing from backend .env',
  );
}

const isNewSecretKey = SERVICE_KEY.startsWith('sb_secret_');
const isLegacyJwt = SERVICE_KEY.startsWith('eyJ');

console.log(
  `[AUTH] Admin key mode: ${
    isNewSecretKey
      ? 'new Supabase secret key'
      : isLegacyJwt
        ? 'legacy service_role JWT'
        : 'unknown secure key format'
  }`,
);

function adminHeaders(extra = {}) {
  const headers = {
    apikey: SERVICE_KEY,
    'Content-Type': 'application/json',
    ...extra,
  };

  // IMPORTANT:
  // sb_secret_* is not a JWT and must not be sent as Authorization: Bearer.
  // Keep Bearer only for the legacy service_role JWT.
  if (isLegacyJwt) {
    headers.Authorization = `Bearer ${SERVICE_KEY}`;
  }

  return headers;
}

async function api(url, options = {}) {
  const response = await fetch(url, {
    ...options,
    headers: adminHeaders(options.headers || {}),
  });

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      `${response.status}: ${
        payload?.message ||
        payload?.msg ||
        payload?.error_description ||
        JSON.stringify(payload)
      }`,
    );
  }

  return payload;
}

async function findUser(email) {
  for (let page = 1; page <= 20; page += 1) {
    const payload = await api(
      `${SUPABASE_URL}/auth/v1/admin/users?page=${page}&per_page=100`,
    );

    const users = payload?.users || [];

    const match = users.find(
      (user) =>
        String(user.email || '').toLowerCase() === email.toLowerCase(),
    );

    if (match) return match;
    if (users.length < 100) break;
  }

  return null;
}

async function ensureAuthUser(account, password) {
  let user = await findUser(account.email);

  const metadata = {
    full_name: account.displayName,
    username: account.username,
    seeded_test_account: true,
    membership_test_plan: account.planKey,
  };

  if (!user) {
    user = await api(`${SUPABASE_URL}/auth/v1/admin/users`, {
      method: 'POST',
      body: JSON.stringify({
        email: account.email,
        password,
        email_confirm: true,
        user_metadata: metadata,
      }),
    });

    console.log(`[CREATE] ${account.planKey.toUpperCase()} ${account.email}`);
    return user;
  }

  user = await api(`${SUPABASE_URL}/auth/v1/admin/users/${user.id}`, {
    method: 'PUT',
    body: JSON.stringify({
      password,
      email_confirm: true,
      user_metadata: {
        ...(user.user_metadata || {}),
        ...metadata,
      },
    }),
  });

  console.log(`[UPDATE] ${account.planKey.toUpperCase()} ${account.email}`);

  return user;
}

async function restUpsert(table, conflictColumn, body) {
  const response = await fetch(
    `${SUPABASE_URL}/rest/v1/${table}?on_conflict=${encodeURIComponent(
      conflictColumn,
    )}`,
    {
      method: 'POST',
      headers: adminHeaders({
        Prefer: 'resolution=merge-duplicates,return=representation',
      }),
      body: JSON.stringify(body),
    },
  );

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      `${table}: ${response.status}: ${
        payload?.message ||
        payload?.hint ||
        payload?.details ||
        JSON.stringify(payload)
      }`,
    );
  }

  return payload;
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

for (const account of accounts) {
  const user = await ensureAuthUser(account, password);

  await restUpsert(
    'ul_creator_profiles',
    'user_id',
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
  );

  await restUpsert(
    'ul_onboarding_state',
    'user_id',
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
  );

  await restUpsert(
    'ul_user_entitlements',
    'user_id',
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
  );
}

console.log('');
console.log('==============================================');
console.log('Universal Live test accounts seeded');
console.log('==============================================');

for (const account of accounts) {
  console.log(
    `${account.planKey.toUpperCase().padEnd(8)} ${account.email}`,
  );
}

console.log(`PASSWORD ${password}`);
console.log('');
console.log('Existing users/data were not deleted.');
console.log('Rerunning this seed updates the same accounts.');
