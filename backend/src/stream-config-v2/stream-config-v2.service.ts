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

  async create(token: string, body: any) {
    const user = await this.db.currentUser(token);

    const fps = Number(body.fps ?? 30);
    const bitrate = Number(body.bitrateKbps ?? 6800);

    if (![30, 60].includes(fps)) {
      throw new BadRequestException('fps must be 30 or 60');
    }
    if (bitrate < 1000 || bitrate > 30000) {
      throw new BadRequestException('bitrateKbps is out of range');
    }

    if (body.isDefault) {
      await this.db.adminRest(
        `ul_stream_configs?user_id=eq.${encodeURIComponent(user.id)}&is_default=eq.true`,
        {
          method: 'PATCH',
          body: JSON.stringify({ is_default: false, updated_at: new Date().toISOString() }),
        },
      );
    }

    const rows = await this.db.adminRest<any[]>(
      'ul_stream_configs',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          name: body.name || 'Default',
          resolution: body.resolution || '1080p',
          width: body.width || 1920,
          height: body.height || 1080,
          fps,
          bitrate_kbps: bitrate,
          orientation: body.orientation || 'auto',
          microphone_enabled: body.microphoneEnabled ?? true,
          internal_audio_enabled: body.internalAudioEnabled ?? true,
          facecam_enabled: body.facecamEnabled ?? false,
          scene_id: body.sceneId || null,
          connection_id: body.connectionId || null,
          privacy: body.privacy || 'public',
          is_default: !!body.isDefault,
          metadata: body.metadata || {},
        }),
      },
    );

    return rows?.[0];
  }

  async update(token: string, id: string, body: any) {
    const user = await this.db.currentUser(token);
    const patch: any = { updated_at: new Date().toISOString() };

    const map: Record<string, string> = {
      name: 'name',
      resolution: 'resolution',
      width: 'width',
      height: 'height',
      fps: 'fps',
      bitrateKbps: 'bitrate_kbps',
      orientation: 'orientation',
      microphoneEnabled: 'microphone_enabled',
      internalAudioEnabled: 'internal_audio_enabled',
      facecamEnabled: 'facecam_enabled',
      sceneId: 'scene_id',
      connectionId: 'connection_id',
      privacy: 'privacy',
      isDefault: 'is_default',
      metadata: 'metadata',
    };

    for (const [input, column] of Object.entries(map)) {
      if (input in body) patch[column] = body[input];
    }

    const rows = await this.db.adminRest<any[]>(
      `ul_stream_configs?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(patch) },
    );

    if (!rows?.length) throw new NotFoundException('Stream config not found');
    return rows[0];
  }
}
