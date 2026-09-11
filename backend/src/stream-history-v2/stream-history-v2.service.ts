import { Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class StreamHistoryV2Service {
  constructor(private readonly db: BackendSupabase) {}

  async list(token: string, options: {
    limit?: number;
    offset?: number;
    status?: string;
    from?: string;
    to?: string;
  } = {}) {
    const user = await this.db.currentUser(token);
    const limit = Math.min(Math.max(Number(options.limit) || 25, 1), 100);
    const offset = Math.max(Number(options.offset) || 0, 0);
    const filters = [
      `user_id=eq.${encodeURIComponent(user.id)}`,
      'select=*',
      'order=created_at.desc',
      `limit=${limit}`,
      `offset=${offset}`,
    ];
    if (options.status && options.status !== 'all') filters.push(`status=eq.${encodeURIComponent(options.status)}`);
    if (options.from) filters.push(`created_at=gte.${encodeURIComponent(options.from)}`);
    if (options.to) filters.push(`created_at=lte.${encodeURIComponent(options.to)}`);

    const sessions = await this.db.adminRest<any[]>(`ul_broadcast_sessions?${filters.join('&')}`, { method: 'GET' });
    if (!sessions?.length) return [];

    const out = [];
    for (const session of sessions) {
      const summaries = await this.db.adminRest<any[]>(
        `ul_stream_summaries?session_id=eq.${encodeURIComponent(session.id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
        { method: 'GET' },
      );
      const destinations = await this.db.adminRest<any[]>(
        `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(session.id)}&user_id=eq.${encodeURIComponent(user.id)}&select=platform,status,reconnect_count`,
        { method: 'GET' },
      );
      out.push({ ...session, summary: summaries?.[0] || null, destinations: destinations || [] });
    }
    return out;
  }

  async detail(token: string, sessionId: string) {
    const user = await this.db.currentUser(token);
    const sessions = await this.db.adminRest<any[]>(
      `ul_broadcast_sessions?id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (!sessions?.length) throw new NotFoundException('Stream not found');

    const [destinations, summaries, events] = await Promise.all([
      this.db.adminRest<any[]>(
        `ul_broadcast_destinations?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.asc`,
        { method: 'GET' },
      ),
      this.db.adminRest<any[]>(
        `ul_stream_summaries?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
        { method: 'GET' },
      ),
      this.db.adminRest<any[]>(
        `ul_stream_events?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.asc&limit=250`,
        { method: 'GET' },
      ),
    ]);

    return {
      session: sessions[0],
      destinations,
      summary: summaries?.[0] || null,
      events,
    };
  }

  async analytics(token: string, sessionId: string) {
    const user = await this.db.currentUser(token);
    const detail = await this.detail(token, sessionId);
    const samples = await this.db.adminRest<any[]>(
      `ul_stream_telemetry?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*&order=sampled_at.asc&limit=1000`,
      { method: 'GET' },
    );
    const computed = this.computeAnalytics(detail.session, detail.destinations || [], detail.events || [], samples || []);
    return { ...detail, analytics: computed, telemetry: samples || [] };
  }

  private numbers(samples: any[], key: string): number[] {
    return (samples || []).map((s: any) => Number(s[key])).filter((n: number) => Number.isFinite(n));
  }

  private avg(values: number[], decimals = 0): number | null {
    if (!values.length) return null;
    const value = values.reduce((a, b) => a + b, 0) / values.length;
    return decimals ? Number(value.toFixed(decimals)) : Math.round(value);
  }

  private computeAnalytics(session: any, destinations: any[], events: any[], samples: any[]) {
    const bitrates = this.numbers(samples, 'bitrate_kbps');
    const encoderBitrates = this.numbers(samples, 'encoder_bitrate_kbps');
    const upload = this.numbers(samples, 'rtmp_upload_kbps');
    const fps = this.numbers(samples, 'fps');
    const encodedFps = this.numbers(samples, 'encoded_fps');
    const sentFps = this.numbers(samples, 'sent_fps');
    const videoAge = this.numbers(samples, 'last_video_packet_age_ms');
    const audioAge = this.numbers(samples, 'last_audio_packet_age_ms');
    const keyframes = this.numbers(samples, 'keyframe_interval_ms');
    const drops = this.numbers(samples, 'dropped_frames');
    const started = session.started_at ? new Date(session.started_at).getTime() : null;
    const ended = session.ended_at ? new Date(session.ended_at).getTime() : Date.now();
    const durationSeconds = started ? Math.max(0, Math.floor((ended - started) / 1000)) : null;
    const reconnectCount = destinations.reduce((sum, d) => sum + Number(d.reconnect_count || 0), 0);
    const warningCount = events.filter((e) => String(e.severity).toLowerCase() === 'warning').length;
    const errorCount = events.filter((e) => ['error', 'critical'].includes(String(e.severity).toLowerCase())).length;
    const avgTarget = this.avg(this.numbers(samples, 'target_bitrate_kbps'));
    const avgUpload = this.avg(upload);
    const bitrateRatio = avgTarget && avgUpload ? avgUpload / avgTarget : 1;
    const healthyPts = samples.every((s: any) => s.video_pts_monotonic !== false && s.audio_pts_monotonic !== false);
    let healthGrade = 'excellent';
    if (errorCount > 0 || bitrateRatio < 0.5 || !healthyPts) healthGrade = 'poor';
    else if (warningCount > 2 || reconnectCount > 2 || bitrateRatio < 0.75) healthGrade = 'needs_attention';
    else if (warningCount > 0 || reconnectCount > 0 || bitrateRatio < 0.9) healthGrade = 'good';

    return {
      durationSeconds,
      sampleCount: samples.length,
      avgBitrateKbps: this.avg(bitrates.length ? bitrates : encoderBitrates),
      minBitrateKbps: bitrates.length ? Math.min(...bitrates) : null,
      peakBitrateKbps: bitrates.length ? Math.max(...bitrates) : null,
      avgRtmpUploadKbps: avgUpload,
      avgFps: this.avg(fps, 2),
      avgEncodedFps: this.avg(encodedFps, 2),
      avgSentFps: this.avg(sentFps, 2),
      droppedFrames: drops.length ? Math.max(...drops) : 0,
      reconnectCount,
      maxVideoPacketAgeMs: videoAge.length ? Math.max(...videoAge) : null,
      maxAudioPacketAgeMs: audioAge.length ? Math.max(...audioAge) : null,
      keyframeIntervalAvgMs: this.avg(keyframes),
      videoPtsMonotonic: samples.every((s: any) => s.video_pts_monotonic !== false),
      audioPtsMonotonic: samples.every((s: any) => s.audio_pts_monotonic !== false),
      warningCount,
      errorCount,
      healthGrade,
      destinationHealth: destinations.map((d) => ({
        id: d.id,
        platform: d.platform,
        status: d.status,
        reconnectCount: Number(d.reconnect_count || 0),
        currentBitrateKbps: d.current_bitrate_kbps ?? null,
        currentFps: d.current_fps ?? null,
        lastErrorCode: d.last_error_code ?? null,
        lastErrorMessage: d.last_error_message ?? null,
      })),
    };
  }

  async finalize(token: string, sessionId: string) {
    const user = await this.db.currentUser(token);
    const detail = await this.detail(token, sessionId);
    const samples = await this.db.adminRest<any[]>(
      `ul_stream_telemetry?session_id=eq.${encodeURIComponent(sessionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    const a = this.computeAnalytics(detail.session, detail.destinations || [], detail.events || [], samples || []);
    const totalVideoFrames = samples.length ? Math.max(...samples.map((s: any) => Number(s.published_video_frames || 0))) : 0;
    const totalAudioFrames = samples.length ? Math.max(...samples.map((s: any) => Number(s.published_audio_frames || 0))) : 0;
    const successful = (detail.destinations || []).filter((d: any) => ['live', 'ended'].includes(String(d.status))).length;
    const failed = (detail.destinations || []).filter((d: any) => String(d.status) === 'failed').length;

    const rows = await this.db.adminRest<any[]>('ul_stream_summaries?on_conflict=session_id', {
      method: 'POST',
      headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
      body: JSON.stringify({
        session_id: sessionId,
        user_id: user.id,
        duration_seconds: a.durationSeconds,
        avg_bitrate_kbps: a.avgBitrateKbps,
        min_bitrate_kbps: a.minBitrateKbps,
        peak_bitrate_kbps: a.peakBitrateKbps,
        avg_rtmp_upload_kbps: a.avgRtmpUploadKbps,
        avg_fps: a.avgFps,
        avg_encoded_fps: a.avgEncodedFps,
        avg_sent_fps: a.avgSentFps,
        dropped_frames: a.droppedFrames,
        reconnect_count: a.reconnectCount,
        total_video_frames: totalVideoFrames,
        total_audio_frames: totalAudioFrames,
        destination_count: detail.destinations?.length || 0,
        successful_destination_count: successful,
        failed_destination_count: failed,
        max_video_packet_age_ms: a.maxVideoPacketAgeMs,
        max_audio_packet_age_ms: a.maxAudioPacketAgeMs,
        keyframe_interval_avg_ms: a.keyframeIntervalAvgMs,
        video_pts_monotonic: a.videoPtsMonotonic,
        audio_pts_monotonic: a.audioPtsMonotonic,
        health_grade: a.healthGrade,
        warning_count: a.warningCount,
        error_count: a.errorCount,
        final_status: detail.session.status,
        finalized_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }),
    });
    return rows?.[0];
  }
}
