import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';

import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class AssetsV2Service {
  constructor(private readonly db: BackendSupabase) {}

  async listAssets(token: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_creator_assets?user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.desc`,
      { method: 'GET' },
    );
  }

  async registerAsset(token: string, body: {
    assetType: string;
    name: string;
    storageBucket?: string;
    storagePath: string;
    publicUrl?: string | null;
    mimeType?: string | null;
    fileSizeBytes?: number | null;
    width?: number | null;
    height?: number | null;
  }) {
    const user = await this.db.currentUser(token);

    if (!body.storagePath?.trim()) {
      throw new BadRequestException('storagePath is required');
    }

    const rows = await this.db.adminRest<any[]>(
      'ul_creator_assets',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          asset_type: body.assetType,
          name: body.name,
          storage_bucket: body.storageBucket || 'universal-live-assets',
          storage_path: body.storagePath,
          public_url: body.publicUrl || null,
          mime_type: body.mimeType || null,
          file_size_bytes: body.fileSizeBytes || null,
          width: body.width || null,
          height: body.height || null,
        }),
      },
    );

    return rows?.[0];
  }

  async removeAsset(token: string, id: string) {
    const user = await this.db.currentUser(token);
    await this.db.adminRest(
      `ul_creator_assets?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'DELETE' },
    );
    return { deleted: true };
  }

  async presets(token: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_scene_presets?is_active=eq.true&or=(is_system.eq.true,owner_user_id.eq.${encodeURIComponent(user.id)})&select=*&order=is_system.desc,name.asc`,
      { method: 'GET' },
    );
  }

  async createFromPreset(token: string, presetId: string, name?: string) {
    const user = await this.db.currentUser(token);

    const presets = await this.db.adminRest<any[]>(
      `ul_scene_presets?id=eq.${encodeURIComponent(presetId)}&is_active=eq.true&select=*`,
      { method: 'GET' },
    );

    if (!presets?.length) throw new NotFoundException('Preset not found');

    const preset = presets[0];

    const scenes = await this.db.adminRest<any[]>(
      'ul_scenes',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          name: name?.trim() || preset.name,
          description: preset.description,
          aspect_ratio: preset.scene_payload?.aspectRatio || '16:9',
          width: 1920,
          height: 1080,
          template_key: preset.preset_key,
          is_default: false,
        }),
      },
    );

    return scenes?.[0];
  }
}
