import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class StreamingConnectionsService {
  constructor(private readonly db: BackendSupabase) {}

  async list(token: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_streaming_connections?user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.desc`,
      { method: 'GET' },
    );
  }

  async create(token: string, body: {
    platform: string;
    displayName: string;
    isDefault?: boolean;
  }) {
    const user = await this.db.currentUser(token);

    if (!body.platform?.trim()) throw new BadRequestException('platform is required');
    if (!body.displayName?.trim()) throw new BadRequestException('displayName is required');

    if (body.isDefault) {
      await this.db.adminRest(
        `ul_streaming_connections?user_id=eq.${encodeURIComponent(user.id)}&is_default=eq.true`,
        {
          method: 'PATCH',
          body: JSON.stringify({ is_default: false, updated_at: new Date().toISOString() }),
        },
      );
    }

    const rows = await this.db.adminRest<any[]>(
      'ul_streaming_connections',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          platform: body.platform.trim().toLowerCase(),
          display_name: body.displayName.trim(),
          status: 'disconnected',
          is_default: !!body.isDefault,
          is_enabled: true,
        }),
      },
    );
    return rows?.[0];
  }

  async update(token: string, id: string, body: Record<string, unknown>) {
    const user = await this.db.currentUser(token);
    const allowed: Record<string, unknown> = {};

    if ('displayName' in body) allowed.display_name = body.displayName;
    if ('isEnabled' in body) allowed.is_enabled = body.isEnabled;
    if ('isDefault' in body) allowed.is_default = body.isDefault;

    allowed.updated_at = new Date().toISOString();

    const rows = await this.db.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(allowed) },
    );
    if (!rows?.length) throw new NotFoundException('Connection not found');
    return rows[0];
  }

  async remove(token: string, id: string) {
    const user = await this.db.currentUser(token);
    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'DELETE' },
    );
    return { deleted: true };
  }

  async test(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Connection not found');

    const connection = rows[0];
    const ok = connection.status === 'connected' || connection.platform === 'custom_rtmp';

    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          last_tested_at: new Date().toISOString(),
          last_success_at: ok ? new Date().toISOString() : connection.last_success_at,
          last_error_code: ok ? null : 'NOT_AUTHORIZED',
          last_error_message: ok ? null : 'Connection authorization is incomplete',
          updated_at: new Date().toISOString(),
        }),
      },
    );

    return {
      ok,
      connectionId: id,
      status: ok ? 'ready' : 'needs_authorization',
    };
  }
}
