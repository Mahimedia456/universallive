import { Injectable } from '@nestjs/common';
import { SupabaseRestClient } from '../common/supabase-rest';

@Injectable()
export class DevicesService {
  constructor(private readonly supabase: SupabaseRestClient) {}

  private async user(accessToken: string): Promise<any> {
    return this.supabase.authRequest<any>('user', {
      method: 'GET',
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  async registerDevice(
    accessToken: string,
    body: {
      deviceId: string;
      platform: string;
      pushToken?: string | null;
      appVersion?: string | null;
      osVersion?: string | null;
      deviceModel?: string | null;
    },
  ) {
    const user = await this.user(accessToken);
    return this.supabase.restRequest<any[]>(
      'ul_devices?on_conflict=user_id,device_id',
      accessToken,
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          device_id: body.deviceId,
          platform: body.platform,
          push_token: body.pushToken,
          app_version: body.appVersion,
          os_version: body.osVersion,
          device_model: body.deviceModel,
          last_seen_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        }),
      },
    );
  }

  async getOnboarding(accessToken: string) {
    const user = await this.user(accessToken);
    const rows = await this.supabase.restRequest<any[]>(
      `ul_onboarding_state?user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      accessToken,
      { method: 'GET' },
    );
    return rows?.[0] || { user_id: user.id };
  }

  async updateOnboarding(accessToken: string, body: Record<string, unknown>) {
    const user = await this.user(accessToken);
    return this.supabase.restRequest<any[]>(
      'ul_onboarding_state?on_conflict=user_id',
      accessToken,
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          ...body,
          updated_at: new Date().toISOString(),
        }),
      },
    );
  }
}
