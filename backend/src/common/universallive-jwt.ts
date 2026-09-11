import {
  UnauthorizedException,
} from '@nestjs/common';
import {
  createHmac,
  timingSafeEqual,
} from 'crypto';

export type UniversalLiveTokenType = 'access' | 'password_reset';

export type UniversalLiveJwtPayload = {
  sub: string;
  email: string;
  typ: UniversalLiveTokenType;
  iat: number;
  exp: number;
  iss: string;
  aud: string;
};

const ISSUER = 'universallive-backend';
const AUDIENCE = 'universallive-mobile';

function checkedSecret(value: string | undefined, name: string): string {
  const normalized = String(value || '').trim();
  if (normalized.length < 32) {
    throw new Error(`${name} must contain at least 32 characters`);
  }
  return normalized;
}

function encode(value: unknown): string {
  return Buffer.from(JSON.stringify(value), 'utf8').toString('base64url');
}

function decode<T>(value: string): T {
  return JSON.parse(Buffer.from(value, 'base64url').toString('utf8')) as T;
}

function signature(input: string, secret: string): string {
  return createHmac('sha256', secret).update(input).digest('base64url');
}

function sign(input: {
  userId: string;
  email: string;
  typ: UniversalLiveTokenType;
  secret: string | undefined;
  ttlMinutes: number;
  secretName: string;
}): string {
  const now = Math.floor(Date.now() / 1000);
  const payload: UniversalLiveJwtPayload = {
    sub: input.userId,
    email: input.email,
    typ: input.typ,
    iat: now,
    exp: now + Math.max(60, Math.floor(input.ttlMinutes * 60)),
    iss: ISSUER,
    aud: AUDIENCE,
  };
  const header = encode({ alg: 'HS256', typ: 'JWT' });
  const body = encode(payload);
  const unsigned = `${header}.${body}`;
  return `${unsigned}.${signature(unsigned, checkedSecret(input.secret, input.secretName))}`;
}

export function signAccessToken(input: {
  userId: string;
  email: string;
  secret: string | undefined;
  ttlMinutes: number;
}): string {
  return sign({
    ...input,
    typ: 'access',
    secretName: 'JWT_ACCESS_SECRET',
  });
}

export function signPasswordResetToken(input: {
  userId: string;
  email: string;
  secret: string | undefined;
  ttlMinutes: number;
}): string {
  return sign({
    ...input,
    typ: 'password_reset',
    secretName: 'JWT_RESET_SECRET',
  });
}

function verify(
  token: string,
  secretValue: string | undefined,
  secretName: string,
): UniversalLiveJwtPayload {
  try {
    const parts = String(token || '').split('.');
    if (parts.length !== 3) throw new Error('Malformed token');
    const [headerPart, payloadPart, signaturePart] = parts;
    const header = decode<{ alg?: string; typ?: string }>(headerPart);
    if (header.alg !== 'HS256' || header.typ !== 'JWT') throw new Error('Unexpected JWT header');

    const unsigned = `${headerPart}.${payloadPart}`;
    const expected = Buffer.from(
      signature(unsigned, checkedSecret(secretValue, secretName)),
      'utf8',
    );
    const actual = Buffer.from(signaturePart, 'utf8');
    if (expected.length !== actual.length || !timingSafeEqual(expected, actual)) {
      throw new Error('Invalid signature');
    }

    const payload = decode<UniversalLiveJwtPayload>(payloadPart);
    const now = Math.floor(Date.now() / 1000);
    if (!payload.sub || !payload.email || !payload.typ) throw new Error('Incomplete token');
    if (payload.iss !== ISSUER || payload.aud !== AUDIENCE) throw new Error('Invalid token audience');
    if (!Number.isFinite(payload.exp) || payload.exp <= now) throw new Error('Expired token');
    return payload;
  } catch (error) {
    if (error instanceof UnauthorizedException) throw error;
    throw new UnauthorizedException('Invalid or expired session');
  }
}

export function verifyAccessToken(
  token: string,
  accessSecret: string | undefined,
): UniversalLiveJwtPayload {
  const payload = verify(token, accessSecret, 'JWT_ACCESS_SECRET');
  if (payload.typ !== 'access') {
    throw new UnauthorizedException('Access token required');
  }
  return payload;
}

export function verifyPasswordResetToken(
  token: string,
  resetSecret: string | undefined,
): UniversalLiveJwtPayload {
  try {
    const payload = verify(token, resetSecret, 'JWT_RESET_SECRET');
    if (payload.typ !== 'password_reset') {
      throw new UnauthorizedException('Password reset token required');
    }
    return payload;
  } catch (error) {
    if (error instanceof UnauthorizedException) {
      throw new UnauthorizedException('Password reset session expired');
    }
    throw error;
  }
}

export function tokenLifetimeSeconds(token: string): number | null {
  try {
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    const payload = decode<UniversalLiveJwtPayload>(parts[1]);
    return Math.max(0, payload.exp - payload.iat);
  } catch {
    return null;
  }
}
