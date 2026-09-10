import { getAccessToken } from '@/lib/auth-storage';
import { refreshStoredSession } from '@/services/auth-api';

const rawBaseUrl = process.env.EXPO_PUBLIC_API_URL?.trim() || 'http://127.0.0.1:3000';
const API_BASE = rawBaseUrl.replace(/\/+$/, '');

export class ApiError extends Error {
  constructor(message: string, readonly status: number, readonly body?: unknown) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const token = await getAccessToken();
  if (!token) throw new ApiError('You are not signed in.', 401);

  const response = await fetch(`${API_BASE}/api/v1${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(init.headers ?? {}),
    },
  });

  if (response.status === 401 && retry) {
    try {
      await refreshStoredSession();
      return request<T>(path, init, false);
    } catch {
      // fall through and report the original authenticated request failure
    }
  }

  const text = await response.text();
  let body: unknown = undefined;
  if (text) {
    try { body = JSON.parse(text); } catch { body = text; }
  }
  if (!response.ok) {
    const raw = typeof body === 'object' && body && 'message' in body ? (body as { message?: unknown }).message : undefined;
    const message = Array.isArray(raw) ? raw.join('\n') : raw ? String(raw) : `API request failed with HTTP ${response.status}`;
    throw new ApiError(message, response.status, body);
  }
  return body as T;
}

export type CloudProfile = {
  id: string;
  user_id: string;
  auth_user_id?: string | null;
  display_name: string;
  birth_year: number | null;
  sizing_profile: 'menswear' | 'womenswear' | 'universal';
  height_cm: number | null;
  preferred_unit: 'cm' | 'in';
  country_code: string | null;
  fit_preference: 'slim' | 'regular' | 'relaxed';
  theme_preference: 'system' | 'light' | 'dark';
  onboarding_completed: boolean;
};

export type UpdateCloudProfile = {
  displayName?: string;
  birthYear?: number | null;
  sizingProfile?: 'menswear' | 'womenswear' | 'universal';
  heightCm?: number;
  preferredUnit?: 'cm' | 'in';
  countryCode?: string | null;
  fitPreference?: 'slim' | 'regular' | 'relaxed';
  themePreference?: 'system' | 'light' | 'dark';
  onboardingCompleted?: boolean;
};

export type AuthMe = {
  user: { id: string; email: string; role: 'user'; firstName: string; lastName?: string };
  profileId: string;
};

export const backendApi = {
  health: () => fetch(`${API_BASE}/api/v1/health`).then((response) => response.json()),
  me: () => request<AuthMe>('/auth/me'),
  profile: () => request<CloudProfile>('/profile'),
  updateProfile: (payload: UpdateCloudProfile) => request<CloudProfile>('/profile', { method: 'PATCH', body: JSON.stringify(payload) }),
};
