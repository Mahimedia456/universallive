import { getAuthTokens } from '@/lib/auth-storage';
import { backendApi } from '@/services/backend-api';
import { AI_WORKER_URL } from '@/services/ai-worker';

export type SystemCheckKey = 'database' | 'backend' | 'auth' | 'ai';
export type SystemCheckState = 'idle' | 'checking' | 'ok' | 'error';

export type SystemCheckResult = {
  key: SystemCheckKey;
  label: string;
  state: SystemCheckState;
  detail: string;
};

const API_URL = (process.env.EXPO_PUBLIC_API_URL?.trim() || 'http://127.0.0.1:3000').replace(/\/+$/, '');

async function fetchWithTimeout(url: string, timeoutMs = 7000) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), timeoutMs);
  try { return await fetch(url, { signal: controller.signal }); }
  finally { clearTimeout(timeout); }
}

export const systemCheckUrls = { backend: API_URL, ai: AI_WORKER_URL };

export async function runSystemChecks(): Promise<SystemCheckResult[]> {
  const results: SystemCheckResult[] = [];

  try {
    const response = await fetchWithTimeout(`${API_URL}/api/v1/health`);
    const payload = await response.json() as { status?: string; auth?: string; database?: { ok?: boolean; detail?: string } };
    if (!response.ok || payload.status !== 'ok' || payload.database?.ok === false) throw new Error(payload.database?.detail || `Backend health is ${payload.status ?? `HTTP ${response.status}`}.`);
    results.push({ key: 'database', label: 'SizeME Database', state: 'ok', detail: 'Backend can reach the SizeME users/profile database.' });
    results.push({ key: 'backend', label: 'NestJS API', state: 'ok', detail: `API reachable · auth=${payload.auth ?? 'unknown'}` });
  } catch (error) {
    const detail = error instanceof Error ? error.message : 'Backend/database check failed.';
    results.push({ key: 'database', label: 'SizeME Database', state: 'error', detail });
    results.push({ key: 'backend', label: 'NestJS API', state: 'error', detail });
  }

  try {
    const tokens = await getAuthTokens();
    if (!tokens) throw new Error('No SizeME login session is stored on this device.');
    const me = await backendApi.me();
    results.push({ key: 'auth', label: 'SizeME Auth', state: 'ok', detail: `JWT accepted for ${me.user.email}. Profile ${me.profileId.slice(0, 8)}…` });
  } catch (error) {
    results.push({ key: 'auth', label: 'SizeME Auth', state: 'error', detail: error instanceof Error ? error.message : 'Authenticated backend request failed.' });
  }

  try {
    const response = await fetchWithTimeout(`${AI_WORKER_URL}/health`);
    const payload = await response.json() as { status?: string; pose_engine?: { ready?: boolean; engine?: string } };
    if (!response.ok || payload.status !== 'ok' || payload.pose_engine?.ready === false) throw new Error(`AI worker is ${payload.status ?? `HTTP ${response.status}`}.`);
    results.push({ key: 'ai', label: 'Python AI Worker', state: 'ok', detail: `Reachable${payload.pose_engine?.engine ? ` · ${payload.pose_engine.engine}` : ''}.` });
  } catch (error) {
    results.push({ key: 'ai', label: 'Python AI Worker', state: 'error', detail: error instanceof Error ? error.message : 'AI worker check failed.' });
  }

  return results;
}
