import {
  BadRequestException,
  Injectable,
  InternalServerErrorException,
} from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createHash, randomBytes } from 'crypto';
import { BackendSupabase } from '../common/backend-supabase';
import { CredentialCryptoService } from '../rtmp-credentials/credential-crypto.service';

type SupportedPlatform = 'youtube' | 'facebook' | 'twitch' | 'tiktok';

type OAuthCallbackInput = {
  code?: string;
  state?: string;
  error?: string;
  errorDescription?: string;
};

type ProviderTokens = {
  accessToken: string;
  refreshToken?: string | null;
  expiresIn?: number | null;
  scope?: string | null;
};

type ProviderProfile = {
  externalId: string;
  displayName: string;
  handle?: string | null;
  avatarUrl?: string | null;
  metadata?: Record<string, unknown>;
};

@Injectable()
export class PlatformOauthService {
  constructor(
    private readonly db: BackendSupabase,
    private readonly config: ConfigService,
    private readonly crypto: CredentialCryptoService,
  ) {}

  async begin(token: string, rawPlatform: string) {
    const user = await this.db.currentUser(token);
    const platform = this.platform(rawPlatform);
    const provider = this.providerConfig(platform);

    const rawState = randomBytes(32).toString('hex');
    const stateHash = createHash('sha256').update(rawState).digest('hex');
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();
    const callbackUrl = this.callbackUrl(platform);

    await this.db.adminRest('ul_oauth_states', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        platform,
        state_hash: stateHash,
        redirect_uri: callbackUrl,
        status: 'pending',
        expires_at: expiresAt,
      }),
    });

    const authorizationUrl = this.authorizationUrl(
      platform,
      provider.clientId,
      callbackUrl,
      rawState,
    );

    return {
      platform,
      authorizationUrl,
      expiresAt,
      configured: true,
    };
  }

  async status(token: string, rawPlatform: string) {
    const user = await this.db.currentUser(token);
    const platform = this.platform(rawPlatform);

    const [credentials, connections] = await Promise.all([
      this.db.adminRest<any[]>(
        `ul_oauth_credentials?user_id=eq.${encodeURIComponent(user.id)}&platform=eq.${encodeURIComponent(platform)}&select=id,connection_id,provider_user_id,provider_display_name,expires_at,scope,updated_at&order=updated_at.desc&limit=1`,
        { method: 'GET' },
      ),
      this.db.adminRest<any[]>(
        `ul_streaming_connections?user_id=eq.${encodeURIComponent(user.id)}&platform=eq.${encodeURIComponent(platform)}&select=id,display_name,status,is_enabled,last_error_message&order=updated_at.desc&limit=1`,
        { method: 'GET' },
      ),
    ]);

    const credential = credentials?.[0];
    const connection = connections?.[0];
    const publishReady = connection
      ? await this.hasRtmpCredential(user.id, connection.id)
      : false;

    return {
      platform,
      connected: !!credential,
      connectionId: connection?.id || credential?.connection_id || null,
      displayName:
        connection?.display_name || credential?.provider_display_name || null,
      publishReady,
      status: connection?.status || (credential ? 'connected' : 'disconnected'),
      message: credential
        ? publishReady
          ? `${this.label(platform)} account connected and publish route is ready.`
          : `${this.label(platform)} account connected. Use Custom RTMP if this provider does not expose an ingest route to your app.`
        : `${this.label(platform)} is not connected.`,
    };
  }

  async callback(rawPlatform: string, input: OAuthCallbackInput) {
    const platform = this.platform(rawPlatform);
    if (!input.state) throw new BadRequestException('OAuth state is missing');

    const stateHash = createHash('sha256').update(input.state).digest('hex');
    const states = await this.db.adminRest<any[]>(
      `ul_oauth_states?state_hash=eq.${encodeURIComponent(stateHash)}&platform=eq.${encodeURIComponent(platform)}&select=*&limit=1`,
      { method: 'GET' },
    );
    const state = states?.[0];
    if (!state) throw new BadRequestException('OAuth state is invalid');
    if (state.consumed_at) throw new BadRequestException('OAuth state has already been used');
    if (new Date(state.expires_at).getTime() <= Date.now()) {
      throw new BadRequestException('OAuth state has expired');
    }

    if (input.error) {
      await this.finishState(state.id, 'failed', input.errorDescription || input.error);
      throw new BadRequestException(input.errorDescription || input.error);
    }
    if (!input.code) throw new BadRequestException('Authorization code is missing');

    const tokens = await this.exchangeCode(
      platform,
      input.code,
      state.redirect_uri || this.callbackUrl(platform),
    );
    const profile = await this.fetchProfile(platform, tokens.accessToken);

    const connection = await this.upsertConnection(
      state.user_id,
      platform,
      profile,
    );

    await this.saveOAuthCredential(
      state.user_id,
      connection.id,
      platform,
      profile,
      tokens,
    );

    await this.upsertChannel(state.user_id, connection.id, platform, profile);

    let publishReady = false;
    let provisionMessage: string | null = null;
    try {
      if (platform === 'twitch') {
        await this.provisionTwitchRoute(
          state.user_id,
          connection.id,
          profile.externalId,
          tokens.accessToken,
        );
        publishReady = true;
      } else if (platform === 'youtube') {
        publishReady = await this.provisionYouTubeRoute(
          state.user_id,
          connection.id,
          tokens.accessToken,
        );
      }
    } catch (err: any) {
      provisionMessage = err?.message || 'Account linked, but the publish route could not be created.';
    }

    const now = new Date().toISOString();
    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(connection.id)}&user_id=eq.${encodeURIComponent(state.user_id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: publishReady ? 'connected' : 'needs_attention',
          last_health_status: publishReady ? 'ready' : 'oauth_connected',
          last_health_checked_at: now,
          last_error_code: publishReady ? null : 'OAUTH_ROUTE_NOT_READY',
          last_error_message: publishReady
            ? null
            : provisionMessage || 'OAuth account is linked, but a publish ingest route is not available yet.',
          updated_at: now,
        }),
      },
    );

    await this.finishState(state.id, 'completed', null, connection.id);

    return {
      platform,
      displayName: profile.displayName,
      connectionId: connection.id,
      publishReady,
      message: publishReady
        ? `${this.label(platform)} is connected and ready to stream.`
        : `${this.label(platform)} account is connected. You can use Custom RTMP for publishing if required.`,
    };
  }

  async disconnect(token: string, rawPlatform: string) {
    const user = await this.db.currentUser(token);
    const platform = this.platform(rawPlatform);

    const credentials = await this.db.adminRest<any[]>(
      `ul_oauth_credentials?user_id=eq.${encodeURIComponent(user.id)}&platform=eq.${encodeURIComponent(platform)}&select=id,connection_id`,
      { method: 'GET' },
    );

    for (const row of credentials || []) {
      await this.db.adminRest(
        `ul_oauth_credentials?id=eq.${encodeURIComponent(row.id)}&user_id=eq.${encodeURIComponent(user.id)}`,
        { method: 'DELETE' },
      );
      if (row.connection_id) {
        await this.db.adminRest(
          `ul_streaming_connections?id=eq.${encodeURIComponent(row.connection_id)}&user_id=eq.${encodeURIComponent(user.id)}`,
          {
            method: 'PATCH',
            body: JSON.stringify({
              status: 'disconnected',
              last_health_status: 'needs_setup',
              last_error_code: 'OAUTH_DISCONNECTED',
              last_error_message: 'Platform account disconnected',
              updated_at: new Date().toISOString(),
            }),
          },
        );
      }
    }

    return { platform, disconnected: true };
  }

  async channels(token: string, rawPlatform: string) {
    const user = await this.db.currentUser(token);
    const platform = this.platform(rawPlatform);
    return this.db.adminRest<any[]>(
      `ul_platform_channels?user_id=eq.${encodeURIComponent(user.id)}&platform=eq.${encodeURIComponent(platform)}&select=*&order=channel_name.asc`,
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
          updated_at: new Date().toISOString(),
        }),
      },
    );

    return rows?.[0] || null;
  }

  successHtml(result: any): string {
    return this.resultHtml(
      'Connected',
      `${escapeHtml(result.displayName || result.platform)} is linked to Universal Live.`,
      result.publishReady
        ? 'Your publish route is ready. Return to Universal Live and tap Check Connection.'
        : 'Return to Universal Live and tap Check Connection. Custom RTMP remains available for this platform.',
      true,
    );
  }

  errorHtml(message: string): string {
    return this.resultHtml(
      'Connection failed',
      escapeHtml(message),
      'Return to Universal Live and try again, or use Custom RTMP.',
      false,
    );
  }

  private resultHtml(title: string, line1: string, line2: string, ok: boolean): string {
    const accent = ok ? '#14d8e5' : '#f5b942';
    return `<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1"><title>${title}</title></head><body style="margin:0;background:#05070a;color:#fff;font-family:system-ui,-apple-system,sans-serif;display:grid;place-items:center;min-height:100vh"><main style="max-width:520px;padding:32px;text-align:center"><div style="font-size:14px;letter-spacing:.18em;color:${accent};font-weight:800">UNIVERSAL LIVE</div><h1 style="font-size:32px;margin:18px 0 10px">${title}</h1><p style="color:#c1c7ce;line-height:1.6">${line1}</p><p style="color:#8c969f;line-height:1.6">${line2}</p></main></body></html>`;
  }

  private platform(raw: string): SupportedPlatform {
    const value = String(raw || '').trim().toLowerCase();
    if (!['youtube', 'facebook', 'twitch', 'tiktok'].includes(value)) {
      throw new BadRequestException('Unsupported OAuth platform');
    }
    return value as SupportedPlatform;
  }

  private label(platform: SupportedPlatform): string {
    return {
      youtube: 'YouTube',
      facebook: 'Facebook',
      twitch: 'Twitch',
      tiktok: 'TikTok',
    }[platform];
  }

  private callbackUrl(platform: SupportedPlatform): string {
    const base = (
      this.config.get<string>('OAUTH_CALLBACK_BASE_URL') ||
      this.config.get<string>('PUBLIC_API_BASE_URL') ||
      ''
    ).replace(/\/+$/, '');
    if (!base) {
      throw new InternalServerErrorException(
        'OAUTH_CALLBACK_BASE_URL is required for platform OAuth',
      );
    }
    if (base.endsWith('/streaming/oauth')) {
      return `${base}/${platform}/callback`;
    }
    return `${base}/streaming/oauth/${platform}/callback`;
  }

  private providerConfig(platform: SupportedPlatform) {
    const env = (key: string) => this.config.get<string>(key)?.trim();
    const map = {
      youtube: {
        clientId: env('YOUTUBE_OAUTH_CLIENT_ID'),
        clientSecret: env('YOUTUBE_OAUTH_CLIENT_SECRET'),
      },
      facebook: {
        clientId: env('FACEBOOK_OAUTH_CLIENT_ID'),
        clientSecret: env('FACEBOOK_OAUTH_CLIENT_SECRET'),
      },
      twitch: {
        clientId: env('TWITCH_OAUTH_CLIENT_ID'),
        clientSecret: env('TWITCH_OAUTH_CLIENT_SECRET'),
      },
      tiktok: {
        clientId: env('TIKTOK_OAUTH_CLIENT_KEY'),
        clientSecret: env('TIKTOK_OAUTH_CLIENT_SECRET'),
      },
    } as const;
    const value = map[platform];
    if (!value.clientId || !value.clientSecret) {
      throw new BadRequestException(
        `${this.label(platform)} OAuth is not configured on the backend`,
      );
    }
    return value as { clientId: string; clientSecret: string };
  }

  private authorizationUrl(
    platform: SupportedPlatform,
    clientId: string,
    redirectUri: string,
    state: string,
  ): string {
    if (platform === 'youtube') {
      const q = new URLSearchParams({
        client_id: clientId,
        redirect_uri: redirectUri,
        response_type: 'code',
        scope: 'https://www.googleapis.com/auth/youtube',
        access_type: 'offline',
        include_granted_scopes: 'true',
        prompt: 'consent',
        state,
      });
      return `https://accounts.google.com/o/oauth2/v2/auth?${q}`;
    }
    if (platform === 'twitch') {
      const q = new URLSearchParams({
        client_id: clientId,
        redirect_uri: redirectUri,
        response_type: 'code',
        scope: 'user:read:email channel:read:stream_key',
        force_verify: 'true',
        state,
      });
      return `https://id.twitch.tv/oauth2/authorize?${q}`;
    }
    if (platform === 'facebook') {
      const graphVersion = this.config.get<string>('FACEBOOK_GRAPH_VERSION') || 'v23.0';
      const q = new URLSearchParams({
        client_id: clientId,
        redirect_uri: redirectUri,
        response_type: 'code',
        scope: 'public_profile,email,pages_show_list',
        state,
      });
      return `https://www.facebook.com/${graphVersion}/dialog/oauth?${q}`;
    }
    const q = new URLSearchParams({
      client_key: clientId,
      redirect_uri: redirectUri,
      response_type: 'code',
      scope: 'user.info.basic',
      state,
    });
    return `https://www.tiktok.com/v2/auth/authorize/?${q}`;
  }

  private async exchangeCode(
    platform: SupportedPlatform,
    code: string,
    redirectUri: string,
  ): Promise<ProviderTokens> {
    const provider = this.providerConfig(platform);

    if (platform === 'youtube') {
      return this.tokenRequest(
        'https://oauth2.googleapis.com/token',
        new URLSearchParams({
          client_id: provider.clientId,
          client_secret: provider.clientSecret,
          code,
          grant_type: 'authorization_code',
          redirect_uri: redirectUri,
        }),
      );
    }
    if (platform === 'twitch') {
      return this.tokenRequest(
        'https://id.twitch.tv/oauth2/token',
        new URLSearchParams({
          client_id: provider.clientId,
          client_secret: provider.clientSecret,
          code,
          grant_type: 'authorization_code',
          redirect_uri: redirectUri,
        }),
      );
    }
    if (platform === 'tiktok') {
      return this.tokenRequest(
        'https://open.tiktokapis.com/v2/oauth/token/',
        new URLSearchParams({
          client_key: provider.clientId,
          client_secret: provider.clientSecret,
          code,
          grant_type: 'authorization_code',
          redirect_uri: redirectUri,
        }),
      );
    }

    const graphVersion = this.config.get<string>('FACEBOOK_GRAPH_VERSION') || 'v23.0';
    const q = new URLSearchParams({
      client_id: provider.clientId,
      client_secret: provider.clientSecret,
      code,
      redirect_uri: redirectUri,
    });
    const response = await fetch(
      `https://graph.facebook.com/${graphVersion}/oauth/access_token?${q}`,
    );
    const payload: any = await response.json().catch(() => ({}));
    if (!response.ok || !payload.access_token) {
      throw new BadRequestException(payload?.error?.message || 'Facebook token exchange failed');
    }
    return {
      accessToken: payload.access_token,
      expiresIn: Number(payload.expires_in || 0) || null,
      scope: null,
    };
  }

  private async tokenRequest(url: string, body: URLSearchParams): Promise<ProviderTokens> {
    const response = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body,
    });
    const payload: any = await response.json().catch(() => ({}));
    if (!response.ok || !payload.access_token) {
      throw new BadRequestException(
        payload?.error_description || payload?.message || payload?.error || 'OAuth token exchange failed',
      );
    }
    return {
      accessToken: payload.access_token,
      refreshToken: payload.refresh_token || null,
      expiresIn: Number(payload.expires_in || 0) || null,
      scope: Array.isArray(payload.scope)
        ? payload.scope.join(' ')
        : payload.scope || null,
    };
  }

  private async fetchProfile(
    platform: SupportedPlatform,
    accessToken: string,
  ): Promise<ProviderProfile> {
    if (platform === 'youtube') {
      const response = await fetch(
        'https://www.googleapis.com/youtube/v3/channels?part=id,snippet&mine=true',
        { headers: { Authorization: `Bearer ${accessToken}` } },
      );
      const payload: any = await response.json().catch(() => ({}));
      const channel = payload?.items?.[0];
      if (!response.ok || !channel?.id) {
        throw new BadRequestException(payload?.error?.message || 'No YouTube channel found for this account');
      }
      return {
        externalId: channel.id,
        displayName: channel.snippet?.title || 'YouTube Channel',
        handle: channel.snippet?.customUrl || null,
        avatarUrl: channel.snippet?.thumbnails?.default?.url || null,
      };
    }

    if (platform === 'twitch') {
      const provider = this.providerConfig(platform);
      const response = await fetch('https://api.twitch.tv/helix/users', {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Client-Id': provider.clientId,
        },
      });
      const payload: any = await response.json().catch(() => ({}));
      const user = payload?.data?.[0];
      if (!response.ok || !user?.id) {
        throw new BadRequestException(payload?.message || 'No Twitch user found');
      }
      return {
        externalId: user.id,
        displayName: user.display_name || user.login || 'Twitch Channel',
        handle: user.login || null,
        avatarUrl: user.profile_image_url || null,
      };
    }

    if (platform === 'tiktok') {
      const response = await fetch(
        'https://open.tiktokapis.com/v2/user/info/?fields=open_id,union_id,avatar_url,display_name,username',
        { headers: { Authorization: `Bearer ${accessToken}` } },
      );
      const payload: any = await response.json().catch(() => ({}));
      const user = payload?.data?.user;
      if (!response.ok || !user?.open_id) {
        throw new BadRequestException(payload?.error?.message || 'No TikTok user found');
      }
      return {
        externalId: user.open_id,
        displayName: user.display_name || user.username || 'TikTok Account',
        handle: user.username || null,
        avatarUrl: user.avatar_url || null,
        metadata: { unionId: user.union_id || null },
      };
    }

    const graphVersion = this.config.get<string>('FACEBOOK_GRAPH_VERSION') || 'v23.0';
    const response = await fetch(
      `https://graph.facebook.com/${graphVersion}/me?fields=id,name,picture&access_token=${encodeURIComponent(accessToken)}`,
    );
    const payload: any = await response.json().catch(() => ({}));
    if (!response.ok || !payload?.id) {
      throw new BadRequestException(payload?.error?.message || 'No Facebook account found');
    }
    return {
      externalId: payload.id,
      displayName: payload.name || 'Facebook Account',
      avatarUrl: payload.picture?.data?.url || null,
    };
  }

  private async upsertConnection(
    userId: string,
    platform: SupportedPlatform,
    profile: ProviderProfile,
  ) {
    const existing = await this.db.adminRest<any[]>(
      `ul_streaming_connections?user_id=eq.${encodeURIComponent(userId)}&platform=eq.${encodeURIComponent(platform)}&external_account_id=eq.${encodeURIComponent(profile.externalId)}&select=*&limit=1`,
      { method: 'GET' },
    );
    const now = new Date().toISOString();
    if (existing?.[0]) {
      const rows = await this.db.adminRest<any[]>(
        `ul_streaming_connections?id=eq.${encodeURIComponent(existing[0].id)}`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            display_name: profile.displayName,
            external_account_id: profile.externalId,
            external_channel_id: profile.externalId,
            external_channel_name: profile.displayName,
            is_enabled: true,
            updated_at: now,
          }),
        },
      );
      return rows?.[0] || existing[0];
    }

    const rows = await this.db.adminRest<any[]>('ul_streaming_connections', {
      method: 'POST',
      body: JSON.stringify({
        user_id: userId,
        platform,
        display_name: profile.displayName,
        external_account_id: profile.externalId,
        external_channel_id: profile.externalId,
        external_channel_name: profile.displayName,
        status: 'needs_attention',
        is_enabled: true,
        metadata: {
          oauth: true,
          handle: profile.handle || null,
          avatarUrl: profile.avatarUrl || null,
        },
        updated_at: now,
      }),
    });
    if (!rows?.[0]) throw new InternalServerErrorException('Could not create streaming connection');
    return rows[0];
  }

  private async saveOAuthCredential(
    userId: string,
    connectionId: string,
    platform: SupportedPlatform,
    profile: ProviderProfile,
    tokens: ProviderTokens,
  ) {
    const expiresAt = tokens.expiresIn
      ? new Date(Date.now() + tokens.expiresIn * 1000).toISOString()
      : null;
    await this.db.adminRest(
      'ul_oauth_credentials?on_conflict=user_id,platform,provider_user_id',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: userId,
          connection_id: connectionId,
          platform,
          provider_user_id: profile.externalId,
          provider_display_name: profile.displayName,
          access_token_ciphertext: this.crypto.encrypt(tokens.accessToken),
          refresh_token_ciphertext: tokens.refreshToken
            ? this.crypto.encrypt(tokens.refreshToken)
            : null,
          expires_at: expiresAt,
          scope: tokens.scope,
          metadata: profile.metadata || {},
          updated_at: new Date().toISOString(),
        }),
      },
    );
  }

  private async upsertChannel(
    userId: string,
    connectionId: string,
    platform: SupportedPlatform,
    profile: ProviderProfile,
  ) {
    await this.db.adminRest(
      'ul_platform_channels?on_conflict=user_id,platform,external_channel_id',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: userId,
          connection_id: connectionId,
          platform,
          external_channel_id: profile.externalId,
          channel_name: profile.displayName,
          channel_handle: profile.handle || null,
          avatar_url: profile.avatarUrl || null,
          can_stream: false,
          metadata: profile.metadata || {},
          updated_at: new Date().toISOString(),
        }),
      },
    );
  }

  private async provisionTwitchRoute(
    userId: string,
    connectionId: string,
    broadcasterId: string,
    accessToken: string,
  ) {
    const provider = this.providerConfig('twitch');
    const response = await fetch(
      `https://api.twitch.tv/helix/streams/key?broadcaster_id=${encodeURIComponent(broadcasterId)}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Client-Id': provider.clientId,
        },
      },
    );
    const payload: any = await response.json().catch(() => ({}));
    const streamKey = payload?.data?.[0]?.stream_key;
    if (!response.ok || !streamKey) {
      throw new Error(payload?.message || 'Twitch stream key is not available');
    }
    await this.saveRtmpRoute(
      userId,
      connectionId,
      'rtmp://live.twitch.tv/app',
      streamKey,
    );
  }

  private async provisionYouTubeRoute(
    userId: string,
    connectionId: string,
    accessToken: string,
  ): Promise<boolean> {
    const response = await fetch(
      'https://www.googleapis.com/youtube/v3/liveStreams?part=snippet,cdn,contentDetails,status',
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          snippet: { title: 'Universal Live' },
          cdn: {
            frameRate: '30fps',
            ingestionType: 'rtmp',
            resolution: '1080p',
          },
          contentDetails: { isReusable: true },
        }),
      },
    );
    const payload: any = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(payload?.error?.message || 'YouTube live stream route could not be created');
    }
    const info = payload?.cdn?.ingestionInfo;
    const serverUrl = info?.rtmpsIngestionAddress || info?.ingestionAddress;
    const streamKey = info?.streamName;
    if (!serverUrl || !streamKey) return false;
    await this.saveRtmpRoute(userId, connectionId, serverUrl, streamKey);
    return true;
  }

  private async saveRtmpRoute(
    userId: string,
    connectionId: string,
    serverUrl: string,
    streamKey: string,
  ) {
    const existing = await this.db.adminRest<any[]>(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(userId)}&credential_type=eq.rtmp&select=id,key_version`,
      { method: 'GET' },
    );
    const version = Number(existing?.[0]?.key_version || 0) + 1;
    const now = new Date().toISOString();
    await this.db.adminRest(
      'ul_stream_credentials?on_conflict=connection_id,credential_type',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: userId,
          connection_id: connectionId,
          credential_type: 'rtmp',
          server_url_ciphertext: this.crypto.encrypt(serverUrl),
          stream_key_ciphertext: this.crypto.encrypt(streamKey),
          key_version: version,
          last_rotated_at: now,
          updated_at: now,
        }),
      },
    );
    await this.db.adminRest(
      `ul_platform_channels?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(userId)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({ can_stream: true, updated_at: now }),
      },
    );
  }

  private async hasRtmpCredential(userId: string, connectionId: string): Promise<boolean> {
    const rows = await this.db.adminRest<any[]>(
      `ul_stream_credentials?user_id=eq.${encodeURIComponent(userId)}&connection_id=eq.${encodeURIComponent(connectionId)}&credential_type=eq.rtmp&select=id&limit=1`,
      { method: 'GET' },
    );
    return !!rows?.length;
  }

  private async finishState(
    stateId: string,
    status: 'completed' | 'failed',
    errorMessage: string | null,
    connectionId?: string,
  ) {
    await this.db.adminRest(
      `ul_oauth_states?id=eq.${encodeURIComponent(stateId)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status,
          error_message: errorMessage,
          connection_id: connectionId || null,
          consumed_at: new Date().toISOString(),
        }),
      },
    );
  }
}

function escapeHtml(value: string): string {
  return String(value || '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
