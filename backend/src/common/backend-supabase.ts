import {
  Injectable,
  InternalServerErrorException,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

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

  private get publicKey(): string {
    const value =
      this.config.get<string>('SUPABASE_PUBLISHABLE_KEY') ||
      this.config.get<string>('SUPABASE_ANON_KEY');

    if (!value) {
      throw new InternalServerErrorException(
        'SUPABASE_PUBLISHABLE_KEY or SUPABASE_ANON_KEY is missing',
      );
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
   * New Supabase sb_secret_* keys are API keys, NOT JWT bearer tokens.
   * Legacy service_role keys are JWTs and may be used as Bearer tokens.
   */
  private serviceHeaders(): Record<string, string> {
    const key = this.serviceKey;

    if (key.startsWith('sb_secret_')) {
      return {
        apikey: key,
      };
    }

    // Legacy service_role JWT compatibility.
    return {
      apikey: key,
      Authorization: `Bearer ${key}`,
    };
  }

  async currentUser(accessToken: string): Promise<any> {
    const response = await fetch(`${this.baseUrl}/auth/v1/user`, {
      headers: {
        apikey: this.publicKey,
        Authorization: `Bearer ${accessToken}`,
      },
    });

    if (!response.ok) {
      throw new UnauthorizedException('Invalid or expired session');
    }

    return response.json();
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

  async adminAuth<T>(
    path: string,
    init: RequestInit = {},
  ): Promise<T> {
    const response = await fetch(`${this.baseUrl}/auth/v1/admin/${path}`, {
      ...init,
      headers: {
        ...this.serviceHeaders(),
        'Content-Type': 'application/json',
        ...(init.headers || {}),
      },
    });

    const payload = await response.json().catch(() => null);

    if (!response.ok) {
      const error: any = new Error(
        payload?.message ||
          payload?.msg ||
          `Supabase Auth Admin error ${response.status}`,
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
