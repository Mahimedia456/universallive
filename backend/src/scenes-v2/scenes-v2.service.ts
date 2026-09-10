import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';

import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class ScenesV2Service {
  constructor(private readonly db: BackendSupabase) {}

  async list(token: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_scenes?user_id=eq.${encodeURIComponent(user.id)}&is_archived=eq.false&select=*&order=sort_order.asc,created_at.asc`,
      { method: 'GET' },
    );
  }

  async get(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_scenes?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Scene not found');
    return rows[0];
  }

  async create(token: string, body: {
    name: string;
    description?: string;
    aspectRatio?: string;
    width?: number;
    height?: number;
    isDefault?: boolean;
    templateKey?: string | null;
  }) {
    const user = await this.db.currentUser(token);

    if (!body.name?.trim()) {
      throw new BadRequestException('Scene name is required');
    }

    if (body.isDefault) {
      await this.db.adminRest(
        `ul_scenes?user_id=eq.${encodeURIComponent(user.id)}&is_default=eq.true`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            is_default: false,
            updated_at: new Date().toISOString(),
          }),
        },
      );
    }

    const rows = await this.db.adminRest<any[]>(
      'ul_scenes',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          name: body.name.trim(),
          description: body.description || null,
          aspect_ratio: body.aspectRatio || '16:9',
          width: body.width || 1920,
          height: body.height || 1080,
          is_default: !!body.isDefault,
          template_key: body.templateKey || null,
        }),
      },
    );

    return rows?.[0];
  }

  async update(token: string, id: string, body: Record<string, unknown>) {
    const user = await this.db.currentUser(token);
    const patch: Record<string, unknown> = {
      updated_at: new Date().toISOString(),
    };

    if ('name' in body) patch.name = body.name;
    if ('description' in body) patch.description = body.description;
    if ('isDefault' in body) patch.is_default = body.isDefault;
    if ('sortOrder' in body) patch.sort_order = body.sortOrder;
    if ('thumbnailUrl' in body) patch.thumbnail_url = body.thumbnailUrl;
    if ('isArchived' in body) patch.is_archived = body.isArchived;

    const rows = await this.db.adminRest<any[]>(
      `ul_scenes?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify(patch),
      },
    );

    if (!rows?.length) throw new NotFoundException('Scene not found');
    return rows[0];
  }

  async duplicate(token: string, id: string) {
    const scene = await this.get(token, id);
    return this.create(token, {
      name: `${scene.name} Copy`,
      description: scene.description || undefined,
      aspectRatio: scene.aspect_ratio,
      width: scene.width,
      height: scene.height,
      isDefault: false,
      templateKey: scene.template_key,
    });
  }

  async remove(token: string, id: string) {
    return this.update(token, id, { isArchived: true });
  }
}
