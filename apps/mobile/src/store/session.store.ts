import { create } from 'zustand';
import { clearAuthTokens, getAuthTokens, saveAuthTokens } from '@/lib/auth-storage';
import { authApi, type AuthSessionResponse, type AuthUser } from '@/services/auth-api';
import { backendApi } from '@/services/backend-api';

export type SessionUser = {
  id: string;
  firstName: string;
  lastName?: string;
  email: string;
};

type SessionState = {
  user: SessionUser | null;
  isAuthenticated: boolean;
  hasHydrated: boolean;
  recoveryEmail: string;
  pendingEmail: string;
  resetToken: string;
  hydrateSession: () => Promise<void>;
  signIn: (email: string, password: string) => Promise<void>;
  signUp: (firstName: string, lastName: string, email: string, password: string) => Promise<{ needsVerification: boolean }>;
  verifySignupOtp: (email: string, token: string) => Promise<void>;
  resendSignupOtp: (email: string) => Promise<void>;
  beginPasswordReset: (email: string) => Promise<void>;
  verifyRecoveryOtp: (email: string, token: string) => Promise<void>;
  resendRecoveryOtp: (email: string) => Promise<void>;
  updatePassword: (password: string) => Promise<void>;
  signOut: () => Promise<void>;
  clearPasswordReset: () => void;
  setHasHydrated: (value: boolean) => void;
};

function mapUser(user: AuthUser): SessionUser {
  return { id: user.id, email: user.email, firstName: user.firstName, ...(user.lastName ? { lastName: user.lastName } : {}) };
}

async function storeSession(session: AuthSessionResponse) {
  await saveAuthTokens({ accessToken: session.accessToken, refreshToken: session.refreshToken });
  return mapUser(session.user);
}

export const useSessionStore = create<SessionState>((set, get) => ({
  user: null,
  isAuthenticated: false,
  hasHydrated: false,
  recoveryEmail: '',
  pendingEmail: '',
  resetToken: '',

  hydrateSession: async () => {
    try {
      const tokens = await getAuthTokens();
      if (!tokens) {
        set({ user: null, isAuthenticated: false });
        return;
      }
      try {
        const me = await backendApi.me();
        set({ user: { id: me.user.id, email: me.user.email, firstName: me.user.firstName, ...(me.user.lastName ? { lastName: me.user.lastName } : {}) }, isAuthenticated: true });
      } catch {
        const refreshed = await authApi.refreshStoredSession();
        set({ user: mapUser(refreshed.user), isAuthenticated: true });
      }
    } catch {
      await clearAuthTokens();
      set({ user: null, isAuthenticated: false });
    } finally {
      set({ hasHydrated: true });
    }
  },

  signIn: async (email, password) => {
    const session = await authApi.login(email.trim().toLowerCase(), password);
    const user = await storeSession(session);
    set({ user, isAuthenticated: true, pendingEmail: '', recoveryEmail: '', resetToken: '' });
  },

  signUp: async (firstName, lastName, email, password) => {
    const normalized = email.trim().toLowerCase();
    const result = await authApi.register({ firstName: firstName.trim(), lastName: lastName.trim() || undefined, email: normalized, password });
    set({ pendingEmail: normalized, user: null, isAuthenticated: false });
    return { needsVerification: result.verificationRequired };
  },

  verifySignupOtp: async (email, token) => {
    const session = await authApi.verifyEmail(email.trim().toLowerCase(), token.trim());
    const user = await storeSession(session);
    set({ user, isAuthenticated: true, pendingEmail: '' });
  },

  resendSignupOtp: async (email) => {
    await authApi.resendVerification(email.trim().toLowerCase());
  },

  beginPasswordReset: async (email) => {
    const normalized = email.trim().toLowerCase();
    await authApi.forgotPassword(normalized);
    set({ recoveryEmail: normalized, resetToken: '' });
  },

  verifyRecoveryOtp: async (email, token) => {
    const result = await authApi.verifyResetOtp(email.trim().toLowerCase(), token.trim());
    set({ resetToken: result.resetToken });
  },

  resendRecoveryOtp: async (email) => {
    await authApi.resendPasswordReset(email.trim().toLowerCase());
  },

  updatePassword: async (password) => {
    const resetToken = get().resetToken;
    if (!resetToken) throw new Error('Verify your recovery code first.');
    await authApi.resetPassword(resetToken, password);
    await clearAuthTokens();
    set({ user: null, isAuthenticated: false, recoveryEmail: '', resetToken: '' });
  },

  signOut: async () => {
    await authApi.logout();
    set({ user: null, isAuthenticated: false, recoveryEmail: '', pendingEmail: '', resetToken: '' });
  },

  clearPasswordReset: () => set({ recoveryEmail: '', resetToken: '' }),
  setHasHydrated: (hasHydrated) => set({ hasHydrated }),
}));
