import { Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class StreamHistoryV2Service {
  constructor(private readonly db: BackendSupabase) {}

  async list(token: string, limit = 50) {
    const user = await this.db.currentUser(token);
    const safeLimit = Math.min(Math.max(Number(limit) || 50, 1), 100);

    return this.db.adminRest<any[]>(
      `ul_broadcast_sessions?user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.desc&limit=${safeLimit}`,
      { method: 'GET' },
    );
  }

  async detail(token: string, sessionId: string) {
    const user = await this.db.currentUser(token);

    const sessions = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    if (!sessions?.length) throw new NotFoundException('Stream not found');

    const destinations = await this.db.adminRest<any[]>(
      `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    const summaries = await this.db.adminRest<any[]>(
      `ul_stream_summaries?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    return {
      session: sessions[0],
      destinations,
      summary: summaries?.[0] || null,
    };
  }

  async finalize(token: string, sessionId: string) {
    const user = await this.db.currentUser(token);

    const sessions = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (!sessions?.length) throw new NotFoundException('Stream not found');

    const session = sessions[0];

    const samples = await this.db.adminRest<any[]>(
      `ul_stream_telemetry?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    const destinations = await this.db.adminRest<any[]>(
      `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    const bitrates = (samples || [])
      .map((s: any) => Number(s.bitrate_kbps))
      .filter((n: number) => Number.isFinite(n));

    const fpsValues = (samples || [])
      .map((s: any) => Number(s.fps))
      .filter((n: number) => Number.isFinite(n));

    const started = session.started_at ? new Date(session.started_at).getTime() : null;
    const ended = session.ended_at ? new Date(session.ended_at).getTime() : Date.now();

    const durationSeconds =
      started != null ? Math.max(0, Math.floor((ended - started) / 1000)) : null;

    const avg = (values: number[]) =>
      values.length ? Math.round(values.reduce((a, b) => a + b, 0) / values.length) : null;

    const reconnectCount = (destinations || [])
      .reduce((sum: number, d: any) => sum + Number(d.reconnect_count || 0), 0);

    const successful = (destinations || []).filter((d: any) =>
      ['live', 'ended'].includes(String(d.status)),
    ).length;

    const failed = (destinations || []).filter((d: any) =>
      String(d.status) === 'failed',
    ).length;

    const totalVideoFrames = samples?.length
      ? Math.max(...samples.map((s: any) => Number(s.published_video_frames || 0)))
      : 0;

    const totalAudioFrames = samples?.length
      ? Math.max(...samples.map((s: any) => Number(s.published_audio_frames || 0)))
      : 0;

    const droppedFrames = samples?.length
      ? Math.max(...samples.map((s: any) => Number(s.dropped_frames || 0)))
      : 0;

    const rows = await this.db.adminRest<any[]>(
      'ul_stream_summaries?on_conflict=session_id',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          session_id: sessionId,
          user_id: user.id,
          duration_seconds: durationSeconds,
          avg_bitrate_kbps: avg(bitrates),
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
          final_status: session.status,
          finalized_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        }),
      },
    );

    return rows?.[0];
  }
}
