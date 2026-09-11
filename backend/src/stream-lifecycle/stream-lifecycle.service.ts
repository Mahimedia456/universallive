import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { RtmpCredentialsService } from '../rtmp-credentials/rtmp-credentials.service';
import { NotificationsV2Service } from '../notifications-v2/notifications-v2.service';

type CheckRow = {
  key: string;
  ok: boolean;
  required: boolean;
  message: string;
  metadata?: Record<string, unknown>;
};

const ACTIVE_STATUSES = ['created', 'starting', 'connecting', 'live', 'reconnecting'];
const RECOVERABLE_STATUSES = new Set(ACTIVE_STATUSES);
const PUBLISHER_STATES = new Set([
  'idle',
  'connecting',
  'live',
  'reconnecting',
  'error',
  'disconnected',
  'stopped',
]);

@Injectable()
export class StreamLifecycleService {
  constructor(
    private readonly db: BackendSupabase,
    private readonly rtmp: RtmpCredentialsService,
    private readonly notifications: NotificationsV2Service,
  ) {}

  async currentDraft(token: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_stream_drafts?user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    return rows?.[0] || null;
  }

  async saveDraft(token: string, body: any) {
    const user = await this.db.currentUser(token);
    const connectionId = stringOrNull(body?.connectionId);
    const sceneId = stringOrNull(body?.sceneId);

    if (connectionId) {
      const connections = await this.db.adminRest<any[]>(
        `ul_streaming_connections?id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id,is_enabled`,
        { method: 'GET' },
      );
      if (!connections?.length) throw new BadRequestException('Selected destination was not found');
      if (connections[0].is_enabled === false) throw new BadRequestException('Selected destination is disabled');
    }

    if (sceneId) {
      const scenes = await this.db.adminRest<any[]>(
        `ul_scenes?id=eq.${encodeURIComponent(sceneId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id,is_archived`,
        { method: 'GET' },
      );
      if (!scenes?.length || scenes[0].is_archived === true) {
        throw new BadRequestException('Selected scene was not found');
      }
    }

    const nowIso = new Date().toISOString();
    const rows = await this.db.adminRest<any[]>('ul_stream_drafts?on_conflict=user_id', {
      method: 'POST',
      headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
      body: JSON.stringify({
        user_id: user.id,
        title: stringOrNull(body?.title),
        description: stringOrNull(body?.description),
        category: stringOrNull(body?.category),
        privacy: String(body?.privacy || 'public').trim().toLowerCase(),
        connection_id: connectionId,
        scene_id: sceneId,
        stream_config: normalizeRequestedConfig(body?.config || body?.streamConfig || {}),
        status: 'draft',
        last_saved_at: nowIso,
        updated_at: nowIso,
      }),
    });

    const draft = rows?.[0];
    if (!draft) throw new BadRequestException('Could not save stream draft');
    return draft;
  }

  async clearDraft(token: string) {
    const user = await this.db.currentUser(token);
    await this.db.adminRest(
      `ul_stream_drafts?user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'DELETE' },
    );
    return { ok: true };
  }

  async preflight(token: string, body: any) {
    const user = await this.db.currentUser(token);
    await this.expireAbandonedNonLiveSessions(user.id);
    const connectionIds = uniqueStrings(body?.connectionIds || body?.destinations || []);
    const requested = normalizeRequestedConfig(body?.config || {});
    const checks: CheckRow[] = [];
    const warnings: string[] = [];

    if (!connectionIds.length) {
      checks.push({
        key: 'destination_selected',
        ok: false,
        required: true,
        message: 'Choose at least one destination',
      });
    } else {
      checks.push({
        key: 'destination_selected',
        ok: true,
        required: true,
        message: `${connectionIds.length} destination(s) selected`,
      });
    }

    const entitlement = await this.effectiveEntitlement(user.id);
    const maxDestinations = Math.max(
      1,
      Number(
        entitlement?.max_simultaneous_destinations ??
          entitlement?.maxSimultaneousDestinations ??
          1,
      ) || 1,
    );
    checks.push({
      key: 'entitlement',
      ok: connectionIds.length <= maxDestinations,
      required: true,
      message:
        connectionIds.length <= maxDestinations
          ? `Plan allows this destination count (${connectionIds.length}/${maxDestinations})`
          : `Plan allows only ${maxDestinations} simultaneous destination(s)`,
      metadata: { maxDestinations },
    });

    const resolvedConnections: any[] = [];
    for (const connectionId of connectionIds) {
      const rows = await this.db.adminRest<any[]>(
        `ul_streaming_connections?id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id,platform,display_name,status,is_enabled,last_health_status,last_tested_at,last_error_message`,
        { method: 'GET' },
      );
      const connection = rows?.[0];
      if (!connection) {
        checks.push({
          key: `destination:${connectionId}`,
          ok: false,
          required: true,
          message: 'Destination no longer exists',
        });
        continue;
      }

      resolvedConnections.push(connection);
      const credentialRows = await this.db.adminRest<any[]>(
        `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id,key_version,last_rotated_at,token_expires_at`,
        { method: 'GET' },
      );
      const credential = credentialRows?.[0];
      const enabled = connection.is_enabled !== false;
      const credentialReady = !!credential;
      const tokenExpired = credential?.token_expires_at
        ? new Date(credential.token_expires_at).getTime() <= Date.now()
        : false;

      checks.push({
        key: `destination:${connectionId}`,
        ok: enabled && credentialReady && !tokenExpired,
        required: true,
        message: !enabled
          ? `${connection.display_name || 'Destination'} is disabled`
          : !credentialReady
            ? `${connection.display_name || 'Destination'} has no RTMP credentials`
            : tokenExpired
              ? `${connection.display_name || 'Destination'} credentials have expired`
              : `${connection.display_name || 'Destination'} is ready`,
        metadata: {
          platform: connection.platform,
          credentialVersion: credential?.key_version || null,
          lastTestedAt: connection.last_tested_at || null,
        },
      });

      if (connection.last_health_status && connection.last_health_status !== 'ready') {
        warnings.push(
          `${connection.display_name || 'Destination'} last health state: ${connection.last_health_status}`,
        );
      }
    }

    if (body?.sceneId) {
      const sceneRows = await this.db.adminRest<any[]>(
        `ul_scenes?id=eq.${encodeURIComponent(String(body.sceneId))}&user_id=eq.${encodeURIComponent(user.id)}&select=id,name,is_archived`,
        { method: 'GET' },
      );
      const scene = sceneRows?.[0];
      checks.push({
        key: 'scene',
        ok: !!scene && scene.is_archived !== true,
        required: true,
        message: scene ? `Scene ready: ${scene.name}` : 'Selected scene was not found',
      });
    } else {
      checks.push({
        key: 'scene',
        ok: true,
        required: false,
        message: 'Default/local scene will be used',
      });
    }

    const configCheck = validateRequestedConfig(requested);
    checks.push(configCheck);

    if (requested.bitrateKbps < 2500 && requested.width >= 1280) {
      warnings.push('Configured bitrate is low for HD output and may reduce image quality.');
    }
    if (requested.bitrateKbps > 12000 && requested.fps <= 30) {
      warnings.push('Configured bitrate is unusually high for a 30 FPS mobile stream.');
    }

    const activeRows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?user_id=eq.${encodeURIComponent(user.id)}&status=in.(${ACTIVE_STATUSES.join(',')})&select=id,title,status,last_heartbeat_at,recoverable_until,created_at&order=created_at.desc&limit=1`,
      { method: 'GET' },
    );
    const active = activeRows?.[0];
    const replacingSessionId = String(body?.resumeSessionId || '').trim();
    const conflicts = active && active.id !== replacingSessionId;
    checks.push({
      key: 'active_session',
      ok: !conflicts,
      required: true,
      message: conflicts
        ? 'Another active/recoverable broadcast session already exists'
        : active
          ? `Resuming existing session ${active.id}`
          : 'No conflicting active broadcast session',
      metadata: active ? { sessionId: active.id, status: active.status } : undefined,
    });

    const requiredPassed = checks.filter((check) => check.required).every((check) => check.ok);
    const status = requiredPassed ? 'passed' : 'failed';
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000).toISOString();

    const rows = await this.db.adminRest<any[]>('ul_stream_preflights', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        status,
        title: stringOrNull(body?.title),
        description: stringOrNull(body?.description),
        scene_id: stringOrNull(body?.sceneId),
        destination_connection_ids: connectionIds,
        requested_config: requested,
        checks,
        warnings,
        expires_at: expiresAt,
      }),
    });

    const preflight = rows?.[0];
    if (!preflight) throw new BadRequestException('Could not create stream preflight');

    await this.logEventForUser(user.id, null, 'preflight_completed', status === 'passed' ? 'info' : 'warning', {
      preflightId: preflight.id,
      status,
      destinationCount: resolvedConnections.length,
      warningCount: warnings.length,
    });

    return {
      id: preflight.id,
      status,
      ready: status === 'passed',
      checks,
      warnings,
      expiresAt,
      requestedConfig: requested,
      destinationCount: connectionIds.length,
      activeSession: active || null,
    };
  }

  async preflightDetail(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_stream_preflights?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Preflight not found');
    const row = rows[0];
    return {
      ...row,
      ready:
        row.status === 'passed' &&
        !row.consumed_at &&
        new Date(row.expires_at).getTime() > Date.now(),
    };
  }

  async publisherState(token: string, sessionId: string, body: any) {
    const user = await this.db.currentUser(token);
    const session = await this.assertSession(user.id, sessionId);
    if (['ended', 'failed', 'interrupted'].includes(String(session.status))) {
      throw new BadRequestException('Broadcast session is already closed');
    }

    const publishStatus = String(body?.publishStatus || body?.status || '').trim().toLowerCase();
    if (!PUBLISHER_STATES.has(publishStatus)) {
      throw new BadRequestException('Unsupported publisher state');
    }

    const now = new Date();
    const nowIso = now.toISOString();
    const publisherInstanceId = stringOrNull(body?.publisherInstanceId) || session.publisher_instance_id || null;
    const connectionId = stringOrNull(body?.connectionId);

    const destination = connectionId
      ? await this.destinationByConnection(user.id, sessionId, connectionId)
      : await this.firstDestination(user.id, sessionId);

    const destinationPatch: Record<string, unknown> = {
      publisher_state: publishStatus,
      publisher_instance_id: publisherInstanceId,
      last_state_at: nowIso,
      updated_at: nowIso,
    };

    const numericMap: Array<[string, string]> = [
      ['bitrateKbps', 'current_bitrate_kbps'],
      ['fps', 'current_fps'],
      ['rtmpUploadKbps', 'rtmp_upload_kbps'],
      ['socketWriteLatencyMs', 'socket_write_latency_ms'],
      ['publisherEnqueueLatencyMs', 'publisher_enqueue_latency_ms'],
      ['rtmpQueueDepth', 'rtmp_queue_depth'],
    ];
    for (const [input, column] of numericMap) {
      const value = finiteNumber(body?.[input]);
      if (value != null) destinationPatch[column] = value;
    }

    const lastVideoPacketAgeMs = finiteNumber(body?.lastVideoPacketAgeMs);
    const lastAudioPacketAgeMs = finiteNumber(body?.lastAudioPacketAgeMs);
    if (lastVideoPacketAgeMs != null) {
      destinationPatch.last_video_packet_at = new Date(now.getTime() - Math.max(0, lastVideoPacketAgeMs)).toISOString();
    }
    if (lastAudioPacketAgeMs != null) {
      destinationPatch.last_audio_packet_at = new Date(now.getTime() - Math.max(0, lastAudioPacketAgeMs)).toISOString();
    }

    let destinationStatus = destination?.status || 'pending';
    if (publishStatus === 'connecting') destinationStatus = 'connecting';
    if (publishStatus === 'live') destinationStatus = 'live';
    if (publishStatus === 'reconnecting') destinationStatus = 'reconnecting';
    if (publishStatus === 'error') destinationStatus = 'failed';
    if (publishStatus === 'disconnected') destinationStatus = 'disconnected';
    if (publishStatus === 'stopped') destinationStatus = 'ended';
    destinationPatch.status = destinationStatus;

    if (publishStatus === 'live' && !destination?.started_at) destinationPatch.started_at = nowIso;
    if (publishStatus === 'stopped') destinationPatch.ended_at = nowIso;
    if (publishStatus === 'live') destinationPatch.ended_at = null;
    if (publishStatus === 'reconnecting' && destination?.publisher_state !== 'reconnecting') {
      destinationPatch.reconnect_count = Number(destination?.reconnect_count || 0) + 1;
    }
    if (body?.errorCode !== undefined) destinationPatch.last_error_code = stringOrNull(body.errorCode);
    if (body?.errorMessage !== undefined) destinationPatch.last_error_message = stringOrNull(body.errorMessage);
    if (publishStatus === 'live') {
      destinationPatch.last_error_code = null;
      destinationPatch.last_error_message = null;
    }

    if (destination?.id) {
      await this.db.adminRest(
        `ul_broadcast_destinations?id=eq.${encodeURIComponent(destination.id)}&session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
        { method: 'PATCH', body: JSON.stringify(destinationPatch) },
      );
    }

    const sessionPatch: Record<string, unknown> = {
      publisher_state: publishStatus,
      publisher_instance_id: publisherInstanceId,
      last_publisher_event_at: nowIso,
      last_heartbeat_at: nowIso,
      transition_seq: Number(session.transition_seq || 0) + 1,
      updated_at: nowIso,
    };

    const bitrate = finiteNumber(body?.rtmpUploadKbps ?? body?.bitrateKbps);
    const fps = finiteNumber(body?.sentFps ?? body?.fps);
    if (bitrate != null) sessionPatch.current_bitrate_kbps = Math.round(bitrate);
    if (fps != null) sessionPatch.current_fps = fps;
    if (lastVideoPacketAgeMs != null) {
      sessionPatch.last_video_packet_at = new Date(now.getTime() - Math.max(0, lastVideoPacketAgeMs)).toISOString();
    }
    if (lastAudioPacketAgeMs != null) {
      sessionPatch.last_audio_packet_at = new Date(now.getTime() - Math.max(0, lastAudioPacketAgeMs)).toISOString();
    }

    if (publishStatus === 'live') {
      sessionPatch.status = 'live';
      sessionPatch.started_at = session.started_at || nowIso;
      sessionPatch.recovery_state = 'available';
      sessionPatch.recoverable_until = new Date(now.getTime() + 10 * 60 * 1000).toISOString();
    } else if (publishStatus === 'connecting') {
      sessionPatch.status = 'connecting';
      sessionPatch.recovery_state = 'available';
      sessionPatch.recoverable_until = new Date(now.getTime() + 10 * 60 * 1000).toISOString();
    } else if (publishStatus === 'reconnecting') {
      sessionPatch.status = 'reconnecting';
      sessionPatch.recovery_state = 'recovering';
      sessionPatch.recoverable_until = new Date(now.getTime() + 10 * 60 * 1000).toISOString();
    } else if (publishStatus === 'error') {
      sessionPatch.status = 'reconnecting';
      sessionPatch.recovery_state = 'available';
      sessionPatch.recoverable_until = new Date(now.getTime() + 10 * 60 * 1000).toISOString();
    } else if (publishStatus === 'disconnected') {
      sessionPatch.status = 'reconnecting';
      sessionPatch.recovery_state = 'available';
      sessionPatch.recoverable_until = new Date(now.getTime() + 10 * 60 * 1000).toISOString();
    }

    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(sessionPatch) },
    );

    const stateChanged = session.publisher_state !== publishStatus;
    if (stateChanged || ['error', 'reconnecting', 'live'].includes(publishStatus)) {
      await this.logEventForUser(
        user.id,
        sessionId,
        `publisher_${publishStatus}`,
        publishStatus === 'error' ? 'error' : publishStatus === 'reconnecting' ? 'warning' : 'info',
        {
          connectionId: destination?.connection_id || connectionId,
          publisherInstanceId,
          message: stringOrNull(body?.message),
          errorCode: stringOrNull(body?.errorCode),
          errorMessage: stringOrNull(body?.errorMessage),
        },
        destination?.id || null,
      );
    }

    if (stateChanged && ['error', 'disconnected', 'reconnecting'].includes(publishStatus)) {
      await this.notifications.createAndDispatch(user.id, {
        type: 'stream_connection_warning',
        title: publishStatus === 'reconnecting' ? 'Stream reconnecting' : 'Stream connection interrupted',
        body: stringOrNull(body?.errorMessage) || 'Universal Live is trying to recover your broadcast connection.',
        severity: 'warning',
        actionType: 'live',
        actionPayload: { route: 'live', sessionId },
        pushData: { route: 'live', sessionId, type: 'stream_connection_warning' },
      });
    } else if (
      stateChanged &&
      publishStatus === 'live' &&
      ['error', 'disconnected', 'reconnecting'].includes(String(session.publisher_state || ''))
    ) {
      await this.notifications.createAndDispatch(user.id, {
        type: 'stream_recovered',
        title: 'Stream recovered',
        body: 'Your Universal Live broadcast connection is healthy again.',
        severity: 'success',
        actionType: 'live',
        actionPayload: { route: 'live', sessionId },
        pushData: { route: 'live', sessionId, type: 'stream_recovered' },
      });
    }

    return {
      session: rows?.[0] || { ...session, ...sessionPatch },
      destination: destination?.id ? { ...destination, ...destinationPatch } : null,
    };
  }

  async recover(token: string, sessionId: string, body: any) {
    const user = await this.db.currentUser(token);
    const session = await this.assertSession(user.id, sessionId);
    if (!RECOVERABLE_STATUSES.has(String(session.status))) {
      throw new BadRequestException('This broadcast session can no longer be recovered');
    }
    if (session.recoverable_until && new Date(session.recoverable_until).getTime() < Date.now()) {
      await this.db.adminRest(
        `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            recovery_state: 'expired',
            status: 'interrupted',
            ended_at: new Date().toISOString(),
            stop_reason: 'recovery_window_expired',
            updated_at: new Date().toISOString(),
          }),
        },
      );
      throw new BadRequestException('Recovery window has expired');
    }

    const destinations = await this.db.adminRest<any[]>(
      `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.asc`,
      { method: 'GET' },
    );
    if (!destinations?.length) throw new BadRequestException('No destination is attached to this session');

    const publishConfigs: any[] = [];
    for (const destination of destinations) {
      if (!destination.connection_id) continue;
      const publishConfig = await this.rtmp.publishConfig(token, destination.connection_id);
      publishConfigs.push({
        destinationId: destination.id,
        connectionId: destination.connection_id,
        publishConfig,
      });
    }
    if (!publishConfigs.length) {
      throw new BadRequestException('No recoverable RTMP destination is available');
    }

    const attempt = Number(session.recovery_attempts || 0) + 1;
    const nowIso = new Date().toISOString();
    const recoverableUntil = new Date(Date.now() + 10 * 60 * 1000).toISOString();
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'reconnecting',
          publisher_state: 'reconnecting',
          recovery_state: 'recovering',
          recovery_attempts: attempt,
          recoverable_until: recoverableUntil,
          publisher_instance_id: stringOrNull(body?.publisherInstanceId) || session.publisher_instance_id,
          last_publisher_event_at: nowIso,
          last_heartbeat_at: nowIso,
          transition_seq: Number(session.transition_seq || 0) + 1,
          updated_at: nowIso,
        }),
      },
    );

    await this.logEventForUser(user.id, sessionId, 'recovery_requested', 'warning', {
      attempt,
      reason: stringOrNull(body?.reason) || 'client_requested',
      destinationCount: publishConfigs.length,
    });

    return {
      session: rows?.[0] || session,
      recoveryAttempt: attempt,
      recoverableUntil,
      destinations: publishConfigs,
    };
  }

  async diagnostics(token: string, sessionId: string) {
    const user = await this.db.currentUser(token);
    const session = await this.assertSession(user.id, sessionId);
    const [destinations, samples, events] = await Promise.all([
      this.db.adminRest<any[]>(
        `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.asc`,
        { method: 'GET' },
      ),
      this.db.adminRest<any[]>(
        `ul_stream_telemetry?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*&order=sampled_at.desc&limit=30`,
        { method: 'GET' },
      ),
      this.db.adminRest<any[]>(
        `ul_stream_events?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.desc&limit=50`,
        { method: 'GET' },
      ),
    ]);

    const latest = samples?.[0] || null;
    const upload = finiteNumber(latest?.rtmp_upload_kbps ?? latest?.bitrate_kbps);
    const target = finiteNumber(latest?.target_bitrate_kbps);
    const sentFps = finiteNumber(latest?.sent_fps ?? latest?.fps);
    const videoAge = finiteNumber(latest?.last_video_packet_age_ms);
    const publishState = String(latest?.publish_status || session.publisher_state || session.status || 'idle');

    let health = 'starting';
    const issues: string[] = [];
    if (publishState === 'reconnecting') {
      health = 'recovering';
      issues.push('Publisher is reconnecting to ingest.');
    } else if (publishState === 'error' || String(session.status) === 'failed') {
      health = 'error';
      issues.push('Publisher reported a terminal/error state.');
    } else if (publishState === 'live') {
      health = 'healthy';
      if (videoAge != null && videoAge > 5000) {
        health = 'no_video';
        issues.push(`No recent video packet (${Math.round(videoAge)} ms).`);
      }
      if (target && upload != null && upload < target * 0.6) {
        health = health === 'no_video' ? health : 'degraded';
        issues.push(`RTMP upload is below 60% of configured target (${Math.round(upload)}/${Math.round(target)} Kbps).`);
      }
      if (sentFps != null && sentFps < 20) {
        health = health === 'no_video' ? health : 'degraded';
        issues.push(`Sent FPS is low (${sentFps.toFixed(1)}).`);
      }
    }

    return {
      health,
      issues,
      session,
      destinations: destinations || [],
      latestSample: latest,
      recentSamples: samples || [],
      recentEvents: events || [],
    };
  }

  private async expireAbandonedNonLiveSessions(userId: string) {
    const cutoff = new Date(Date.now() - 2 * 60 * 1000).toISOString();
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?user_id=eq.${encodeURIComponent(userId)}&status=in.(created,starting,connecting)&created_at=lt.${encodeURIComponent(cutoff)}&select=id,status`,
      { method: 'GET' },
    );

    for (const row of rows || []) {
      await this.db.adminRest(
        `ul_broadcast_sessions?id=eq.${encodeURIComponent(row.id)}&user_id=eq.${encodeURIComponent(userId)}`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            status: 'interrupted',
            publisher_state: 'disconnected',
            recovery_state: 'expired',
            ended_at: new Date().toISOString(),
            stop_reason: 'abandoned_before_live',
            updated_at: new Date().toISOString(),
          }),
        },
      );
    }
  }

  private async assertSession(userId: string, sessionId: string) {
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(userId)}&select=*`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Broadcast session not found');
    return rows[0];
  }

  private async firstDestination(userId: string, sessionId: string) {
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(userId)}&select=*&order=created_at.asc&limit=1`,
      { method: 'GET' },
    );
    return rows?.[0] || null;
  }

  private async destinationByConnection(userId: string, sessionId: string, connectionId: string) {
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(userId)}&connection_id=eq.${encodeURIComponent(connectionId)}&select=*&limit=1`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Broadcast destination not found');
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

  private async logEventForUser(
    userId: string,
    sessionId: string | null,
    eventType: string,
    severity: string,
    metadata: Record<string, unknown>,
    destinationId: string | null = null,
  ) {
    if (!sessionId) return;
    await this.db.adminRest('ul_stream_events', {
      method: 'POST',
      body: JSON.stringify({
        session_id: sessionId,
        user_id: userId,
        event_type: eventType,
        severity,
        destination_id: destinationId,
        message: stringOrNull(metadata.message),
        metadata,
      }),
    });
  }
}

function uniqueStrings(raw: unknown): string[] {
  if (!Array.isArray(raw)) return [];
  const values = raw
    .map((value: any) => String(value?.connectionId ?? value ?? '').trim())
    .filter(Boolean);
  return Array.from(new Set(values));
}

function normalizeRequestedConfig(raw: any) {
  return {
    width: finiteNumber(raw?.width) ?? 1920,
    height: finiteNumber(raw?.height) ?? 1080,
    fps: finiteNumber(raw?.fps) ?? 30,
    bitrateKbps: finiteNumber(raw?.bitrateKbps) ?? 6800,
    microphoneEnabled: raw?.microphoneEnabled !== false,
    internalAudioEnabled: raw?.internalAudioEnabled !== false,
    orientation: String(raw?.orientation || 'auto'),
    keyframeIntervalSeconds: finiteNumber(raw?.keyframeIntervalSeconds) ?? 2,
    audioSampleRateHz: finiteNumber(raw?.audioSampleRateHz) ?? 48000,
    audioBitrateKbps: finiteNumber(raw?.audioBitrateKbps) ?? 160,
  };
}

function validateRequestedConfig(config: any): CheckRow {
  const ok =
    Number.isFinite(config.width) &&
    Number.isFinite(config.height) &&
    config.width >= 480 &&
    config.width <= 3840 &&
    config.height >= 480 &&
    config.height <= 3840 &&
    [24, 25, 30, 50, 60].includes(Number(config.fps)) &&
    config.bitrateKbps >= 500 &&
    config.bitrateKbps <= 30000 &&
    config.keyframeIntervalSeconds >= 1 &&
    config.keyframeIntervalSeconds <= 4;

  return {
    key: 'encoder_config',
    ok,
    required: true,
    message: ok
      ? `${config.width}x${config.height} @ ${config.fps} FPS, ${config.bitrateKbps} Kbps, ${config.keyframeIntervalSeconds}s keyframes`
      : 'Encoder configuration is outside supported safety limits',
    metadata: config,
  };
}

function finiteNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function stringOrNull(value: unknown): string | null {
  if (value === null || value === undefined) return null;
  const text = String(value).trim();
  return text ? text : null;
}
