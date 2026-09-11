import { Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class StreamTelemetryService {
  constructor(private readonly db: BackendSupabase) {}

  private async assertSession(token: string, sessionId: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Broadcast session not found');
    return { user, session: rows[0] };
  }

  async sample(token: string, sessionId: string, body: any) {
    const { user } = await this.assertSession(token, sessionId);
    const now = new Date();
    const videoAge = finiteNumber(body.lastVideoPacketAgeMs);
    const audioAge = finiteNumber(body.lastAudioPacketAgeMs);
    const uploadKbps = finiteNumber(body.rtmpUploadKbps ?? body.bitrateKbps);
    const sentFps = finiteNumber(body.sentFps ?? body.fps);

    const rows = await this.db.adminRest<any[]>(
      'ul_stream_telemetry',
      {
        method: 'POST',
        body: JSON.stringify({
          session_id: sessionId,
          user_id: user.id,
          bitrate_kbps: integerOrNull(body.bitrateKbps),
          target_bitrate_kbps: integerOrNull(body.targetBitrateKbps),
          fps: finiteNumber(body.fps),
          dropped_frames: integerOrNull(body.droppedFrames),
          published_video_frames: integerOrNull(body.publishedVideoFrames),
          published_audio_frames: integerOrNull(body.publishedAudioFrames),
          encoder_width: integerOrNull(body.encoderWidth),
          encoder_height: integerOrNull(body.encoderHeight),
          encoder_name: stringOrNull(body.encoderName),
          network_status: stringOrNull(body.networkStatus),
          publish_status: stringOrNull(body.publishStatus),
          audio_status: stringOrNull(body.audioStatus),
          thermal_state: stringOrNull(body.thermalState),
          battery_percent: integerOrNull(body.batteryPercent),
          encoder_bitrate_kbps: integerOrNull(body.encoderBitrateKbps),
          rtmp_upload_kbps: integerOrNull(body.rtmpUploadKbps),
          encoded_fps: finiteNumber(body.encodedFps),
          sent_fps: finiteNumber(body.sentFps),
          rtmp_queue_depth: integerOrNull(body.rtmpQueueDepth),
          socket_write_latency_ms: integerOrNull(body.socketWriteLatencyMs),
          publisher_enqueue_latency_ms: integerOrNull(body.publisherEnqueueLatencyMs),
          last_video_packet_age_ms: integerOrNull(body.lastVideoPacketAgeMs),
          last_audio_packet_age_ms: integerOrNull(body.lastAudioPacketAgeMs),
          capture_frame_age_ms: integerOrNull(body.captureFrameAgeMs),
          keyframe_interval_ms: integerOrNull(body.keyframeIntervalMs),
          video_pts_monotonic: booleanOrNull(body.videoPtsMonotonic),
          audio_pts_monotonic: booleanOrNull(body.audioPtsMonotonic),
          reconnect_count: integerOrNull(body.reconnectCount),
          publisher_instance_id: stringOrNull(body.publisherInstanceId),
          metadata: body.metadata || {},
        }),
      },
    );

    const sessionPatch: Record<string, unknown> = {
      last_heartbeat_at: now.toISOString(),
      updated_at: now.toISOString(),
    };
    if (uploadKbps != null) sessionPatch.current_bitrate_kbps = Math.round(uploadKbps);
    if (sentFps != null) sessionPatch.current_fps = sentFps;
    if (videoAge != null) sessionPatch.last_video_packet_at = new Date(now.getTime() - Math.max(0, videoAge)).toISOString();
    if (audioAge != null) sessionPatch.last_audio_packet_at = new Date(now.getTime() - Math.max(0, audioAge)).toISOString();
    await this.db.adminRest(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(sessionPatch) },
    );

    const connectionId = stringOrNull(body.connectionId);
    if (connectionId) {
      const destinationPatch: Record<string, unknown> = { updated_at: now.toISOString() };
      if (uploadKbps != null) {
        destinationPatch.current_bitrate_kbps = Math.round(uploadKbps);
        destinationPatch.rtmp_upload_kbps = Math.round(uploadKbps);
      }
      if (sentFps != null) destinationPatch.current_fps = sentFps;
      if (videoAge != null) destinationPatch.last_video_packet_at = new Date(now.getTime() - Math.max(0, videoAge)).toISOString();
      if (audioAge != null) destinationPatch.last_audio_packet_at = new Date(now.getTime() - Math.max(0, audioAge)).toISOString();
      if (integerOrNull(body.socketWriteLatencyMs) != null) destinationPatch.socket_write_latency_ms = integerOrNull(body.socketWriteLatencyMs);
      if (integerOrNull(body.publisherEnqueueLatencyMs) != null) destinationPatch.publisher_enqueue_latency_ms = integerOrNull(body.publisherEnqueueLatencyMs);
      if (integerOrNull(body.rtmpQueueDepth) != null) destinationPatch.rtmp_queue_depth = integerOrNull(body.rtmpQueueDepth);
      await this.db.adminRest(
        `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&connection_id=eq.${encodeURIComponent(connectionId)}`,
        { method: 'PATCH', body: JSON.stringify(destinationPatch) },
      );
    }

    return rows?.[0];
  }

  async event(token: string, sessionId: string, body: any) {
    const { user } = await this.assertSession(token, sessionId);

    const rows = await this.db.adminRest<any[]>(
      'ul_stream_events',
      {
        method: 'POST',
        body: JSON.stringify({
          session_id: sessionId,
          user_id: user.id,
          event_type: body.eventType,
          severity: body.severity || 'info',
          destination_id: body.destinationId || null,
          message: body.message || null,
          correlation_id: body.correlationId || null,
          metadata: body.metadata || {},
        }),
      },
    );

    return rows?.[0];
  }

  async latest(token: string, sessionId: string) {
    await this.assertSession(token, sessionId);

    const samples = await this.db.adminRest<any[]>(
      `ul_stream_telemetry?session_id=eq.${encodeURIComponent(sessionId)}&select=*&order=sampled_at.desc&limit=60`,
      { method: 'GET' },
    );

    const events = await this.db.adminRest<any[]>(
      `ul_stream_events?session_id=eq.${encodeURIComponent(sessionId)}&select=*&order=created_at.desc&limit=100`,
      { method: 'GET' },
    );

    return { samples, events };
  }
}

function finiteNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function integerOrNull(value: unknown): number | null {
  const number = finiteNumber(value);
  return number == null ? null : Math.round(number);
}

function booleanOrNull(value: unknown): boolean | null {
  if (value === true || value === false) return value;
  if (String(value).toLowerCase() === 'true') return true;
  if (String(value).toLowerCase() === 'false') return false;
  return null;
}

function stringOrNull(value: unknown): string | null {
  if (value === null || value === undefined) return null;
  const text = String(value).trim();
  return text ? text : null;
}
