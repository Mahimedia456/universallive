import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

const ALLOWED_PLATFORMS = new Set([
  'youtube',
  'facebook',
  'twitch',
  'tiktok',
  'custom_rtmp',
]);

@Injectable()
export class StreamingConnectionsService {
  constructor(private readonly db: BackendSupabase) {}

  async list(token: string) {
    const user = await this.db.currentUser(token);
    const [connections, credentials] = await Promise.all([
      this.db.adminRest<any[]>(
        `ul_streaming_connections?user_id=eq.${encodeURIComponent(user.id)}&select=*&order=is_default.desc,created_at.desc`,
        { method: 'GET' },
      ),
      this.db.adminRest<any[]>(
        `ul_stream_credentials?user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=connection_id,last_rotated_at,key_version`,
        { method: 'GET' },
      ),
    ]);

    const credentialMap = new Map(
      (credentials || []).map((row) => [row.connection_id, row]),
    );

    return (connections || []).map((connection) =>
      this.decorateConnection(connection, credentialMap.get(connection.id)),
    );
  }

  async detail(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const connection = await this.assertOwned(user.id, id);
    const [credentialRows, eventRows] = await Promise.all([
      this.db.adminRest<any[]>(
        `ul_stream_credentials?connection_id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id,last_rotated_at,key_version,token_expires_at`,
        { method: 'GET' },
      ),
      this.db.adminRest<any[]>(
        `ul_connection_events?user_id=eq.${encodeURIComponent(user.id)}&connection_id=eq.${encodeURIComponent(id)}&select=id,event_type,status,message,metadata,created_at&order=created_at.desc&limit=10`,
        { method: 'GET' },
      ),
    ]);

    return {
      ...this.decorateConnection(connection, credentialRows?.[0]),
      credential: credentialRows?.[0]
        ? {
            configured: true,
            key_version: credentialRows[0].key_version,
            last_rotated_at: credentialRows[0].last_rotated_at,
            token_expires_at: credentialRows[0].token_expires_at,
          }
        : { configured: false },
      recent_events: eventRows || [],
    };
  }

  async create(token: string, body: {
    platform: string;
    displayName: string;
    isDefault?: boolean;
  }) {
    const user = await this.db.currentUser(token);
    const platform = normalizePlatform(body.platform);
    const displayName = String(body.displayName || '').trim();

    if (!displayName) throw new BadRequestException('displayName is required');
    if (displayName.length > 120) throw new BadRequestException('displayName is too long');

    const existing = await this.db.adminRest<any[]>(
      `ul_streaming_connections?user_id=eq.${encodeURIComponent(user.id)}&select=id,is_default&limit=1`,
      { method: 'GET' },
    );
    const makeDefault = !!body.isDefault || !existing?.length;

    if (makeDefault) await this.clearDefault(user.id);

    const rows = await this.db.adminRest<any[]>(
      'ul_streaming_connections',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          platform,
          display_name: displayName,
          status: 'disconnected',
          is_default: makeDefault,
          is_enabled: true,
          last_health_status: 'not_tested',
        }),
      },
    );

    const created = rows?.[0];
    if (!created) throw new BadRequestException('Could not create connection');

    await this.logEvent(user.id, created.id, 'created', 'ok', 'Destination created', {
      platform,
      is_default: makeDefault,
    });

    // Creating the first destination satisfies the onboarding prompt regardless
    // of which locked UI route initiated it.
    await this.db.adminRest(
      'ul_onboarding_state?on_conflict=user_id',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          first_destination_prompt_completed: true,
          updated_at: new Date().toISOString(),
        }),
      },
    );

    return this.decorateConnection(created, null);
  }

  async update(token: string, id: string, body: Record<string, unknown>) {
    const user = await this.db.currentUser(token);
    const current = await this.assertOwned(user.id, id);

    const allowed: Record<string, unknown> = {};

    if ('displayName' in body) {
      const name = String(body.displayName || '').trim();
      if (!name) throw new BadRequestException('displayName is required');
      if (name.length > 120) throw new BadRequestException('displayName is too long');
      allowed.display_name = name;
    }
    if ('isEnabled' in body) allowed.is_enabled = body.isEnabled === true;
    if ('isDefault' in body) {
      const nextDefault = body.isDefault === true;
      if (nextDefault) await this.clearDefault(user.id, id);
      allowed.is_default = nextDefault;
    }

    if (Object.keys(allowed).length === 0) {
      throw new BadRequestException('No supported connection fields supplied');
    }

    allowed.updated_at = new Date().toISOString();

    const rows = await this.db.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(allowed) },
    );
    if (!rows?.length) throw new NotFoundException('Connection not found');

    let updated = rows[0];

    if (updated.is_enabled === false && updated.is_default === true) {
      await this.db.adminRest(
        `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
        { method: 'PATCH', body: JSON.stringify({ is_default: false, updated_at: new Date().toISOString() }) },
      );
      await this.promoteDefault(user.id, id);
      updated = { ...updated, is_default: false };
    } else if (current.is_default === true && updated.is_default === false) {
      await this.promoteDefault(user.id, id);
    }

    await this.logEvent(user.id, id, 'updated', 'ok', 'Destination settings updated', {
      fields: Object.keys(allowed).filter((key) => key !== 'updated_at'),
    });

    return this.withCredentialState(user.id, updated);
  }

  async remove(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const connection = await this.assertOwned(user.id, id);

    await this.logEvent(user.id, id, 'deleted', 'ok', 'Destination removed', {
      platform: connection.platform,
      display_name: connection.display_name,
    });

    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'DELETE' },
    );

    if (connection.is_default) await this.promoteDefault(user.id, id);
    return { deleted: true };
  }

  async test(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const connection = await this.assertOwned(user.id, id);
    const credential = await this.db.adminRest<any[]>(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id,last_rotated_at,key_version`,
      { method: 'GET' },
    );

    const ok = connection.is_enabled !== false && !!credential?.length;
    const errorMessage = connection.is_enabled === false
      ? 'Connection is disabled'
      : !credential?.length
        ? 'RTMP server and stream key are not configured'
        : null;
    const now = new Date().toISOString();

    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: ok ? 'connected' : 'disconnected',
          last_tested_at: now,
          last_health_checked_at: now,
          last_health_status: ok ? 'ready' : 'needs_setup',
          last_success_at: ok ? now : connection.last_success_at,
          last_error_code: ok ? null : 'NOT_READY',
          last_error_message: ok ? null : errorMessage,
          updated_at: now,
        }),
      },
    );

    await this.logEvent(
      user.id,
      id,
      'tested',
      ok ? 'ready' : 'needs_setup',
      ok ? 'Destination configuration is ready' : errorMessage,
      { credential_configured: !!credential?.length },
    );

    return {
      ok,
      connectionId: id,
      status: ok ? 'ready' : 'needs_setup',
      message: ok
        ? 'Destination configuration is ready. Final network/publish validation occurs during preflight/live start.'
        : errorMessage,
      checkedAt: now,
    };
  }

  private async clearDefault(userId: string, exceptId?: string) {
    let query = `ul_streaming_connections?user_id=eq.${encodeURIComponent(userId)}&is_default=eq.true`;
    if (exceptId) query += `&id=neq.${encodeURIComponent(exceptId)}`;
    await this.db.adminRest(query, {
      method: 'PATCH',
      body: JSON.stringify({ is_default: false, updated_at: new Date().toISOString() }),
    });
  }

  private async promoteDefault(userId: string, excludedId?: string) {
    let query = `ul_streaming_connections?user_id=eq.${encodeURIComponent(userId)}&is_enabled=eq.true&select=id&order=created_at.asc&limit=1`;
    if (excludedId) {
      query = `ul_streaming_connections?user_id=eq.${encodeURIComponent(userId)}&is_enabled=eq.true&id=neq.${encodeURIComponent(excludedId)}&select=id&order=created_at.asc&limit=1`;
    }
    const rows = await this.db.adminRest<any[]>(query, { method: 'GET' });
    if (!rows?.length) return;
    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(rows[0].id)}&user_id=eq.${encodeURIComponent(userId)}`,
      { method: 'PATCH', body: JSON.stringify({ is_default: true, updated_at: new Date().toISOString() }) },
    );
  }

  private async assertOwned(userId: string, id: string) {
    const rows = await this.db.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(userId)}&select=*`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Connection not found');
    return rows[0];
  }

  private async withCredentialState(userId: string, connection: any) {
    const credentials = await this.db.adminRest<any[]>(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connection.id)}&user_id=eq.${encodeURIComponent(userId)}&credential_type=eq.rtmp&select=id,last_rotated_at,key_version`,
      { method: 'GET' },
    );
    return this.decorateConnection(connection, credentials?.[0]);
  }

  private decorateConnection(connection: any, credential: any) {
    return {
      ...connection,
      credential_configured: !!credential,
      credential_updated_at: credential?.last_rotated_at || null,
      credential_version: credential?.key_version || null,
      ready_to_publish:
        connection.is_enabled !== false &&
        connection.status === 'connected' &&
        !!credential,
    };
  }

  private async logEvent(
    userId: string,
    connectionId: string,
    eventType: string,
    status: string | null,
    message: string | null,
    metadata: Record<string, unknown> = {},
  ) {
    await this.db.adminRest('ul_connection_events', {
      method: 'POST',
      body: JSON.stringify({
        user_id: userId,
        connection_id: connectionId,
        event_type: eventType,
        status,
        message,
        metadata,
      }),
    });
  }
}

function normalizePlatform(raw: unknown): string {
  const platform = String(raw || '').trim().toLowerCase();
  if (!platform) throw new BadRequestException('platform is required');
  if (!ALLOWED_PLATFORMS.has(platform)) {
    throw new BadRequestException(
      `Unsupported platform. Use one of: ${Array.from(ALLOWED_PLATFORMS).join(', ')}`,
    );
  }
  return platform;
}
