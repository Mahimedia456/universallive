import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

const CONTENT_TYPE_LIMIT = 12;
const PLATFORM_LIMIT = 8;
const ALLOWED_EXPERIENCE = new Set(['new', 'some', 'experienced']);
const ALLOWED_GOALS = new Set([
  'gaming',
  'multiplatform',
  'quality',
  'facecam',
  'professional',
]);

@Injectable()
export class DevicesService {
  constructor(private readonly db: BackendSupabase) {}

  async registerDevice(
    accessToken: string,
    body: {
      deviceId: string;
      platform: string;
      pushToken?: string | null;
      appVersion?: string | null;
      osVersion?: string | null;
      deviceModel?: string | null;
      locale?: string | null;
      timezone?: string | null;
      metadata?: Record<string, unknown> | null;
    },
  ) {
    const user = await this.db.currentUser(accessToken);
    const deviceId = String(body.deviceId || '').trim();
    const platform = String(body.platform || '').trim().toLowerCase();

    if (!deviceId) throw new BadRequestException('deviceId is required');
    if (!platform) throw new BadRequestException('platform is required');
    if (deviceId.length > 200) throw new BadRequestException('deviceId is too long');

    const rows = await this.db.adminRest<any[]>(
      'ul_devices?on_conflict=user_id,device_id',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          device_id: deviceId,
          platform,
          push_token: cleanNullable(body.pushToken, 4096),
          push_token_updated_at: body.pushToken ? new Date().toISOString() : undefined,
          push_enabled: body.pushToken ? true : undefined,
          app_version: cleanNullable(body.appVersion, 80),
          os_version: cleanNullable(body.osVersion, 120),
          device_model: cleanNullable(body.deviceModel, 160),
          locale: cleanNullable(body.locale, 40),
          timezone: cleanNullable(body.timezone, 100),
          metadata: isPlainObject(body.metadata) ? body.metadata : {},
          last_seen_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        }),
      },
    );

    return rows?.[0] || null;
  }

  async listDevices(accessToken: string) {
    const user = await this.db.currentUser(accessToken);
    const rows = await this.db.adminRest<any[]>(
      `ul_devices?user_id=eq.${encodeURIComponent(user.id)}&select=id,device_id,platform,app_version,os_version,device_model,last_seen_at,permission_snapshot,permission_snapshot_at,locale,timezone,push_token,push_token_updated_at,push_enabled&order=last_seen_at.desc`,
      { method: 'GET' },
    );
    return (rows || []).map(({ push_token, ...row }) => ({
      ...row,
      push_registered: Boolean(push_token),
    }));
  }

  async updatePermissionSnapshot(
    accessToken: string,
    body: { deviceId: string; permissionSnapshot: Record<string, unknown> },
  ) {
    const user = await this.db.currentUser(accessToken);
    const deviceId = String(body.deviceId || '').trim();
    if (!deviceId) throw new BadRequestException('deviceId is required');
    if (!isPlainObject(body.permissionSnapshot)) {
      throw new BadRequestException('permissionSnapshot must be an object');
    }

    const rows = await this.db.adminRest<any[]>(
      `ul_devices?user_id=eq.${encodeURIComponent(user.id)}&device_id=eq.${encodeURIComponent(deviceId)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          permission_snapshot: body.permissionSnapshot,
          permission_snapshot_at: new Date().toISOString(),
          last_seen_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        }),
      },
    );

    if (!rows?.length) throw new NotFoundException('Device not registered');
    return rows[0];
  }

  async getOnboarding(accessToken: string) {
    const user = await this.db.currentUser(accessToken);
    const rows = await this.db.adminRest<any[]>(
      `ul_onboarding_state?user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    return rows?.[0] || this.emptyOnboarding(user.id);
  }

  async updateOnboarding(accessToken: string, body: Record<string, unknown>) {
    const user = await this.db.currentUser(accessToken);
    const now = new Date().toISOString();
    const allowed = this.sanitizeOnboarding(body);

    if (allowed.creator_setup_completed === true) {
      allowed.creator_setup_completed_at = now;
    }
    if (allowed.permission_education_completed === true) {
      allowed.permission_setup_completed_at = now;
    }

    const rows = await this.db.adminRest<any[]>(
      'ul_onboarding_state?on_conflict=user_id',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          ...allowed,
          updated_at: now,
        }),
      },
    );

    return rows?.[0] || this.emptyOnboarding(user.id);
  }

  private sanitizeOnboarding(body: Record<string, unknown>): Record<string, unknown> {
    const out: Record<string, unknown> = {};
    const booleans = [
      'creator_setup_completed',
      'permission_education_completed',
      'first_destination_prompt_completed',
      'microphone_acknowledged',
      'camera_acknowledged',
      'screen_capture_acknowledged',
      'notifications_acknowledged',
    ];

    for (const key of booleans) {
      if (key in body) out[key] = body[key] === true;
    }

    if ('creator_content_types' in body) {
      out.creator_content_types = cleanStringArray(
        body.creator_content_types,
        CONTENT_TYPE_LIMIT,
        60,
      );
    }

    if ('preferred_platforms' in body) {
      out.preferred_platforms = cleanStringArray(
        body.preferred_platforms,
        PLATFORM_LIMIT,
        40,
      ).map((value) => value.toLowerCase());
    }

    if ('experience_level' in body) {
      const value = String(body.experience_level || '').trim().toLowerCase();
      if (value && !ALLOWED_EXPERIENCE.has(value)) {
        throw new BadRequestException('Invalid experience_level');
      }
      out.experience_level = value || null;
    }

    if ('primary_goal' in body) {
      const value = String(body.primary_goal || '').trim().toLowerCase();
      if (value && !ALLOWED_GOALS.has(value)) {
        throw new BadRequestException('Invalid primary_goal');
      }
      out.primary_goal = value || null;
    }

    if (!Object.keys(out).length) {
      throw new BadRequestException('No supported onboarding fields supplied');
    }

    return out;
  }

  private emptyOnboarding(userId: string) {
    return {
      user_id: userId,
      creator_setup_completed: false,
      permission_education_completed: false,
      first_destination_prompt_completed: false,
      microphone_acknowledged: false,
      camera_acknowledged: false,
      screen_capture_acknowledged: false,
      notifications_acknowledged: false,
      creator_content_types: [],
      preferred_platforms: [],
      experience_level: null,
      primary_goal: null,
      creator_setup_completed_at: null,
      permission_setup_completed_at: null,
    };
  }
}

function cleanNullable(value: unknown, maxLength: number): string | null {
  const text = String(value ?? '').trim();
  if (!text) return null;
  return text.slice(0, maxLength);
}

function cleanStringArray(value: unknown, maxItems: number, maxLength: number): string[] {
  if (!Array.isArray(value)) {
    throw new BadRequestException('Expected an array');
  }

  const values = value
    .map((item) => String(item ?? '').trim())
    .filter(Boolean)
    .map((item) => item.slice(0, maxLength));

  return Array.from(new Set(values)).slice(0, maxItems);
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return !!value && typeof value === 'object' && !Array.isArray(value);
}
