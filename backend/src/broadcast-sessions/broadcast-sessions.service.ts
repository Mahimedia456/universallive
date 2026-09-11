import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

const OPEN_STATUSES = ['created', 'starting', 'connecting', 'live', 'reconnecting'];

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
        `ul_streaming_connections?id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id,platform,status,is_enabled,display_name,last_health_status`,
        { method: 'GET' },
      );
      const connection = connections?.[0];
      if (!connection) throw new BadRequestException('One or more destinations no longer exist');
      if (connection.is_enabled === false) {
        throw new BadRequestException(`${connection.display_name || 'Destination'} is disabled`);
      }

      const credentials = await this.db.adminRest<any[]>(
        `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id,token_expires_at`,
        { method: 'GET' },
      );
      if (!credentials?.length) {
        throw new BadRequestException(`${connection.display_name || 'Destination'} needs RTMP setup`);
      }
      if (
        credentials[0]?.token_expires_at &&
        new Date(credentials[0].token_expires_at).getTime() <= Date.now()
      ) {
        throw new BadRequestException(`${connection.display_name || 'Destination'} credentials have expired`);
      }

      resolved.push(connection);
    }

    const preflight = body?.preflightId
      ? await this.assertUsablePreflight(user.id, String(body.preflightId), resolved.map((item) => item.id))
      : null;

    const open = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?user_id=eq.${encodeURIComponent(user.id)}&status=in.(${OPEN_STATUSES.join(',')})&select=id,status,created_at&order=created_at.desc&limit=1`,
      { method: 'GET' },
    );
    if (open?.length) {
      throw new BadRequestException(
        `An active/recoverable session already exists (${open[0].status}). End or recover it first.`,
      );
    }

    const now = new Date();
    const recoverableUntil = new Date(now.getTime() + 10 * 60 * 1000).toISOString();
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
          preflight_id: preflight?.id || null,
          status: 'created',
          publisher_state: 'idle',
          recovery_state: 'available',
          recoverable_until: recoverableUntil,
          last_heartbeat_at: now.toISOString(),
          metadata: {
            ...(body.metadata || {}),
            requested_destination_count: resolved.length,
            entitlement_max_destinations: Math.max(1, maxDestinations),
            preflight_required: !!preflight,
            requested_config: preflight?.requested_config || body?.streamConfig || null,
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
            publisher_state: 'idle',
          }),
        },
      );
    }

    if (preflight) {
      await this.db.adminRest(
        `ul_stream_preflights?id=eq.${encodeURIComponent(preflight.id)}&user_id=eq.${encodeURIComponent(user.id)}`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            consumed_at: now.toISOString(),
            updated_at: now.toISOString(),
          }),
        },
      );
    }

    await this.db.adminRest(
      `ul_stream_drafts?user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'started',
          updated_at: now.toISOString(),
        }),
      },
    );

    await this.logEvent(user.id, session.id, 'session_created', 'info', 'Broadcast session created', {
      destinationCount: resolved.length,
      preflightId: preflight?.id || null,
    });

    return this.get(token, session.id);
  }

  async current(token: string) {
    const user = await this.db.currentUser(token);
    const statuses = OPEN_STATUSES.join(',');
    const sessions = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?user_id=eq.${encodeURIComponent(user.id)}&status=in.(${statuses})&select=*&order=created_at.desc&limit=1`,
      { method: 'GET' },
    );
    const session = sessions?.[0];
    if (!session) return null;

    const heartbeatAt = session.last_heartbeat_at
      ? new Date(session.last_heartbeat_at).getTime()
      : session.created_at
        ? new Date(session.created_at).getTime()
        : 0;
    const heartbeatStale = heartbeatAt > 0 && Date.now() - heartbeatAt > 90_000;
    if (heartbeatStale && ['created', 'starting', 'connecting', 'live'].includes(String(session.status))) {
      const now = new Date().toISOString();
      await this.db.adminRest(
        `ul_broadcast_sessions?id=eq.${encodeURIComponent(session.id)}&user_id=eq.${encodeURIComponent(user.id)}`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            status: 'interrupted',
            recovery_state: 'available',
            publisher_state: 'disconnected',
            ended_at: now,
            stop_reason: 'publisher_heartbeat_stale',
            updated_at: now,
          }),
        },
      );
      return null;
    }

    if (
      session.recoverable_until &&
      new Date(session.recoverable_until).getTime() < Date.now() &&
      session.status !== 'live'
    ) {
      await this.db.adminRest(
        `ul_broadcast_sessions?id=eq.${encodeURIComponent(session.id)}&user_id=eq.${encodeURIComponent(user.id)}`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            status: 'interrupted',
            recovery_state: 'expired',
            publisher_state: 'disconnected',
            ended_at: new Date().toISOString(),
            stop_reason: 'recovery_window_expired',
            updated_at: new Date().toISOString(),
          }),
        },
      );
      return null;
    }

    return session;
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
    const user = await this.db.currentUser(token);
    const session = await this.assertOwnedSession(user.id, id);
    if (!['created', 'starting', 'connecting', 'reconnecting'].includes(String(session.status))) {
      if (session.status === 'live') return session;
      throw new BadRequestException(`Cannot start a session in ${session.status} state`);
    }

    const now = new Date();
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'connecting',
          publisher_state: 'connecting',
          last_heartbeat_at: now.toISOString(),
          last_publisher_event_at: now.toISOString(),
          recovery_state: 'available',
          recoverable_until: new Date(now.getTime() + 10 * 60 * 1000).toISOString(),
          transition_seq: Number(session.transition_seq || 0) + 1,
          updated_at: now.toISOString(),
        }),
      },
    );

    await this.db.adminRest(
      `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'connecting',
          publisher_state: 'connecting',
          last_state_at: now.toISOString(),
          updated_at: now.toISOString(),
        }),
      },
    );

    await this.logEvent(user.id, id, 'session_connecting', 'info', 'Native publisher is starting');
    return rows?.[0] || session;
  }

  async heartbeat(token: string, id: string, body: any = {}) {
    const user = await this.db.currentUser(token);
    const session = await this.assertOwnedSession(user.id, id);
    if (!OPEN_STATUSES.includes(String(session.status))) return session;

    const now = new Date();
    const patch: Record<string, unknown> = {
      last_heartbeat_at: now.toISOString(),
      updated_at: now.toISOString(),
    };
    if (session.status !== 'ended') {
      patch.recoverable_until = new Date(now.getTime() + 10 * 60 * 1000).toISOString();
    }
    if (finiteNumber(body?.bitrateKbps) != null) patch.current_bitrate_kbps = Math.round(Number(body.bitrateKbps));
    if (finiteNumber(body?.fps) != null) patch.current_fps = Number(body.fps);

    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(patch) },
    );
    return rows?.[0] || { ...session, ...patch };
  }

  async end(token: string, id: string, stopReason?: string) {
    const user = await this.db.currentUser(token);
    const session = await this.assertOwnedSession(user.id, id);
    if (session.status === 'ended') return session;

    const nowIso = new Date().toISOString();
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'ended',
          publisher_state: 'stopped',
          recovery_state: 'closed',
          recoverable_until: null,
          ended_at: nowIso,
          stop_reason: stopReason || 'user',
          last_heartbeat_at: nowIso,
          last_publisher_event_at: nowIso,
          transition_seq: Number(session.transition_seq || 0) + 1,
          updated_at: nowIso,
        }),
      },
    );

    await this.db.adminRest(
      `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&status=not.in.(failed,ended)`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'ended',
          publisher_state: 'stopped',
          ended_at: nowIso,
          last_state_at: nowIso,
          updated_at: nowIso,
        }),
      },
    );

    await this.logEvent(user.id, id, 'session_ended', 'info', 'Broadcast session ended', {
      stopReason: stopReason || 'user',
    });
    await this.finalizeSummary(user.id, id, rows?.[0] || { ...session, ended_at: nowIso, status: 'ended' });
    return rows?.[0] || session;
  }

  async setDestinationStatus(token: string, sessionId: string, destinationId: string, body: any) {
    const user = await this.db.currentUser(token);

    const patch: any = {
      status: body.status,
      updated_at: new Date().toISOString(),
      last_state_at: new Date().toISOString(),
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

  private async assertUsablePreflight(userId: string, preflightId: string, connectionIds: string[]) {
    const rows = await this.db.adminRest<any[]>(
      `ul_stream_preflights?id=eq.${encodeURIComponent(preflightId)}&user_id=eq.${encodeURIComponent(userId)}&select=*`,
      { method: 'GET' },
    );
    const preflight = rows?.[0];
    if (!preflight) throw new BadRequestException('Preflight not found');
    if (preflight.status !== 'passed') throw new BadRequestException('Preflight did not pass');
    if (preflight.consumed_at) throw new BadRequestException('Preflight has already been consumed');
    if (new Date(preflight.expires_at).getTime() <= Date.now()) {
      throw new BadRequestException('Preflight has expired; run checks again');
    }

    const expected = [...(preflight.destination_connection_ids || [])].map(String).sort();
    const actual = [...connectionIds].map(String).sort();
    if (expected.join('|') !== actual.join('|')) {
      throw new BadRequestException('Selected destinations changed after preflight');
    }
    return preflight;
  }

  private async assertOwnedSession(userId: string, id: string) {
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(userId)}&select=*`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Broadcast session not found');
    return rows[0];
  }

  private async finalizeSummary(userId: string, sessionId: string, session: any) {
    const [samples, destinations] = await Promise.all([
      this.db.adminRest<any[]>(
        `ul_stream_telemetry?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(userId)}&select=*`,
        { method: 'GET' },
      ),
      this.db.adminRest<any[]>(
        `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(userId)}&select=*`,
        { method: 'GET' },
      ),
    ]);

    const bitrates = (samples || [])
      .map((s: any) => finiteNumber(s.rtmp_upload_kbps ?? s.bitrate_kbps))
      .filter((n: number | null): n is number => n != null);
    const fpsValues = (samples || [])
      .map((s: any) => finiteNumber(s.sent_fps ?? s.fps))
      .filter((n: number | null): n is number => n != null);
    const started = session.started_at ? new Date(session.started_at).getTime() : null;
    const ended = session.ended_at ? new Date(session.ended_at).getTime() : Date.now();
    const durationSeconds = started != null ? Math.max(0, Math.floor((ended - started) / 1000)) : 0;
    const average = (values: number[]) => values.length
      ? Math.round(values.reduce((a, b) => a + b, 0) / values.length)
      : null;

    const reconnectCount = (destinations || []).reduce(
      (sum: number, row: any) => sum + Number(row.reconnect_count || 0),
      0,
    );
    const droppedFrames = (samples || []).reduce(
      (max: number, row: any) => Math.max(max, Number(row.dropped_frames || 0)),
      0,
    );
    const totalVideoFrames = (samples || []).reduce(
      (max: number, row: any) => Math.max(max, Number(row.published_video_frames || 0)),
      0,
    );
    const totalAudioFrames = (samples || []).reduce(
      (max: number, row: any) => Math.max(max, Number(row.published_audio_frames || 0)),
      0,
    );
    const successful = (destinations || []).filter((row: any) => ['live', 'ended'].includes(String(row.status))).length;
    const failed = (destinations || []).filter((row: any) => String(row.status) === 'failed').length;

    await this.db.adminRest('ul_stream_summaries?on_conflict=session_id', {
      method: 'POST',
      headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
      body: JSON.stringify({
        session_id: sessionId,
        user_id: userId,
        duration_seconds: durationSeconds,
        avg_bitrate_kbps: average(bitrates),
        peak_bitrate_kbps: bitrates.length ? Math.max(...bitrates) : null,
        avg_fps: fpsValues.length
          ? Number((fpsValues.reduce((a, b) => a + b, 0) / fpsValues.length).toFixed(2))
          : null,
        dropped_frames: droppedFrames,
        reconnect_count: reconnectCount,
        total_video_frames: totalVideoFrames,
        total_audio_frames: totalAudioFrames,
        destination_count: destinations?.length || 0,
        successful_destination_count: successful,
        failed_destination_count: failed,
        final_status: 'ended',
        finalized_at: new Date().toISOString(),
        metadata: {
          finalPublisherState: session.publisher_state || null,
          recoveryAttempts: Number(session.recovery_attempts || 0),
        },
        updated_at: new Date().toISOString(),
      }),
    });
  }

  private async logEvent(
    userId: string,
    sessionId: string,
    eventType: string,
    severity: string,
    message: string,
    metadata: Record<string, unknown> = {},
  ) {
    await this.db.adminRest('ul_stream_events', {
      method: 'POST',
      body: JSON.stringify({
        session_id: sessionId,
        user_id: userId,
        event_type: eventType,
        severity,
        message,
        metadata,
      }),
    });
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

function finiteNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}
