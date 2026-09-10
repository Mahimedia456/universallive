import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';

@Injectable()
export class SupabaseRestClient {
  constructor(private readonly config: ConfigService) {}

  get url(): string {
    const value = this.config.get<string>('SUPABASE_URL')?.replace(/\/+$/, '');
    if (!value) throw new InternalServerErrorException('SUPABASE_URL is not configured');
    return value;
  }

  get publishableKey(): string {
    const value =
      this.config.get<string>('SUPABASE_PUBLISHABLE_KEY') ||
      this.config.get<string>('SUPABASE_ANON_KEY');

    if (!value) {
      throw new InternalServerErrorException(
        'SUPABASE_PUBLISHABLE_KEY or SUPABASE_ANON_KEY is not configured',
      );
    }
    return value;
  }

  get serviceKey(): string {
    const value =
      this.config.get<string>('SUPABASE_SECRET_KEY') ||
      this.config.get<string>('SUPABASE_SERVICE_ROLE_KEY');

    if (!value) {
      throw new InternalServerErrorException(
        'SUPABASE_SECRET_KEY or SUPABASE_SERVICE_ROLE_KEY is not configured',
      );
    }
    return value;
  }

  async authRequest<T>(
    path: string,
    init: RequestInit = {},
  ): Promise<T> {
    const response = await fetch(`${this.url}/auth/v1/${path}`, {
      ...init,
      headers: {
        apikey: this.publishableKey,
        'Content-Type': 'application/json',
        ...(init.headers || {}),
      },
    });

    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      const message =
        payload?.msg ||
        payload?.message ||
        payload?.error_description ||
        payload?.error ||
        `Supabase Auth request failed (${response.status})`;
      const error: any = new Error(message);
      error.statusCode = response.status;
      error.payload = payload;
      throw error;
    }
    return payload as T;
  }

  async restRequest<T>(
    path: string,
    accessToken: string,
    init: RequestInit = {},
  ): Promise<T> {
    const response = await fetch(`${this.url}/rest/v1/${path}`, {
      ...init,
      headers: {
        apikey: this.publishableKey,
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
        Prefer: 'return=representation',
        ...(init.headers || {}),
      },
    });

    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      const error: any = new Error(
        payload?.message || `Supabase REST request failed (${response.status})`,
      );
      error.statusCode = response.status;
      error.payload = payload;
      throw error;
    }
    return payload as T;
  }
}
