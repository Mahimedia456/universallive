import { Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class SettingsService {
  constructor(private readonly db: BackendSupabase) {}

  async get(token: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_user_settings?user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    return rows?.[0] || this.defaults(user.id);
  }

  async update(token: string, body: any) {
    const user = await this.db.currentUser(token);
    const payload: Record<string, unknown> = {
      user_id: user.id,
      updated_at: new Date().toISOString(),
    };
    if (body.notificationsEnabled !== undefined) payload.notifications_enabled = body.notificationsEnabled === true;
    if (body.marketingNotificationsEnabled !== undefined) payload.marketing_notifications_enabled = body.marketingNotificationsEnabled === true;
    if (body.streamDefaults && typeof body.streamDefaults === 'object') payload.stream_defaults = body.streamDefaults;
    if (body.uiConfig && typeof body.uiConfig === 'object') payload.ui_config = body.uiConfig;

    const rows = await this.db.adminRest<any[]>('ul_user_settings?on_conflict=user_id', {
      method: 'POST',
      headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
      body: JSON.stringify(payload),
    });
    return rows?.[0] || this.defaults(user.id);
  }

  async streaming(token: string) {
    const settings = await this.get(token);
    return settings.stream_defaults || {};
  }

  async updateStreaming(token: string, streamDefaults: Record<string, unknown>) {
    return this.update(token, { streamDefaults });
  }

  private defaults(userId: string) {
    return {
      user_id: userId,
      notifications_enabled: true,
      marketing_notifications_enabled: false,
      stream_defaults: {},
      ui_config: {},
    };
  }
}
