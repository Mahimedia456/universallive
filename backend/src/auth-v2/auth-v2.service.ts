import {
  BadRequestException,
  ConflictException,
  HttpException,
  HttpStatus,
  Injectable,
  UnauthorizedException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
const bcrypt = require('bcryptjs') as {
  hash(value: string, rounds: number): Promise<string>;
  compare(value: string, hash: string): Promise<boolean>;
};
import {
  createHash,
  createHmac,
  randomBytes,
  randomInt,
  timingSafeEqual,
} from 'crypto';
const nodemailer = require('nodemailer') as {
  createTransport(options: Record<string, unknown>): {
    sendMail(message: Record<string, unknown>): Promise<unknown>;
  };
};

import type {
  UniversalLiveUserRow,
} from '../auth/auth.types';
import {
  BackendSupabase,
} from '../common/backend-supabase';
import {
  signAccessToken,
  signPasswordResetToken,
  tokenLifetimeSeconds,
  verifyAccessToken,
  verifyPasswordResetToken,
} from '../common/universallive-jwt';

type OtpPurpose = 'verify_email' | 'password_reset';

type OtpRow = {
  id: string;
  user_id: string;
  email: string;
  purpose: OtpPurpose;
  code_hash: string;
  attempt_count: number;
  max_attempts: number;
  expires_at: string;
  used_at: string | null;
  created_at: string;
};

type RefreshTokenRow = {
  id: string;
  user_id: string;
  token_hash: string;
  expires_at: string;
  revoked_at: string | null;
};

@Injectable()
export class AuthV2Service {
  constructor(
    private readonly db: BackendSupabase,
    private readonly config: ConfigService,
  ) {}

  private normalizeEmail(email: string): string {
    const normalized = String(email || '').trim().toLowerCase();
    if (!/^\S+@\S+\.\S+$/.test(normalized) || normalized.length > 254) {
      throw new BadRequestException('Enter a valid email address');
    }
    return normalized;
  }

  private normalizeUsername(username?: string): string | null {
    const normalized = String(username || '')
      .trim()
      .replace(/^@+/, '')
      .toLowerCase();
    if (!normalized) return null;
    if (!/^[a-z0-9._-]{3,30}$/.test(normalized)) {
      throw new BadRequestException(
        'Username must be 3-30 characters using letters, numbers, dot, underscore or hyphen',
      );
    }
    return normalized;
  }

  private validatePassword(password: string) {
    if (!password || password.length < 8 || password.length > 128) {
      throw new BadRequestException('Password must be 8-128 characters');
    }
    if (!/[A-Za-z]/.test(password) || !/\d/.test(password)) {
      throw new BadRequestException('Password must include a letter and a number');
    }
  }

  private number(name: string, fallback: number): number {
    const value = Number(this.config.get(name) ?? fallback);
    return Number.isFinite(value) && value > 0 ? value : fallback;
  }

  private accessSecret(): string | undefined {
    return this.config.get<string>('JWT_ACCESS_SECRET');
  }

  private resetSecret(): string | undefined {
    return this.config.get<string>('JWT_RESET_SECRET');
  }

  private otpPepper(): string {
    const value = String(this.config.get('OTP_PEPPER') || '').trim();
    if (value.length < 32) {
      throw new Error('OTP_PEPPER must contain at least 32 characters');
    }
    return value;
  }

  private hashOpaqueToken(token: string): string {
    return createHash('sha256')
      .update(`${token}:${this.otpPepper()}`)
      .digest('hex');
  }

  private hashOtp(userId: string, purpose: OtpPurpose, code: string): string {
    return createHmac('sha256', this.otpPepper())
      .update(`${userId}:${purpose}:${code}`)
      .digest('hex');
  }

  private sameHash(left: string, right: string): boolean {
    const a = Buffer.from(left, 'hex');
    const b = Buffer.from(right, 'hex');
    return a.length === b.length && timingSafeEqual(a, b);
  }

  private async userByEmail(email: string): Promise<UniversalLiveUserRow | null> {
    const rows = await this.db.adminRest<UniversalLiveUserRow[]>(
      `ul_users?email=eq.${encodeURIComponent(email)}&select=*`,
      { method: 'GET' },
    );
    return rows?.[0] || null;
  }

  private async userById(id: string): Promise<UniversalLiveUserRow> {
    return this.db.userRowById(id);
  }

  private async audit(
    event: string,
    user: UniversalLiveUserRow | null,
    email: string | null,
    metadata: Record<string, unknown> = {},
  ) {
    try {
      await this.db.adminRest('ul_auth_audit_log', {
        method: 'POST',
        body: JSON.stringify({
          user_id: user?.id || null,
          email: email || user?.email || null,
          event,
          metadata,
        }),
      });
    } catch {
      // Authentication must not fail because audit persistence is temporarily unavailable.
    }
  }

  private async bootstrapAccount(user: UniversalLiveUserRow) {
    await Promise.all([
      this.db.adminRest('ul_creator_profiles?on_conflict=user_id', {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          display_name: user.full_name,
          username: user.username,
          onboarding_completed: false,
          updated_at: new Date().toISOString(),
        }),
      }),
      this.db.adminRest('ul_user_entitlements?on_conflict=user_id', {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          plan_key: 'free',
          status: 'active',
          source: 'system',
          updated_at: new Date().toISOString(),
        }),
      }),
      this.db.adminRest('ul_onboarding_state?on_conflict=user_id', {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          updated_at: new Date().toISOString(),
        }),
      }),
    ]);
  }

  private async sendOtpEmail(
    email: string,
    code: string,
    purpose: OtpPurpose,
  ) {
    const mode = String(this.config.get('EMAIL_MODE') || 'console').toLowerCase();
    const ttl = this.number('OTP_TTL_MINUTES', 10);
    const subject = purpose === 'verify_email'
      ? 'Verify your Universal Live account'
      : 'Reset your Universal Live password';
    const heading = purpose === 'verify_email'
      ? 'Verify your email'
      : 'Reset your password';

    if (mode === 'console') {
      console.log(`[Universal Live OTP] ${purpose} ${email}: ${code}`);
      return;
    }

    const host = String(this.config.get('SMTP_HOST') || '').trim();
    const user = String(this.config.get('SMTP_USER') || '').trim();
    const pass = String(this.config.get('SMTP_PASS') || '').trim();
    if (!host || !user || !pass) {
      throw new Error('SMTP_HOST, SMTP_USER and SMTP_PASS are required for EMAIL_MODE=smtp');
    }

    const transporter = nodemailer.createTransport({
      host,
      port: this.number('SMTP_PORT', 587),
      secure: String(this.config.get('SMTP_SECURE') || 'false').toLowerCase() === 'true',
      auth: { user, pass },
    });

    const fromEmail = String(
      this.config.get('SMTP_FROM_EMAIL') || 'no-reply@universallive.app',
    ).trim();
    const fromName = String(
      this.config.get('SMTP_FROM_NAME') || 'Universal Live',
    ).trim();

    await transporter.sendMail({
      from: `"${fromName.replace(/"/g, '')}" <${fromEmail}>`,
      to: email,
      subject,
      text: `${heading}\n\nYour Universal Live code is ${code}. It expires in ${ttl} minutes.`,
      html: `
        <div style="margin:0;background:#081014;padding:32px;font-family:Arial,sans-serif;color:#f7fbfc">
          <div style="max-width:560px;margin:0 auto;background:#101b20;border:1px solid #21353d;border-radius:20px;padding:32px">
            <div style="font-size:13px;letter-spacing:2px;color:#5ee7f0;font-weight:700">UNIVERSAL LIVE</div>
            <h1 style="margin:16px 0 8px;font-size:28px">${heading}</h1>
            <p style="color:#b6c5ca;line-height:1.6">Use this 6-digit code in the Universal Live app. It expires in ${ttl} minutes.</p>
            <div style="margin:28px 0;padding:20px;text-align:center;border-radius:16px;background:#061115;border:1px solid #26444f;font-size:34px;letter-spacing:10px;font-weight:800;color:#69edf4">${code}</div>
            <p style="color:#7f939a;font-size:12px;line-height:1.6">If you did not request this code, you can ignore this email.</p>
          </div>
        </div>
      `,
    });
  }

  private async issueOtp(user: UniversalLiveUserRow, purpose: OtpPurpose) {
    const recent = await this.db.adminRest<OtpRow[]>(
      `ul_auth_otp_codes?user_id=eq.${encodeURIComponent(user.id)}&purpose=eq.${purpose}&order=created_at.desc&limit=1&select=*`,
      { method: 'GET' },
    );
    const latest = recent?.[0];
    const resendSeconds = this.number('OTP_RESEND_SECONDS', 45);
    if (latest) {
      const ageSeconds = (Date.now() - new Date(latest.created_at).getTime()) / 1000;
      if (ageSeconds < resendSeconds) {
        throw new HttpException(
          `Please wait ${Math.ceil(resendSeconds - ageSeconds)} seconds before requesting another code`,
          HttpStatus.TOO_MANY_REQUESTS,
        );
      }
    }

    const code = randomInt(0, 1_000_000).toString().padStart(6, '0');
    const ttlMinutes = this.number('OTP_TTL_MINUTES', 10);
    const expiresAt = new Date(Date.now() + ttlMinutes * 60_000).toISOString();

    await this.db.adminRest(
      `ul_auth_otp_codes?user_id=eq.${encodeURIComponent(user.id)}&purpose=eq.${purpose}&used_at=is.null`,
      {
        method: 'PATCH',
        body: JSON.stringify({ used_at: new Date().toISOString() }),
      },
    );

    await this.db.adminRest('ul_auth_otp_codes', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        email: user.email,
        purpose,
        code_hash: this.hashOtp(user.id, purpose, code),
        max_attempts: 5,
        expires_at: expiresAt,
      }),
    });

    await this.sendOtpEmail(user.email, code, purpose);
    await this.audit(`${purpose}.otp_sent`, user, user.email);
  }

  private async consumeOtp(
    user: UniversalLiveUserRow,
    purpose: OtpPurpose,
    code: string,
  ) {
    if (!/^\d{6}$/.test(String(code || '').trim())) {
      throw new BadRequestException('Enter the 6-digit code');
    }

    const rows = await this.db.adminRest<OtpRow[]>(
      `ul_auth_otp_codes?user_id=eq.${encodeURIComponent(user.id)}&purpose=eq.${purpose}&used_at=is.null&order=created_at.desc&limit=1&select=*`,
      { method: 'GET' },
    );
    const otp = rows?.[0];
    if (!otp) {
      throw new BadRequestException('No active verification code was found');
    }
    if (new Date(otp.expires_at).getTime() <= Date.now()) {
      throw new BadRequestException('This verification code has expired');
    }
    if (otp.attempt_count >= otp.max_attempts) {
      throw new HttpException(
        'Too many incorrect code attempts. Request a new code.',
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }

    const expected = this.hashOtp(user.id, purpose, String(code).trim());
    if (!this.sameHash(otp.code_hash, expected)) {
      await this.db.adminRest(
        `ul_auth_otp_codes?id=eq.${encodeURIComponent(otp.id)}`,
        {
          method: 'PATCH',
          body: JSON.stringify({ attempt_count: otp.attempt_count + 1 }),
        },
      );
      throw new BadRequestException('Incorrect verification code');
    }

    await this.db.adminRest(
      `ul_auth_otp_codes?id=eq.${encodeURIComponent(otp.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({ used_at: new Date().toISOString() }),
      },
    );
  }

  private async issueSession(user: UniversalLiveUserRow) {
    const accessToken = signAccessToken({
      userId: user.id,
      email: user.email,
      secret: this.accessSecret(),
      ttlMinutes: this.number('JWT_ACCESS_TTL_MINUTES', 15),
    });
    const refreshToken = randomBytes(48).toString('base64url');
    const refreshDays = this.number('REFRESH_TOKEN_TTL_DAYS', 30);

    await this.db.adminRest('ul_auth_refresh_tokens', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        token_hash: this.hashOpaqueToken(refreshToken),
        expires_at: new Date(Date.now() + refreshDays * 86_400_000).toISOString(),
      }),
    });

    return {
      access_token: accessToken,
      refresh_token: refreshToken,
      expires_in: tokenLifetimeSeconds(accessToken),
      token_type: 'bearer',
      user: {
        id: user.id,
        email: user.email,
        email_confirmed_at: user.email_verified_at,
      },
    };
  }

  async register(input: {
    email: string;
    password: string;
    fullName?: string;
    username?: string;
  }) {
    const email = this.normalizeEmail(input.email);
    this.validatePassword(input.password);
    const username = this.normalizeUsername(input.username);
    const fullName = String(input.fullName || '').trim();
    if (fullName.length < 2 || fullName.length > 100) {
      throw new BadRequestException('Full name must be 2-100 characters');
    }

    if (await this.userByEmail(email)) {
      throw new ConflictException('An account with this email already exists');
    }
    if (username) {
      const rows = await this.db.adminRest<UniversalLiveUserRow[]>(
        `ul_users?username=eq.${encodeURIComponent(username)}&select=id`,
        { method: 'GET' },
      );
      if (rows?.length) {
        throw new ConflictException('This username is already taken');
      }
    }

    const passwordHash = await bcrypt.hash(input.password, 12);
    const inserted = await this.db.adminRest<UniversalLiveUserRow[]>('ul_users', {
      method: 'POST',
      body: JSON.stringify({
        email,
        password_hash: passwordHash,
        full_name: fullName,
        username,
      }),
    });
    const user = inserted?.[0];
    if (!user) throw new Error('Account creation did not return the new user');

    await this.bootstrapAccount(user);
    await this.issueOtp(user, 'verify_email');
    await this.audit('auth.registered', user, email);

    return {
      verification_required: true,
      email,
    };
  }

  async signIn(input: { email: string; password: string }) {
    const email = this.normalizeEmail(input.email);
    const user = await this.userByEmail(email);

    if (!user || !user.is_active) {
      await this.audit('auth.login_failed', user, email, { reason: 'invalid_credentials' });
      throw new UnauthorizedException('Invalid email or password');
    }

    if (user.locked_until && new Date(user.locked_until).getTime() > Date.now()) {
      throw new HttpException(
        'Account temporarily locked. Try again later.',
        HttpStatus.TOO_MANY_REQUESTS,
      );
    }

    const valid = await bcrypt.compare(input.password || '', user.password_hash);
    if (!valid) {
      const attempts = user.failed_login_attempts + 1;
      const lockedUntil = attempts >= 5
        ? new Date(Date.now() + 15 * 60_000).toISOString()
        : null;
      await this.db.adminRest(`ul_users?id=eq.${encodeURIComponent(user.id)}`, {
        method: 'PATCH',
        body: JSON.stringify({
          failed_login_attempts: attempts >= 5 ? 0 : attempts,
          locked_until: lockedUntil,
        }),
      });
      await this.audit('auth.login_failed', user, email, { reason: 'invalid_credentials' });
      throw new UnauthorizedException('Invalid email or password');
    }

    if (!user.email_verified_at) {
      await this.audit('auth.login_blocked', user, email, { reason: 'email_unverified' });
      throw new UnauthorizedException('Verify your email before signing in');
    }

    await this.db.adminRest(`ul_users?id=eq.${encodeURIComponent(user.id)}`, {
      method: 'PATCH',
      body: JSON.stringify({
        failed_login_attempts: 0,
        locked_until: null,
        last_login_at: new Date().toISOString(),
      }),
    });

    await this.audit('auth.login_success', user, email);
    return this.issueSession({ ...user, last_login_at: new Date().toISOString() });
  }

  async refresh(refreshToken: string) {
    const token = String(refreshToken || '').trim();
    if (token.length < 32) throw new UnauthorizedException('Invalid refresh token');

    const rows = await this.db.adminRest<RefreshTokenRow[]>(
      `ul_auth_refresh_tokens?token_hash=eq.${encodeURIComponent(this.hashOpaqueToken(token))}&revoked_at=is.null&select=*`,
      { method: 'GET' },
    );
    const stored = rows?.[0];
    if (!stored || new Date(stored.expires_at).getTime() <= Date.now()) {
      throw new UnauthorizedException('Refresh token is invalid or expired');
    }

    await this.db.adminRest(
      `ul_auth_refresh_tokens?id=eq.${encodeURIComponent(stored.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          revoked_at: new Date().toISOString(),
          last_used_at: new Date().toISOString(),
        }),
      },
    );

    const user = await this.userById(stored.user_id);
    if (!user.email_verified_at) throw new UnauthorizedException('Email verification required');
    await this.audit('auth.refresh', user, user.email);
    return this.issueSession(user);
  }

  async resendVerification(emailInput: string) {
    const email = this.normalizeEmail(emailInput);
    const user = await this.userByEmail(email);
    if (!user) return { sent: true };
    if (user.email_verified_at) return { sent: true, already_verified: true };
    await this.issueOtp(user, 'verify_email');
    return { sent: true };
  }

  async verifyEmail(input: { email: string; token: string }) {
    const email = this.normalizeEmail(input.email);
    const user = await this.userByEmail(email);
    if (!user) throw new BadRequestException('Invalid verification request');
    if (!user.email_verified_at) {
      await this.consumeOtp(user, 'verify_email', input.token);
      const now = new Date().toISOString();
      await this.db.adminRest(`ul_users?id=eq.${encodeURIComponent(user.id)}`, {
        method: 'PATCH',
        body: JSON.stringify({ email_verified_at: now }),
      });
      user.email_verified_at = now;
    }
    await this.bootstrapAccount(user);
    await this.audit('auth.email_verified', user, email);
    return this.issueSession(user);
  }

  async requestPasswordReset(emailInput: string) {
    const email = this.normalizeEmail(emailInput);
    const user = await this.userByEmail(email);
    if (user?.is_active) {
      await this.issueOtp(user, 'password_reset');
      await this.audit('auth.password_reset_requested', user, email);
    }
    return { sent: true };
  }

  async verifyRecovery(input: { email: string; token: string }) {
    const email = this.normalizeEmail(input.email);
    const user = await this.userByEmail(email);
    if (!user?.is_active) throw new BadRequestException('Invalid recovery request');

    await this.consumeOtp(user, 'password_reset', input.token);
    const resetToken = signPasswordResetToken({
      userId: user.id,
      email: user.email,
      secret: this.resetSecret(),
      ttlMinutes: this.number('RESET_TOKEN_TTL_MINUTES', 10),
    });
    await this.audit('auth.password_reset_verified', user, email);

    return {
      access_token: resetToken,
      refresh_token: null,
      expires_in: tokenLifetimeSeconds(resetToken),
      token_type: 'bearer',
      user: { id: user.id, email: user.email },
    };
  }

  async updatePassword(bearerToken: string, password: string) {
    this.validatePassword(password);
    const token = String(bearerToken || '').trim();
    if (!token) throw new UnauthorizedException('Bearer token required');

    let userId: string;
    let email: string;
    try {
      const payload = verifyPasswordResetToken(token, this.resetSecret());
      userId = payload.sub;
      email = payload.email;
    } catch {
      const payload = verifyAccessToken(token, this.accessSecret());
      userId = payload.sub;
      email = payload.email;
    }

    const user = await this.userById(userId);
    if (user.email.toLowerCase() !== email.toLowerCase()) {
      throw new UnauthorizedException('Session no longer matches this account');
    }

    const passwordHash = await bcrypt.hash(password, 12);
    const now = new Date().toISOString();
    await this.db.adminRest(`ul_users?id=eq.${encodeURIComponent(user.id)}`, {
      method: 'PATCH',
      body: JSON.stringify({
        password_hash: passwordHash,
        password_changed_at: now,
        failed_login_attempts: 0,
        locked_until: null,
      }),
    });
    await this.db.adminRest(
      `ul_auth_refresh_tokens?user_id=eq.${encodeURIComponent(user.id)}&revoked_at=is.null`,
      {
        method: 'PATCH',
        body: JSON.stringify({ revoked_at: now }),
      },
    );
    await this.audit('auth.password_changed', user, user.email);
    return { updated: true };
  }

  async signOut(accessToken: string, refreshToken?: string) {
    const payload = verifyAccessToken(accessToken, this.accessSecret());
    if (refreshToken) {
      await this.db.adminRest(
        `ul_auth_refresh_tokens?user_id=eq.${encodeURIComponent(payload.sub)}&token_hash=eq.${encodeURIComponent(this.hashOpaqueToken(refreshToken))}&revoked_at=is.null`,
        {
          method: 'PATCH',
          body: JSON.stringify({ revoked_at: new Date().toISOString() }),
        },
      );
    }
    await this.audit('auth.logout', await this.userById(payload.sub), payload.email);
    return { signedOut: true };
  }
}
