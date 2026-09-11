import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createSign } from 'crypto';
import { BackendSupabase } from '../common/backend-supabase';

type PushInput = {
  title: string;
  body: string;
  data?: Record<string, string | number | boolean | null | undefined>;
  notificationId?: string | null;
};

@Injectable()
export class PushService {
  private cachedAccessToken: { token: string; expiresAt: number } | null = null;

  constructor(
    private readonly db: BackendSupabase,
    private readonly config: ConfigService,
  ) {}

  configured(): boolean {
    return Boolean(
      this.config.get<string>('FIREBASE_PROJECT_ID') &&
      this.config.get<string>('FIREBASE_CLIENT_EMAIL') &&
      this.config.get<string>('FIREBASE_PRIVATE_KEY'),
    );
  }

  status() {
    return {
      provider: 'fcm-http-v1',
      configured: this.configured(),
      projectId: this.config.get<string>('FIREBASE_PROJECT_ID') || null,
    };
  }

  async sendToUser(userId: string, input: PushInput) {
    const devices = await this.db.adminRest<any[]>(
      `ul_devices?user_id=eq.${encodeURIComponent(userId)}&push_token=not.is.null&select=id,push_token,platform&order=last_seen_at.desc`,
      { method: 'GET' },
    );

    const androidDevices = (devices || []).filter(
      (device) => String(device.platform || '').toLowerCase() === 'android' && String(device.push_token || '').trim(),
    );

    if (!androidDevices.length) {
      return { provider: 'fcm-http-v1', configured: this.configured(), queued: 0, sent: 0, failed: 0 };
    }

    if (!this.configured()) {
      await Promise.all(androidDevices.map((device) => this.recordDelivery(userId, input.notificationId, device.id, 'fcm', 'skipped', null, 'push_not_configured')));
      return { provider: 'fcm-http-v1', configured: false, queued: androidDevices.length, sent: 0, failed: androidDevices.length };
    }

    let sent = 0;
    let failed = 0;
    for (const device of androidDevices) {
      try {
        const providerMessageId = await this.sendFcm(String(device.push_token), input);
        sent += 1;
        await this.recordDelivery(userId, input.notificationId, device.id, 'fcm', 'sent', providerMessageId, null);
      } catch (error) {
        failed += 1;
        const message = error instanceof Error ? error.message.slice(0, 500) : 'FCM send failed';
        await this.recordDelivery(userId, input.notificationId, device.id, 'fcm', 'failed', null, message);
      }
    }

    return { provider: 'fcm-http-v1', configured: true, queued: androidDevices.length, sent, failed };
  }

  private async sendFcm(token: string, input: PushInput): Promise<string | null> {
    const projectId = String(this.config.get('FIREBASE_PROJECT_ID'));
    const accessToken = await this.googleAccessToken();
    const data = Object.fromEntries(
      Object.entries(input.data || {})
        .filter(([, value]) => value !== null && value !== undefined)
        .map(([key, value]) => [key, String(value)]),
    );
    data.title = input.title;
    data.body = input.body;
    if (input.notificationId) data.notificationId = input.notificationId;

    const response = await fetch(`https://fcm.googleapis.com/v1/projects/${encodeURIComponent(projectId)}/messages:send`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        message: {
          token,
          data,
          android: {
            priority: 'high',
            notification: {
              channel_id: 'universallive_updates',
              sound: 'default',
              default_vibrate_timings: true,
            },
          },
        },
      }),
    });

    const text = await response.text();
    if (!response.ok) throw new Error(`FCM ${response.status}: ${text}`);
    try {
      return JSON.parse(text)?.name || null;
    } catch {
      return null;
    }
  }

  private async googleAccessToken(): Promise<string> {
    const now = Math.floor(Date.now() / 1000);
    if (this.cachedAccessToken && this.cachedAccessToken.expiresAt > now + 90) {
      return this.cachedAccessToken.token;
    }

    const clientEmail = String(this.config.get('FIREBASE_CLIENT_EMAIL'));
    const privateKey = String(this.config.get('FIREBASE_PRIVATE_KEY')).replace(/\\n/g, '\n');
    const header = this.base64Url(JSON.stringify({ alg: 'RS256', typ: 'JWT' }));
    const claim = this.base64Url(JSON.stringify({
      iss: clientEmail,
      sub: clientEmail,
      aud: 'https://oauth2.googleapis.com/token',
      scope: 'https://www.googleapis.com/auth/firebase.messaging',
      iat: now,
      exp: now + 3600,
    }));
    const unsigned = `${header}.${claim}`;
    const signer = createSign('RSA-SHA256');
    signer.update(unsigned);
    signer.end();
    const signature = signer.sign(privateKey).toString('base64url');
    const assertion = `${unsigned}.${signature}`;

    const response = await fetch('https://oauth2.googleapis.com/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({
        grant_type: 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        assertion,
      }),
    });
    const json: any = await response.json();
    if (!response.ok || !json?.access_token) {
      throw new Error(`Google OAuth token failed (${response.status})`);
    }
    this.cachedAccessToken = {
      token: String(json.access_token),
      expiresAt: now + Number(json.expires_in || 3600),
    };
    return this.cachedAccessToken.token;
  }

  private base64Url(value: string): string {
    return Buffer.from(value, 'utf8').toString('base64url');
  }

  private async recordDelivery(
    userId: string,
    notificationId: string | null | undefined,
    deviceId: string | null,
    provider: string,
    status: string,
    providerMessageId: string | null,
    errorMessage: string | null,
  ) {
    try {
      await this.db.adminRest('ul_push_deliveries', {
        method: 'POST',
        body: JSON.stringify({
          notification_id: notificationId || null,
          device_id: deviceId,
          user_id: userId,
          provider,
          status,
          provider_message_id: providerMessageId,
          error_message: errorMessage,
          sent_at: status === 'sent' ? new Date().toISOString() : null,
        }),
      });
    } catch {
      // Push delivery logging must not break the caller.
    }
  }
}
