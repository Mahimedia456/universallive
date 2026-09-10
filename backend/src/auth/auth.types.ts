import type { Request } from 'express';

export type SizeMeUserRow = {
  id: string;
  email: string;
  password_hash: string;
  first_name: string;
  last_name: string | null;
  display_name: string;
  email_verified_at: string | null;
  password_changed_at: string;
  is_active: boolean;
  last_login_at: string | null;
  failed_login_attempts: number;
  locked_until: string | null;
  created_at: string;
  updated_at: string;
};

export type AuthenticatedUserContext = {
  id: string;
  email: string;
  role: 'user';
  firstName: string;
  lastName?: string;
};

export type AuthenticatedRequest = Request & {
  profileId?: string;
  userId?: string;
  authUser?: AuthenticatedUserContext;
  accessToken?: string;
};

export type AccessTokenPayload = {
  sub: string;
  email: string;
  typ: 'access';
  iat: number;
  exp: number;
  iss: string;
  aud: string;
};

export type ResetTokenPayload = {
  sub: string;
  email: string;
  typ: 'password_reset';
  iat: number;
  exp: number;
  iss: string;
  aud: string;
};
