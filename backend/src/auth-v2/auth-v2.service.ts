import {
  BadRequestException,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';

import { SupabaseRestClient } from '../common/supabase-rest';

type AuthSessionResponse = {
  access_token?: string;
  refresh_token?: string;
  expires_in?: number;
  token_type?: string;
  user?: {
    id: string;
    email?: string;
    email_confirmed_at?: string | null;
  };
};

@Injectable()
export class AuthV2Service {
  constructor(private readonly supabase: SupabaseRestClient) {}

  private normalizeError(error: any): never {
    const status = Number(error?.statusCode || 400);
    const message = String(error?.message || 'Authentication request failed');

    if (status === 401 || status === 403) {
      throw new UnauthorizedException(message);
    }
    throw new BadRequestException(message);
  }

  async signUp(input: {
    email: string;
    password: string;
    fullName?: string;
    username?: string;
  }) {
    try {
      return await this.supabase.authRequest<AuthSessionResponse>('signup', {
        method: 'POST',
        body: JSON.stringify({
          email: input.email.trim().toLowerCase(),
          password: input.password,
          data: {
            full_name: input.fullName?.trim() || null,
            username: input.username?.trim() || null,
          },
        }),
      });
    } catch (e) {
      this.normalizeError(e);
    }
  }

  async signIn(input: { email: string; password: string }) {
    try {
      return await this.supabase.authRequest<AuthSessionResponse>(
        'token?grant_type=password',
        {
          method: 'POST',
          body: JSON.stringify({
            email: input.email.trim().toLowerCase(),
            password: input.password,
          }),
        },
      );
    } catch (e) {
      this.normalizeError(e);
    }
  }

  async refresh(refreshToken: string) {
    try {
      return await this.supabase.authRequest<AuthSessionResponse>(
        'token?grant_type=refresh_token',
        {
          method: 'POST',
          body: JSON.stringify({
            refresh_token: refreshToken,
          }),
        },
      );
    } catch (e) {
      this.normalizeError(e);
    }
  }

  async resendSignupOtp(email: string) {
    try {
      return await this.supabase.authRequest('resend', {
        method: 'POST',
        body: JSON.stringify({
          type: 'signup',
          email: email.trim().toLowerCase(),
        }),
      });
    } catch (e) {
      this.normalizeError(e);
    }
  }

  async verifyEmail(input: { email: string; token: string }) {
    try {
      return await this.supabase.authRequest<AuthSessionResponse>('verify', {
        method: 'POST',
        body: JSON.stringify({
          type: 'email',
          email: input.email.trim().toLowerCase(),
          token: input.token.trim(),
        }),
      });
    } catch (e) {
      this.normalizeError(e);
    }
  }

  async requestPasswordReset(email: string) {
    try {
      return await this.supabase.authRequest('recover', {
        method: 'POST',
        body: JSON.stringify({
          email: email.trim().toLowerCase(),
        }),
      });
    } catch (e) {
      this.normalizeError(e);
    }
  }

  async signOut(accessToken: string) {
    try {
      await this.supabase.authRequest('logout', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      });
      return { signedOut: true };
    } catch (e) {
      this.normalizeError(e);
    }
  }

  async verifyRecovery(input: { email: string; token: string }) {
    try {
      return await this.supabase.authRequest<AuthSessionResponse>('verify', {
        method: 'POST',
        body: JSON.stringify({
          type: 'recovery',
          email: input.email.trim().toLowerCase(),
          token: input.token.trim(),
        }),
      });
    } catch (e) {
      this.normalizeError(e);
    }
  }

  async updatePassword(accessToken: string, password: string) {
    if (!accessToken) {
      throw new UnauthorizedException('Bearer token required');
    }
    if (!password || password.length < 8) {
      throw new BadRequestException('Password must be at least 8 characters');
    }

    try {
      return await this.supabase.authRequest('user', {
        method: 'PUT',
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        body: JSON.stringify({ password }),
      });
    } catch (e) {
      this.normalizeError(e);
    }
  }
}
