import { BadRequestException, Injectable } from '@nestjs/common';
import { createHash, randomBytes } from 'crypto';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class PlatformOauthService {
  constructor(private readonly db: BackendSupabase) {}

  async begin(token: string, platform: string, redirectUri?: string) {
    const user = await this.db.currentUser(token);
    const normalized = platform.trim().toLowerCase();

    if (!['youtube', 'facebook', 'twitch'].includes(normalized)) {
      throw new BadRequestException('Unsupported OAuth platform');
    }

    const rawState = randomBytes(32).toString('hex');
    const stateHash = createHash('sha256').update(rawState).digest('hex');
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();

    await this.db.adminRest(
      'ul_oauth_states',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          platform: normalized,
          state_hash: stateHash,
          redirect_uri: redirectUri || null,
          expires_at: expiresAt,
        }),
      },
    );

    return {
      platform: normalized,
      state: rawState,
      expiresAt,
      authorizationUrl: null,
      requiresProviderConfiguration: true,
    };
  }

  async channels(token: string, platform: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_platform_channels?user_id=eq.${encodeURIComponent(user.id)}&platform=eq.${encodeURIComponent(platform.toLowerCase())}&select=*&order=channel_name.asc`,
      { method: 'GET' },
    );
  }

  async selectChannel(token: string, connectionId: string, channelId: string) {
    const user = await this.db.currentUser(token);

    const channelRows = await this.db.adminRest<any[]>(
      `ul_platform_channels?id=eq.${encodeURIComponent(channelId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    if (!channelRows?.length) throw new BadRequestException('Channel not found');

    const channel = channelRows[0];

    const rows = await this.db.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          external_channel_id: channel.external_channel_id,
          external_channel_name: channel.channel_name,
          status: channel.can_stream ? 'connected' : 'needs_attention',
          updated_at: new Date().toISOString(),
        }),
      },
    );

    return rows?.[0] || null;
  }
}
