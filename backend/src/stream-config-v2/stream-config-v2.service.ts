import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class StreamConfigV2Service {
  constructor(private readonly db: BackendSupabase) {}

  async list(token: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_stream_configs?user_id=eq.${encodeURIComponent(user.id)}&select=*&order=is_default.desc,created_at.desc`,
      { method: 'GET' },
    );
  }

  private validate(body: any) {
    const fps = Number(body.fps ?? 30);
    const bitrate = Number(body.bitrateKbps ?? 6800);
    const keyframe = Number(body.keyframeIntervalSeconds ?? 2);
    const audioBitrate = Number(body.audioBitrateKbps ?? 160);
    const audioSampleRate = Number(body.audioSampleRateHz ?? 48000);
    if (![30, 60].includes(fps)) throw new BadRequestException('fps must be 30 or 60');
    if (bitrate < 1000 || bitrate > 30000) throw new BadRequestException('bitrateKbps is out of range');
    if (keyframe < 1 || keyframe > 5) throw new BadRequestException('keyframeIntervalSeconds is out of range');
    if (audioBitrate < 64 || audioBitrate > 320) throw new BadRequestException('audioBitrateKbps is out of range');
    if (![44100, 48000].includes(audioSampleRate)) throw new BadRequestException('audioSampleRateHz must be 44100 or 48000');
    return { fps, bitrate, keyframe, audioBitrate, audioSampleRate };
  }

  async create(token: string, body: any) {
    const user = await this.db.currentUser(token);
    const v = this.validate(body);
    if (body.isDefault) await this.clearDefault(user.id);

    const rows = await this.db.adminRest<any[]>('ul_stream_configs', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        name: body.name || 'Default',
        resolution: body.resolution || '1080p',
        width: body.width || 1920,
        height: body.height || 1080,
        fps: v.fps,
        bitrate_kbps: v.bitrate,
        orientation: body.orientation || 'auto',
        microphone_enabled: body.microphoneEnabled ?? true,
        internal_audio_enabled: body.internalAudioEnabled ?? true,
        facecam_enabled: body.facecamEnabled ?? false,
        scene_id: body.sceneId || null,
        connection_id: body.connectionId || null,
        privacy: body.privacy || 'public',
        is_default: !!body.isDefault,
        keyframe_interval_seconds: v.keyframe,
        audio_bitrate_kbps: v.audioBitrate,
        audio_sample_rate_hz: v.audioSampleRate,
        adaptive_bitrate_enabled: body.adaptiveBitrateEnabled ?? true,
        audio_config: body.audioConfig || {},
        facecam_config: body.facecamConfig || {},
        metadata: body.metadata || {},
      }),
    });
    return rows?.[0];
  }

  private async clearDefault(userId: string) {
    await this.db.adminRest(
      `ul_stream_configs?user_id=eq.${encodeURIComponent(userId)}&is_default=eq.true`,
      { method: 'PATCH', body: JSON.stringify({ is_default: false, updated_at: new Date().toISOString() }) },
    );
  }

  async update(token: string, id: string, body: any) {
    const user = await this.db.currentUser(token);
    const current = await this.db.adminRest<any[]>(
      `ul_stream_configs?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (!current?.length) throw new NotFoundException('Stream config not found');
    const merged = { ...current[0], ...body };
    if ('fps' in body || 'bitrateKbps' in body || 'keyframeIntervalSeconds' in body || 'audioBitrateKbps' in body || 'audioSampleRateHz' in body) this.validate({
      fps: body.fps ?? current[0].fps,
      bitrateKbps: body.bitrateKbps ?? current[0].bitrate_kbps,
      keyframeIntervalSeconds: body.keyframeIntervalSeconds ?? current[0].keyframe_interval_seconds,
      audioBitrateKbps: body.audioBitrateKbps ?? current[0].audio_bitrate_kbps,
      audioSampleRateHz: body.audioSampleRateHz ?? current[0].audio_sample_rate_hz,
    });
    if (body.isDefault === true) await this.clearDefault(user.id);

    const patch: any = { updated_at: new Date().toISOString() };
    const map: Record<string, string> = {
      name: 'name', resolution: 'resolution', width: 'width', height: 'height', fps: 'fps',
      bitrateKbps: 'bitrate_kbps', orientation: 'orientation', microphoneEnabled: 'microphone_enabled',
      internalAudioEnabled: 'internal_audio_enabled', facecamEnabled: 'facecam_enabled', sceneId: 'scene_id',
      connectionId: 'connection_id', privacy: 'privacy', isDefault: 'is_default', metadata: 'metadata',
      keyframeIntervalSeconds: 'keyframe_interval_seconds', audioBitrateKbps: 'audio_bitrate_kbps',
      audioSampleRateHz: 'audio_sample_rate_hz', adaptiveBitrateEnabled: 'adaptive_bitrate_enabled',
      audioConfig: 'audio_config', facecamConfig: 'facecam_config',
    };
    for (const [input, column] of Object.entries(map)) if (input in body) patch[column] = body[input];

    const rows = await this.db.adminRest<any[]>(
      `ul_stream_configs?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(patch) },
    );
    if (!rows?.length) throw new NotFoundException('Stream config not found');
    return rows[0];
  }
}
