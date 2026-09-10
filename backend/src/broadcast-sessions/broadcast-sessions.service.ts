import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class BroadcastSessionsService {
  constructor(private readonly db: BackendSupabase) {}

  async create(token: string, body: any) {
    const user = await this.db.currentUser(token);

    const rows = await this.db.adminRest<any[]>(
      'ul_broadcast_sessions',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          title: body.title || null,
          description: body.description || null,
          scene_id: body.sceneId || null,
          stream_config_id: body.streamConfigId || null,
          client_session_id: body.clientSessionId || null,
          status: 'created',
          metadata: body.metadata || {},
        }),
      },
    );

    const session = rows?.[0];
    if (!session) throw new BadRequestException('Could not create broadcast session');

    const destinations: any[] = Array.isArray(body.destinations) ? body.destinations : [];

    for (const d of destinations) {
      await this.db.adminRest(
        'ul_broadcast_destinations',
        {
          method: 'POST',
          body: JSON.stringify({
            session_id: session.id,
            user_id: user.id,
            connection_id: d.connectionId || null,
            platform: d.platform || 'custom_rtmp',
            status: 'pending',
          }),
        },
      );
    }

    return this.get(token, session.id);
  }

  async get(token: string, id: string) {
    const user = await this.db.currentUser(token);

    const sessions = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    if (!sessions?.length) throw new NotFoundException('Broadcast session not found');

    const destinations = await this.db.adminRest<any[]>(
      `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    return { ...sessions[0], destinations };
  }

  async start(token: string, id: string) {
    return this.setStatus(token, id, 'live', {
      started_at: new Date().toISOString(),
      last_heartbeat_at: new Date().toISOString(),
    });
  }

  async heartbeat(token: string, id: string) {
    return this.setStatus(token, id, 'live', {
      last_heartbeat_at: new Date().toISOString(),
    });
  }

  async end(token: string, id: string, stopReason?: string) {
    return this.setStatus(token, id, 'ended', {
      ended_at: new Date().toISOString(),
      stop_reason: stopReason || 'user',
    });
  }

  async setDestinationStatus(token: string, sessionId: string, destinationId: string, body: any) {
    const user = await this.db.currentUser(token);

    const patch: any = {
      status: body.status,
      updated_at: new Date().toISOString(),
    };

    if (body.status === 'live') patch.started_at = new Date().toISOString();
    if (body.status === 'ended' || body.status === 'failed') patch.ended_at = new Date().toISOString();
    if (body.reconnectCount != null) patch.reconnect_count = body.reconnectCount;
    if (body.lastErrorCode !== undefined) patch.last_error_code = body.lastErrorCode;
    if (body.lastErrorMessage !== undefined) patch.last_error_message = body.lastErrorMessage;

    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_destinations?id=eq.${encodeURIComponent(destinationId)}&session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(patch) },
    );

    if (!rows?.length) throw new NotFoundException('Destination not found');
    return rows[0];
  }

  private async setStatus(token: string, id: string, status: string, extra: any) {
    const user = await this.db.currentUser(token);

    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status,
          ...extra,
          updated_at: new Date().toISOString(),
        }),
      },
    );

    if (!rows?.length) throw new NotFoundException('Broadcast session not found');
    return rows[0];
  }
}
