import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class BroadcastSessionsService {
  constructor(private readonly db: BackendSupabase) {}

  async create(token: string, body: any) {
    const user = await this.db.currentUser(token);
    const destinations: any[] = Array.isArray(body.destinations) ? body.destinations : [];

    if (!destinations.length) {
      throw new BadRequestException('Choose at least one destination');
    }

    const entitlement = await this.effectiveEntitlement(user.id);
    const maxDestinations = Number(
      entitlement?.max_simultaneous_destinations ??
      entitlement?.maxSimultaneousDestinations ??
      1,
    );

    if (destinations.length > Math.max(1, maxDestinations)) {
      throw new BadRequestException(
        `Your plan allows up to ${Math.max(1, maxDestinations)} simultaneous destination(s)`,
      );
    }

    const resolved: any[] = [];
    for (const requested of destinations) {
      const connectionId = String(requested?.connectionId || '').trim();
      if (!connectionId) throw new BadRequestException('Destination connectionId is required');

      const connections = await this.db.adminRest<any[]>(
        `ul_streaming_connections?id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id,platform,status,is_enabled,display_name`,
        { method: 'GET' },
      );
      const connection = connections?.[0];
      if (!connection) throw new BadRequestException('One or more destinations no longer exist');
      if (connection.is_enabled === false) {
        throw new BadRequestException(`${connection.display_name || 'Destination'} is disabled`);
      }

      const credentials = await this.db.adminRest<any[]>(
        `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id`,
        { method: 'GET' },
      );
      if (!credentials?.length) {
        throw new BadRequestException(`${connection.display_name || 'Destination'} needs RTMP setup`);
      }

      resolved.push(connection);
    }

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
          metadata: {
            ...(body.metadata || {}),
            requested_destination_count: resolved.length,
            entitlement_max_destinations: Math.max(1, maxDestinations),
          },
        }),
      },
    );

    const session = rows?.[0];
    if (!session) throw new BadRequestException('Could not create broadcast session');

    for (const connection of resolved) {
      await this.db.adminRest(
        'ul_broadcast_destinations',
        {
          method: 'POST',
          body: JSON.stringify({
            session_id: session.id,
            user_id: user.id,
            connection_id: connection.id,
            platform: connection.platform || 'custom_rtmp',
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

  private async effectiveEntitlement(userId: string): Promise<Record<string, unknown>> {
    const rows = await this.db.adminRest<any[]>(
      `ul_user_entitlements?user_id=eq.${encodeURIComponent(userId)}&select=plan_key,status,entitlements_override`,
      { method: 'GET' },
    );
    const entitlement = rows?.[0] || { plan_key: 'free', status: 'active', entitlements_override: {} };
    const plans = await this.db.adminRest<any[]>(
      `ul_plans?plan_key=eq.${encodeURIComponent(entitlement.plan_key || 'free')}&select=entitlements`,
      { method: 'GET' },
    );
    return {
      ...(plans?.[0]?.entitlements || {}),
      ...(entitlement?.entitlements_override || {}),
    };
  }
}
