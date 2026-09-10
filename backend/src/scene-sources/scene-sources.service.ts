import {
  BadRequestException,
  Injectable,
  NotFoundException,
} from '@nestjs/common';

import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class SceneSourcesService {
  constructor(private readonly db: BackendSupabase) {}

  private async assertScene(token: string, sceneId: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_scenes?id=eq.${encodeURIComponent(sceneId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Scene not found');
    return user;
  }

  async list(token: string, sceneId: string) {
    await this.assertScene(token, sceneId);
    return this.db.adminRest<any[]>(
      `ul_scene_sources?scene_id=eq.${encodeURIComponent(sceneId)}&select=*&order=z_index.asc,created_at.asc`,
      { method: 'GET' },
    );
  }

  async create(token: string, sceneId: string, body: {
    sourceType: string;
    name: string;
    zIndex?: number;
    x?: number;
    y?: number;
    width?: number;
    height?: number;
    rotation?: number;
    opacity?: number;
    config?: Record<string, unknown>;
  }) {
    const user = await this.assertScene(token, sceneId);

    if (!body.sourceType?.trim()) throw new BadRequestException('sourceType is required');
    if (!body.name?.trim()) throw new BadRequestException('name is required');

    const rows = await this.db.adminRest<any[]>(
      'ul_scene_sources',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          scene_id: sceneId,
          source_type: body.sourceType,
          name: body.name,
          z_index: body.zIndex ?? 0,
          x: body.x ?? 0,
          y: body.y ?? 0,
          width: body.width ?? 1,
          height: body.height ?? 1,
          rotation: body.rotation ?? 0,
          opacity: body.opacity ?? 1,
          config: body.config || {},
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

    const map: Record<string, string> = {
      name: 'name',
      zIndex: 'z_index',
      isVisible: 'is_visible',
      isLocked: 'is_locked',
      x: 'x',
      y: 'y',
      width: 'width',
      height: 'height',
      rotation: 'rotation',
      opacity: 'opacity',
      config: 'config',
    };

    for (const [inputKey, dbKey] of Object.entries(map)) {
      if (inputKey in body) patch[dbKey] = body[inputKey];
    }

    const rows = await this.db.adminRest<any[]>(
      `ul_scene_sources?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify(patch),
      },
    );

    if (!rows?.length) throw new NotFoundException('Source not found');
    return rows[0];
  }

  async reorder(token: string, sceneId: string, orderedIds: string[]) {
    const user = await this.assertScene(token, sceneId);

    for (let i = 0; i < orderedIds.length; i += 1) {
      await this.db.adminRest(
        `ul_scene_sources?id=eq.${encodeURIComponent(orderedIds[i])}&scene_id=eq.${encodeURIComponent(sceneId)}&user_id=eq.${encodeURIComponent(user.id)}`,
        {
          method: 'PATCH',
          body: JSON.stringify({
            z_index: i,
            updated_at: new Date().toISOString(),
          }),
        },
      );
    }

    return this.list(token, sceneId);
  }

  async remove(token: string, id: string) {
    const user = await this.db.currentUser(token);
    await this.db.adminRest(
      `ul_scene_sources?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'DELETE' },
    );
    return { deleted: true };
  }
}
