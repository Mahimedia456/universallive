import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

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
        `ul_stream_credentials?user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=connection_id,last_rotated_at`,
        { method: 'GET' },
      ),
    ]);

    const credentialMap = new Map(
      (credentials || []).map((row) => [row.connection_id, row]),
    );

    return (connections || []).map((connection) => ({
      ...connection,
      credential_configured: credentialMap.has(connection.id),
      credential_updated_at: credentialMap.get(connection.id)?.last_rotated_at || null,
      ready_to_publish:
        connection.is_enabled !== false &&
        connection.status === 'connected' &&
        credentialMap.has(connection.id),
    }));
  }

  async create(token: string, body: {
    platform: string;
    displayName: string;
    isDefault?: boolean;
  }) {
    const user = await this.db.currentUser(token);

    if (!body.platform?.trim()) throw new BadRequestException('platform is required');
    if (!body.displayName?.trim()) throw new BadRequestException('displayName is required');

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
          platform: body.platform.trim().toLowerCase(),
          display_name: body.displayName.trim(),
          status: 'disconnected',
          is_default: makeDefault,
          is_enabled: true,
        }),
      },
    );
    return rows?.[0];
  }

  async update(token: string, id: string, body: Record<string, unknown>) {
    const user = await this.db.currentUser(token);
    await this.assertOwned(user.id, id);

    const allowed: Record<string, unknown> = {};

    if ('displayName' in body) {
      const name = String(body.displayName || '').trim();
      if (!name) throw new BadRequestException('displayName is required');
      allowed.display_name = name;
    }
    if ('isEnabled' in body) allowed.is_enabled = !!body.isEnabled;
    if ('isDefault' in body) {
      const nextDefault = !!body.isDefault;
      if (nextDefault) await this.clearDefault(user.id, id);
      allowed.is_default = nextDefault;
    }

    allowed.updated_at = new Date().toISOString();

    const rows = await this.db.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(allowed) },
    );
    if (!rows?.length) throw new NotFoundException('Connection not found');

    if (rows[0].is_enabled === false && rows[0].is_default === true) {
      await this.db.adminRest(
        `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
        { method: 'PATCH', body: JSON.stringify({ is_default: false, updated_at: new Date().toISOString() }) },
      );
      await this.promoteDefault(user.id, id);
      rows[0].is_default = false;
    }

    return this.withCredentialState(user.id, rows[0]);
  }

  async remove(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const connection = await this.assertOwned(user.id, id);

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
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id`,
      { method: 'GET' },
    );

    const ok = connection.is_enabled !== false && !!credential?.length;
    const errorMessage = connection.is_enabled === false
      ? 'Connection is disabled'
      : !credential?.length
        ? 'RTMP server and stream key are not configured'
        : null;

    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: ok ? 'connected' : connection.status,
          last_tested_at: new Date().toISOString(),
          last_success_at: ok ? new Date().toISOString() : connection.last_success_at,
          last_error_code: ok ? null : 'NOT_READY',
          last_error_message: ok ? null : errorMessage,
          updated_at: new Date().toISOString(),
        }),
      },
    );

    return {
      ok,
      connectionId: id,
      status: ok ? 'ready' : 'needs_setup',
      message: ok ? 'Destination is ready to publish' : errorMessage,
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
    if (excludedId) query = `ul_streaming_connections?user_id=eq.${encodeURIComponent(userId)}&is_enabled=eq.true&id=neq.${encodeURIComponent(excludedId)}&select=id&order=created_at.asc&limit=1`;
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
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connection.id)}&user_id=eq.${encodeURIComponent(userId)}&credential_type=eq.rtmp&select=id,last_rotated_at`,
      { method: 'GET' },
    );
    return {
      ...connection,
      credential_configured: !!credentials?.length,
      credential_updated_at: credentials?.[0]?.last_rotated_at || null,
      ready_to_publish: connection.is_enabled !== false && connection.status === 'connected' && !!credentials?.length,
    };
  }
}
