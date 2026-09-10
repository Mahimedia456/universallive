const API_BASE =
  import.meta.env.VITE_API_BASE_URL ||
  'https://universallive.vercel.app/api/v1';

const TOKEN_KEY = 'universallive_admin_access_token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || '';
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

export async function api(path, options = {}) {
  const headers = {
    Accept: 'application/json',
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  };

  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${API_BASE}/${path.replace(/^\/+/, '')}`, {
    ...options,
    headers,
  });

  const payload = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      payload?.message ||
      payload?.error ||
      `Request failed (${response.status})`
    );
  }

  return payload;
}

export async function login(email, password) {
  const payload = await api('auth/mobile/login', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });

  const token = payload?.access_token;
  if (!token) throw new Error('Login did not return an access token');

  localStorage.setItem(TOKEN_KEY, token);

  try {
    const admin = await api('admin-console/me');
    return admin;
  } catch (error) {
    clearToken();
    throw error;
  }
}

export async function logout() {
  clearToken();
}

export { API_BASE };
