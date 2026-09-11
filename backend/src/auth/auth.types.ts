import type { Request } from 'express';

export type UniversalLiveUserRow = {
  id: string;
  email: string;
  password_hash: string;
  full_name: string | null;
  username: string | null;
  email_verified_at: string | null;
  password_changed_at: string;
  is_active: boolean;
  last_login_at: string | null;
  failed_login_attempts: number;
  locked_until: string | null;
  created_at: string;
  updated_at: string;
};

export type UniversalLiveAuthUser = {
  id: string;
  email: string;
  phone: null;
  created_at: string;
  email_confirmed_at: string | null;
  user_metadata: {
    full_name: string | null;
    username: string | null;
  };
};

export type AuthenticatedRequest = Request & {
  user?: UniversalLiveAuthUser;
  userId?: string;
  accessToken?: string;
};
