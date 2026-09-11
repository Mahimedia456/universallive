import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class DiagnosticsService {
  constructor(
    private readonly db: BackendSupabase,
    private readonly config: ConfigService,
  ) {}

  private async count(path: string): Promise<number> {
    const rows = await this.db.adminRest<any[]>(path, { method: 'GET' }).catch(() => []);
    return Array.isArray(rows) ? rows.length : 0;
  }

  async summary(token: string) {
    const user = await this.db.currentUser(token);
    const [connections, scenes, activeSessions, devices, notifications] = await Promise.all([
      this.count(`ul_streaming_connections?user_id=eq.${encodeURIComponent(user.id)}&select=id&limit=200`),
      this.count(`ul_scenes?user_id=eq.${encodeURIComponent(user.id)}&select=id&limit=200`),
      this.count(`ul_broadcast_sessions?user_id=eq.${encodeURIComponent(user.id)}&status=in.(created,starting,live,reconnecting)&select=id&limit=20`),
      this.count(`ul_devices?user_id=eq.${encodeURIComponent(user.id)}&select=id&limit=100`),
      this.count(`ul_notifications?user_id=eq.${encodeURIComponent(user.id)}&read_at=is.null&select=id&limit=200`),
    ]);

    const flags = await this.db.adminRest<any[]>(
      'ul_system_flags?is_public=eq.true&select=key,value,updated_at&order=key.asc',
      { method: 'GET' },
    ).catch(() => []);

    return {
      ok: true,
      generatedAt: new Date().toISOString(),
      contract: {
        apiVersion: 'v1',
        mobileContractVersion: '2026.09-final',
        backendPhase: '34-39-final',
      },
      account: {
        userId: user.id,
        email: user.email,
        emailVerified: Boolean(user.email_confirmed_at),
      },
      backend: {
        database: 'supabase-postgresql',
        auth: 'universallive-db',
        firebaseConfigured: Boolean(
          this.config.get<string>('FIREBASE_PROJECT_ID') &&
          this.config.get<string>('FIREBASE_CLIENT_EMAIL') &&
          this.config.get<string>('FIREBASE_PRIVATE_KEY')
        ),
        smtpConfigured: this.config.get<string>('EMAIL_MODE') === 'smtp',
      },
      counts: {
        connections,
        scenes,
        activeSessions,
        devices,
        unreadNotifications: notifications,
      },
      publicFlags: flags || [],
      redaction: {
        secretsIncluded: false,
        streamKeysIncluded: false,
        accessTokensIncluded: false,
        firebasePrivateKeyIncluded: false,
      },
    };
  }

  async createSnapshot(token: string, body: any) {
    const user = await this.db.currentUser(token);
    const server = await this.summary(token);
    const sanitizedClient = {
      appVersion: String(body?.appVersion || '').slice(0, 64) || null,
      buildNumber: String(body?.buildNumber || '').slice(0, 64) || null,
      platform: String(body?.platform || 'android').slice(0, 32),
      deviceModel: String(body?.deviceModel || '').slice(0, 160) || null,
      osVersion: String(body?.osVersion || '').slice(0, 80) || null,
      captureStatus: String(body?.captureStatus || '').slice(0, 80) || null,
      publishStatus: String(body?.publishStatus || '').slice(0, 80) || null,
      permissionSummary: body?.permissionSummary && typeof body.permissionSummary === 'object'
        ? body.permissionSummary
        : {},
      streamSummary: body?.streamSummary && typeof body.streamSummary === 'object'
        ? body.streamSummary
        : {},
    };

    const rows = await this.db.adminRest<any[]>('ul_diagnostic_snapshots', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        snapshot: { server, client: sanitizedClient },
        expires_at: new Date(Date.now() + 14 * 24 * 60 * 60 * 1000).toISOString(),
      }),
    });

    return {
      id: rows?.[0]?.id,
      createdAt: rows?.[0]?.created_at,
      expiresAt: rows?.[0]?.expires_at,
      redacted: true,
    };
  }
}
