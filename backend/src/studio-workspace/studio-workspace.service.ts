import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class StudioWorkspaceService {
  constructor(private readonly db: BackendSupabase) {}

  private defaultWorkspace(userId: string) {
    return {
      user_id: userId,
      active_scene_id: null,
      quality_config_id: null,
      audio_config: {
        microphoneEnabled: true,
        internalAudioEnabled: true,
        microphoneGain: 1,
        internalAudioGain: 1,
        monitoringEnabled: false,
        preset: 'Streaming',
      },
      facecam_config: {
        enabled: false,
        lens: 'front',
        shape: 'rounded',
        size: 0.25,
        x: 0.72,
        y: 0.05,
        mirror: true,
        background: 'None',
      },
      adaptive_bitrate_enabled: true,
      autosave_enabled: true,
      metadata: {},
    };
  }

  async get(token: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_studio_workspaces?user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (rows?.length) return rows[0];

    const sceneRows = await this.db.adminRest<any[]>(
      `ul_scenes?user_id=eq.${encodeURIComponent(user.id)}&is_archived=eq.false&select=id&order=is_default.desc,sort_order.asc,created_at.asc&limit=1`,
      { method: 'GET' },
    );
    const configRows = await this.db.adminRest<any[]>(
      `ul_stream_configs?user_id=eq.${encodeURIComponent(user.id)}&select=id&order=is_default.desc,created_at.desc&limit=1`,
      { method: 'GET' },
    );

    const payload = {
      ...this.defaultWorkspace(user.id),
      active_scene_id: sceneRows?.[0]?.id || null,
      quality_config_id: configRows?.[0]?.id || null,
    };

    const created = await this.db.adminRest<any[]>('ul_studio_workspaces', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
    return created?.[0] || payload;
  }

  async update(token: string, body: any) {
    const user = await this.db.currentUser(token);
    await this.get(token);

    const patch: any = { updated_at: new Date().toISOString() };

    if ('activeSceneId' in body) {
      if (body.activeSceneId) {
        const scenes = await this.db.adminRest<any[]>(
          `ul_scenes?id=eq.${encodeURIComponent(body.activeSceneId)}&user_id=eq.${encodeURIComponent(user.id)}&is_archived=eq.false&select=id`,
          { method: 'GET' },
        );
        if (!scenes?.length) throw new NotFoundException('Scene not found');
      }
      patch.active_scene_id = body.activeSceneId || null;
    }

    if ('qualityConfigId' in body) {
      if (body.qualityConfigId) {
        const configs = await this.db.adminRest<any[]>(
          `ul_stream_configs?id=eq.${encodeURIComponent(body.qualityConfigId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id`,
          { method: 'GET' },
        );
        if (!configs?.length) throw new NotFoundException('Stream quality config not found');
      }
      patch.quality_config_id = body.qualityConfigId || null;
    }

    if ('audioConfig' in body) patch.audio_config = this.validateAudio(body.audioConfig);
    if ('facecamConfig' in body) patch.facecam_config = this.validateFacecam(body.facecamConfig);
    if ('adaptiveBitrateEnabled' in body) patch.adaptive_bitrate_enabled = !!body.adaptiveBitrateEnabled;
    if ('autosaveEnabled' in body) patch.autosave_enabled = !!body.autosaveEnabled;
    if ('metadata' in body && body.metadata && typeof body.metadata === 'object') patch.metadata = body.metadata;

    const rows = await this.db.adminRest<any[]>(
      `ul_studio_workspaces?user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(patch) },
    );
    return rows?.[0];
  }

  async activateScene(token: string, sceneId: string) {
    const updated = await this.update(token, { activeSceneId: sceneId });
    return { activeSceneId: updated?.active_scene_id || sceneId };
  }

  async audio(token: string) {
    const workspace = await this.get(token);
    return workspace.audio_config || this.defaultWorkspace(workspace.user_id).audio_config;
  }

  async saveAudio(token: string, body: any) {
    const config = this.validateAudio(body);
    await this.update(token, { audioConfig: config });
    return config;
  }

  async facecam(token: string) {
    const workspace = await this.get(token);
    return workspace.facecam_config || this.defaultWorkspace(workspace.user_id).facecam_config;
  }

  async saveFacecam(token: string, body: any) {
    const config = this.validateFacecam(body);
    await this.update(token, { facecamConfig: config });
    return config;
  }

  private validateAudio(value: any) {
    const v = value && typeof value === 'object' ? value : {};
    const gain = (n: unknown, fallback: number) => {
      const parsed = Number(n);
      return Number.isFinite(parsed) ? Math.min(Math.max(parsed, 0), 2) : fallback;
    };
    return {
      microphoneEnabled: v.microphoneEnabled !== false,
      internalAudioEnabled: v.internalAudioEnabled !== false,
      microphoneGain: gain(v.microphoneGain, 1),
      internalAudioGain: gain(v.internalAudioGain, 1),
      monitoringEnabled: !!v.monitoringEnabled,
      preset: String(v.preset || 'Streaming').slice(0, 80),
      noiseSuppression: v.noiseSuppression !== false,
      echoCancellation: v.echoCancellation !== false,
    };
  }

  private validateFacecam(value: any) {
    const v = value && typeof value === 'object' ? value : {};
    const num = (n: unknown, fallback: number, min: number, max: number) => {
      const parsed = Number(n);
      return Number.isFinite(parsed) ? Math.min(Math.max(parsed, min), max) : fallback;
    };
    const lens = ['front', 'back'].includes(String(v.lens)) ? String(v.lens) : 'front';
    const shape = ['rounded', 'circle', 'square'].includes(String(v.shape)) ? String(v.shape) : 'rounded';
    return {
      enabled: !!v.enabled,
      lens,
      shape,
      size: num(v.size, 0.25, 0.1, 0.6),
      x: num(v.x, 0.72, 0, 1),
      y: num(v.y, 0.05, 0, 1),
      mirror: v.mirror !== false,
      background: String(v.background || 'None').slice(0, 80),
    };
  }

  async quality(token: string) {
    const user = await this.db.currentUser(token);
    const workspace = await this.get(token);
    const target = workspace.quality_config_id
      ? `id=eq.${encodeURIComponent(workspace.quality_config_id)}&`
      : '';
    let rows = await this.db.adminRest<any[]>(
      `ul_stream_configs?${target}user_id=eq.${encodeURIComponent(user.id)}&select=*&order=is_default.desc,created_at.desc&limit=1`,
      { method: 'GET' },
    );
    if (!rows?.length && workspace.quality_config_id) {
      rows = await this.db.adminRest<any[]>(
        `ul_stream_configs?user_id=eq.${encodeURIComponent(user.id)}&select=*&order=is_default.desc,created_at.desc&limit=1`,
        { method: 'GET' },
      );
    }
    return rows?.[0] || null;
  }

  async saveQuality(token: string, body: any) {
    const user = await this.db.currentUser(token);
    const width = Number(body.width || 1920);
    const height = Number(body.height || 1080);
    const fps = Number(body.fps || 30);
    const bitrate = Number(body.bitrateKbps || 6800);
    if (![30, 60].includes(fps)) throw new BadRequestException('fps must be 30 or 60');
    if (bitrate < 1000 || bitrate > 30000) throw new BadRequestException('bitrateKbps is out of range');

    const workspace = await this.get(token);
    const payload = {
      user_id: user.id,
      name: body.name || 'Studio Default',
      resolution: body.resolution || `${height}p`,
      width,
      height,
      fps,
      bitrate_kbps: bitrate,
      orientation: body.orientation || 'auto',
      microphone_enabled: body.microphoneEnabled !== false,
      internal_audio_enabled: body.internalAudioEnabled !== false,
      facecam_enabled: !!body.facecamEnabled,
      scene_id: body.sceneId || workspace.active_scene_id || null,
      connection_id: body.connectionId || null,
      privacy: body.privacy || 'public',
      is_default: true,
      keyframe_interval_seconds: Number(body.keyframeIntervalSeconds || 2),
      audio_bitrate_kbps: Number(body.audioBitrateKbps || 160),
      audio_sample_rate_hz: Number(body.audioSampleRateHz || 48000),
      adaptive_bitrate_enabled: body.adaptiveBitrateEnabled !== false,
      audio_config: body.audioConfig || workspace.audio_config || {},
      facecam_config: body.facecamConfig || workspace.facecam_config || {},
      metadata: body.metadata || {},
      updated_at: new Date().toISOString(),
    };

    await this.db.adminRest(
      `ul_stream_configs?user_id=eq.${encodeURIComponent(user.id)}&is_default=eq.true`,
      { method: 'PATCH', body: JSON.stringify({ is_default: false, updated_at: new Date().toISOString() }) },
    );

    let rows: any[];
    if (workspace.quality_config_id) {
      rows = await this.db.adminRest<any[]>(
        `ul_stream_configs?id=eq.${encodeURIComponent(workspace.quality_config_id)}&user_id=eq.${encodeURIComponent(user.id)}`,
        { method: 'PATCH', body: JSON.stringify(payload) },
      );
    } else {
      rows = await this.db.adminRest<any[]>('ul_stream_configs', {
        method: 'POST',
        body: JSON.stringify({ ...payload, created_at: new Date().toISOString() }),
      });
    }

    const saved = rows?.[0];
    if (!saved) throw new BadRequestException('Could not save stream quality');
    await this.update(token, {
      qualityConfigId: saved.id,
      adaptiveBitrateEnabled: payload.adaptive_bitrate_enabled,
    });
    return saved;
  }
}
