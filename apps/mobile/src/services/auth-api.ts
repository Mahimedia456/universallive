import { clearAuthTokens, getRefreshToken, saveAuthTokens } from '@/lib/auth-storage';

const rawBaseUrl = process.env.EXPO_PUBLIC_API_URL?.trim() || 'http://127.0.0.1:3000';
const API_BASE = rawBaseUrl.replace(/\/+$/, '');

export class AuthApiError extends Error {
  constructor(message: string, readonly status: number, readonly body?: unknown) {
    super(message);
    this.name = 'AuthApiError';
  }
}

export type AuthUser = {
  id: string;
  email: string;
  firstName: string;
  lastName?: string;
  displayName?: string;
  emailVerified?: boolean;
};

export type AuthSessionResponse = {
  accessToken: string;
  refreshToken: string;
  expiresInSeconds: number;
  user: AuthUser;
};

async function jsonRequest<T>(path: string, payload?: unknown): Promise<T> {
  const response = await fetch(`${API_BASE}/api/v1${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: payload === undefined ? undefined : JSON.stringify(payload),
  });
  const text = await response.text();
  let body: unknown = undefined;
  if (text) {
    try { body = JSON.parse(text); } catch { body = text; }
  }
  if (!response.ok) {
    const raw = typeof body === 'object' && body && 'message' in body ? (body as { message?: unknown }).message : undefined;
    const message = Array.isArray(raw) ? raw.join('\n') : raw ? String(raw) : `Request failed with HTTP ${response.status}`;
    throw new AuthApiError(message, response.status, body);
  }
  return body as T;
}

export async function persistSession(session: AuthSessionResponse) {
  await saveAuthTokens({ accessToken: session.accessToken, refreshToken: session.refreshToken });
  return session;
}

export async function refreshStoredSession(): Promise<AuthSessionResponse> {
  const refreshToken = await getRefreshToken();
  if (!refreshToken) throw new AuthApiError('No refresh session is available.', 401);
  try {
    const session = await jsonRequest<AuthSessionResponse>('/auth/refresh', { refreshToken });
    return persistSession(session);
  } catch (error) {
    await clearAuthTokens();
    throw error;
  }
}

export const authApi = {
  register: (payload: { firstName: string; lastName?: string; email: string; password: string }) =>
    jsonRequest<{ verificationRequired: boolean; email: string; message: string }>('/auth/register', payload),
  verifyEmail: (email: string, code: string) => jsonRequest<AuthSessionResponse>('/auth/verify-email', { email, code }),
  resendVerification: (email: string) => jsonRequest<{ sent: boolean; message: string }>('/auth/resend-otp', { email, purpose: 'verify_email' }),
  login: (email: string, password: string) => jsonRequest<AuthSessionResponse>('/auth/login', { email, password }),
  forgotPassword: (email: string) => jsonRequest<{ sent: boolean; message: string }>('/auth/forgot-password', { email }),
  verifyResetOtp: (email: string, code: string) => jsonRequest<{ resetToken: string; expiresInSeconds: number }>('/auth/verify-reset-otp', { email, code }),
  resendPasswordReset: (email: string) => jsonRequest<{ sent: boolean; message: string }>('/auth/resend-otp', { email, purpose: 'password_reset' }),
  resetPassword: (resetToken: string, newPassword: string) => jsonRequest<{ changed: boolean }>('/auth/reset-password', { resetToken, newPassword }),
  refreshStoredSession,
  async logout() {
    const refreshToken = await getRefreshToken();
    try { await jsonRequest('/auth/logout', { refreshToken: refreshToken ?? undefined }); } finally { await clearAuthTokens(); }
  },
};
