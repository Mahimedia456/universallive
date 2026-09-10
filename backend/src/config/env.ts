export type AppEnv = {
  NODE_ENV: 'development' | 'test' | 'production';
  PORT: number;
  API_PREFIX: string;
  CORS_ORIGINS: string;
  SUPABASE_URL: string;
  SUPABASE_SECRET_KEY: string;
  SUPABASE_DB_PASSWORD?: string;
  AUTH_MODE: 'sizeme-db';
  JWT_ACCESS_SECRET: string;
  JWT_RESET_SECRET: string;
  OTP_PEPPER: string;
  JWT_ACCESS_TTL_MINUTES: number;
  REFRESH_TOKEN_TTL_DAYS: number;
  RESET_TOKEN_TTL_MINUTES: number;
  OTP_TTL_MINUTES: number;
  OTP_RESEND_SECONDS: number;
  EMAIL_MODE: 'smtp' | 'console';
  SMTP_HOST?: string;
  SMTP_PORT: number;
  SMTP_SECURE: boolean;
  SMTP_USER?: string;
  SMTP_PASS?: string;
  SMTP_FROM_EMAIL: string;
  SMTP_FROM_NAME: string;
};

function positiveInteger(raw: unknown, fallback: number, name: string): number {
  const value = Number(raw ?? fallback);
  if (!Number.isInteger(value) || value <= 0) throw new Error(`${name} must be a positive integer`);
  return value;
}

function secret(raw: unknown, name: string): string {
  const value = String(raw ?? '').trim();
  if (value.length < 32) throw new Error(`${name} must contain at least 32 characters`);
  return value;
}

export function validateEnv(raw: Record<string, unknown>): AppEnv {
  const nodeEnv = String(raw.NODE_ENV ?? 'development') as AppEnv['NODE_ENV'];
  if (!['development', 'test', 'production'].includes(nodeEnv)) throw new Error('NODE_ENV must be development, test or production');

  const port = Number(raw.PORT ?? 3000);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('PORT must be a valid TCP port');

  const supabaseUrl = String(raw.SUPABASE_URL ?? '').trim();
  const secretKey = String(raw.SUPABASE_SECRET_KEY ?? '').trim();
  if (!/^https:\/\/[a-z0-9-]+\.supabase\.co$/i.test(supabaseUrl)) throw new Error('SUPABASE_URL is missing or invalid');
  if (!secretKey.startsWith('sb_secret_')) throw new Error('SUPABASE_SECRET_KEY is missing or invalid');

  const authMode = String(raw.AUTH_MODE ?? 'sizeme-db');
  if (authMode !== 'sizeme-db') throw new Error('AUTH_MODE must be sizeme-db');

  const emailMode = String(raw.EMAIL_MODE ?? 'console') as AppEnv['EMAIL_MODE'];
  if (!['smtp', 'console'].includes(emailMode)) throw new Error('EMAIL_MODE must be smtp or console');
  if (nodeEnv === 'production' && emailMode !== 'smtp') throw new Error('EMAIL_MODE=smtp is required in production');

  const smtpHost = String(raw.SMTP_HOST ?? '').trim() || undefined;
  const smtpUser = String(raw.SMTP_USER ?? '').trim() || undefined;
  const smtpPass = String(raw.SMTP_PASS ?? '').trim() || undefined;
  if (emailMode === 'smtp' && (!smtpHost || !smtpUser || !smtpPass)) throw new Error('SMTP_HOST, SMTP_USER and SMTP_PASS are required when EMAIL_MODE=smtp');

  return {
    NODE_ENV: nodeEnv,
    PORT: port,
    API_PREFIX: String(raw.API_PREFIX ?? 'api/v1').replace(/^\/+|\/+$/g, ''),
    CORS_ORIGINS: String(raw.CORS_ORIGINS ?? '*'),
    SUPABASE_URL: supabaseUrl,
    SUPABASE_SECRET_KEY: secretKey,
    SUPABASE_DB_PASSWORD: String(raw.SUPABASE_DB_PASSWORD ?? '') || undefined,
    AUTH_MODE: 'sizeme-db',
    JWT_ACCESS_SECRET: secret(raw.JWT_ACCESS_SECRET, 'JWT_ACCESS_SECRET'),
    JWT_RESET_SECRET: secret(raw.JWT_RESET_SECRET, 'JWT_RESET_SECRET'),
    OTP_PEPPER: secret(raw.OTP_PEPPER, 'OTP_PEPPER'),
    JWT_ACCESS_TTL_MINUTES: positiveInteger(raw.JWT_ACCESS_TTL_MINUTES, 15, 'JWT_ACCESS_TTL_MINUTES'),
    REFRESH_TOKEN_TTL_DAYS: positiveInteger(raw.REFRESH_TOKEN_TTL_DAYS, 30, 'REFRESH_TOKEN_TTL_DAYS'),
    RESET_TOKEN_TTL_MINUTES: positiveInteger(raw.RESET_TOKEN_TTL_MINUTES, 10, 'RESET_TOKEN_TTL_MINUTES'),
    OTP_TTL_MINUTES: positiveInteger(raw.OTP_TTL_MINUTES, 10, 'OTP_TTL_MINUTES'),
    OTP_RESEND_SECONDS: positiveInteger(raw.OTP_RESEND_SECONDS, 60, 'OTP_RESEND_SECONDS'),
    EMAIL_MODE: emailMode,
    SMTP_HOST: smtpHost,
    SMTP_PORT: positiveInteger(raw.SMTP_PORT, 587, 'SMTP_PORT'),
    SMTP_SECURE: String(raw.SMTP_SECURE ?? 'false').toLowerCase() === 'true',
    SMTP_USER: smtpUser,
    SMTP_PASS: smtpPass,
    SMTP_FROM_EMAIL: String(raw.SMTP_FROM_EMAIL ?? 'no-reply@sizeme.app').trim(),
    SMTP_FROM_NAME: String(raw.SMTP_FROM_NAME ?? 'SizeME').trim(),
  };
}
