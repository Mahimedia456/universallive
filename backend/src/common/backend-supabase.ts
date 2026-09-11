import {
  Injectable,
  InternalServerErrorException,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

import type {
  UniversalLiveAuthUser,
  UniversalLiveUserRow,
} from '../auth/auth.types';
import {
  verifyAccessToken,
} from './universallive-jwt';

@Injectable()
export class BackendSupabase {
  constructor(private readonly config: ConfigService) {}

  private get baseUrl(): string {
    const value = this.config.get<string>('SUPABASE_URL')?.replace(/\/+$/, '');
    if (!value) {
      throw new InternalServerErrorException('SUPABASE_URL is missing');
    }
    return value;
  }

  private get serviceKey(): string {
    const value =
      this.config.get<string>('SUPABASE_SECRET_KEY') ||
      this.config.get<string>('SUPABASE_SERVICE_ROLE_KEY');

    if (!value) {
      throw new InternalServerErrorException(
        'SUPABASE_SECRET_KEY or SUPABASE_SERVICE_ROLE_KEY is missing',
      );
    }
    return value.trim();
  }

  /**
   * New Supabase sb_secret_* keys are API keys, not JWT bearer tokens.
   * Legacy service_role keys are JWTs and may also be sent as Bearer tokens.
   */
  private serviceHeaders(): Record<string, string> {
    const key = this.serviceKey;

    if (key.startsWith('sb_secret_')) {
      return { apikey: key };
    }

    return {
      apikey: key,
      Authorization: `Bearer ${key}`,
    };
  }

  async userRowById(userId: string): Promise<UniversalLiveUserRow> {
    const rows = await this.adminRest<UniversalLiveUserRow[]>(
      `ul_users?id=eq.${encodeURIComponent(userId)}&select=*`,
      { method: 'GET' },
    );
    const user = rows?.[0];
    if (!user || !user.is_active) {
      throw new UnauthorizedException('Account is unavailable');
    }
    return user;
  }

  async currentUser(accessToken: string): Promise<UniversalLiveAuthUser> {
    if (!accessToken) {
      throw new UnauthorizedException('Bearer token required');
    }

    const payload = verifyAccessToken(
      accessToken,
      this.config.get<string>('JWT_ACCESS_SECRET'),
    );
    const row = await this.userRowById(payload.sub);

    if (row.email.toLowerCase() !== payload.email.toLowerCase()) {
      throw new UnauthorizedException('Session no longer matches this account');
    }

    return {
      id: row.id,
      email: row.email,
      phone: null,
      created_at: row.created_at,
      email_confirmed_at: row.email_verified_at,
      user_metadata: {
        full_name: row.full_name,
        username: row.username,
      },
    };
  }

  async adminRest<T>(
    path: string,
    init: RequestInit = {},
  ): Promise<T> {
    const response = await fetch(`${this.baseUrl}/rest/v1/${path}`, {
      ...init,
      headers: {
        ...this.serviceHeaders(),
        'Content-Type': 'application/json',
        Prefer: 'return=representation',
        ...(init.headers || {}),
      },
    });

    const payload = await response.json().catch(() => null);

    if (!response.ok) {
      const error: any = new Error(
        payload?.message ||
          payload?.msg ||
          `Supabase REST error ${response.status}`,
      );
      error.statusCode = response.status;
      error.payload = payload;
      throw error;
    }

    return payload as T;
  }
}

export function bearerToken(value?: string): string {
  const token = (value || '').replace(/^Bearer\s+/i, '').trim();

  if (!token) {
    throw new UnauthorizedException('Bearer token required');
  }

  return token;
}
