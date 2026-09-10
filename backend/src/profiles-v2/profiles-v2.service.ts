import { Injectable } from '@nestjs/common';
import { SupabaseRestClient } from '../common/supabase-rest';

@Injectable()
export class ProfilesV2Service {
  constructor(private readonly supabase: SupabaseRestClient) {}

  async currentUser(accessToken: string): Promise<any> {
    return this.supabase.authRequest<any>('user', {
      method: 'GET',
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  async getProfile(accessToken: string) {
    const user = await this.currentUser(accessToken);
    const rows = await this.supabase.restRequest<any[]>(
      `ul_creator_profiles?user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      accessToken,
      { method: 'GET' },
    );

    return rows?.[0] || {
      user_id: user.id,
      display_name: user.user_metadata?.full_name || null,
      username: user.user_metadata?.username || null,
      avatar_url: null,
      creator_type: null,
      onboarding_completed: false,
    };
  }

  async upsertProfile(
    accessToken: string,
    input: {
      displayName?: string;
      username?: string;
      avatarUrl?: string | null;
      creatorType?: string | null;
      onboardingCompleted?: boolean;
      locale?: string | null;
      timezone?: string | null;
    },
  ) {
    const user = await this.currentUser(accessToken);
    const rows = await this.supabase.restRequest<any[]>(
      'ul_creator_profiles?on_conflict=user_id',
      accessToken,
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          display_name: input.displayName,
          username: input.username,
          avatar_url: input.avatarUrl,
          creator_type: input.creatorType,
          onboarding_completed: input.onboardingCompleted ?? false,
          locale: input.locale,
          timezone: input.timezone,
          updated_at: new Date().toISOString(),
        }),
      },
    );
    return rows?.[0] || null;
  }
}
