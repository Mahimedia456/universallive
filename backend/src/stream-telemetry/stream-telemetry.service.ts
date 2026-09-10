import { Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class StreamTelemetryService {
  constructor(private readonly db: BackendSupabase) {}

  private async assertSession(token: string, sessionId: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Broadcast session not found');
    return user;
  }

  async sample(token: string, sessionId: string, body: any) {
    const user = await this.assertSession(token, sessionId);

    const rows = await this.db.adminRest<any[]>(
      'ul_stream_telemetry',
      {
        method: 'POST',
        body: JSON.stringify({
          session_id: sessionId,
          user_id: user.id,
          bitrate_kbps: body.bitrateKbps ?? null,
          target_bitrate_kbps: body.targetBitrateKbps ?? null,
          fps: body.fps ?? null,
          dropped_frames: body.droppedFrames ?? null,
          published_video_frames: body.publishedVideoFrames ?? null,
          published_audio_frames: body.publishedAudioFrames ?? null,
          encoder_width: body.encoderWidth ?? null,
          encoder_height: body.encoderHeight ?? null,
          encoder_name: body.encoderName ?? null,
          network_status: body.networkStatus ?? null,
          publish_status: body.publishStatus ?? null,
          audio_status: body.audioStatus ?? null,
          thermal_state: body.thermalState ?? null,
          battery_percent: body.batteryPercent ?? null,
          metadata: body.metadata || {},
        }),
      },
    );

    return rows?.[0];
  }

  async event(token: string, sessionId: string, body: any) {
    const user = await this.assertSession(token, sessionId);

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
