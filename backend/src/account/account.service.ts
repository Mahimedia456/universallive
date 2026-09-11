import { BadRequestException, Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class AccountService {
  constructor(private readonly db: BackendSupabase) {}

  async overview(token: string) {
    const user = await this.db.currentUser(token);
    const [profiles, destinations, sessions, scenes] = await Promise.all([
      this.db.adminRest<any[]>(`ul_creator_profiles?user_id=eq.${encodeURIComponent(user.id)}&select=*`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_streaming_connections?user_id=eq.${encodeURIComponent(user.id)}&select=id`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_broadcast_sessions?user_id=eq.${encodeURIComponent(user.id)}&select=id`, { method: 'GET' }),
      this.db.adminRest<any[]>(`ul_scenes?user_id=eq.${encodeURIComponent(user.id)}&select=id`, { method: 'GET' }),
    ]);
    return {
      user: { id: user.id, email: user.email, emailVerifiedAt: user.email_confirmed_at },
      profile: profiles?.[0] || null,
      stats: {
        destinations: destinations?.length || 0,
        streams: sessions?.length || 0,
        scenes: scenes?.length || 0,
      },
    };
  }

  async sessions(token: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_auth_refresh_tokens?user_id=eq.${encodeURIComponent(user.id)}&select=id,user_agent,expires_at,revoked_at,last_used_at,created_at&order=created_at.desc&limit=50`,
      { method: 'GET' },
    );
    const now = Date.now();
    return (rows || []).map((row) => ({
      ...row,
      active: !row.revoked_at && new Date(row.expires_at).getTime() > now,
    }));
  }

  async logoutAll(token: string) {
    const user = await this.db.currentUser(token);
    const now = new Date().toISOString();
    await this.db.adminRest(
      `ul_auth_refresh_tokens?user_id=eq.${encodeURIComponent(user.id)}&revoked_at=is.null`,
      { method: 'PATCH', body: JSON.stringify({ revoked_at: now }) },
    );
    return { revoked: true, at: now };
  }

  async deleteAccount(token: string, confirmation: string) {
    const user = await this.db.currentUser(token);
    if (String(confirmation || '').trim().toUpperCase() !== 'DELETE') {
      throw new BadRequestException('confirmation must be DELETE');
    }
    await this.db.adminRest(`ul_users?id=eq.${encodeURIComponent(user.id)}`, { method: 'DELETE' });
    return { deleted: true };
  }
}
