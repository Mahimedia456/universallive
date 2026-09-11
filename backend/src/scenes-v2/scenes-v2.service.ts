import {
  BadRequestException,
  ConflictException,
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
    if (!body.name?.trim()) throw new BadRequestException('Scene name is required');

    const existing = await this.list(token);
    const isDefault = body.isDefault === true || existing.length === 0;
    if (isDefault) await this.clearDefault(user.id);

    const rows = await this.db.adminRest<any[]>('ul_scenes', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        name: body.name.trim(),
        description: body.description || null,
        aspect_ratio: body.aspectRatio || '16:9',
        width: body.width || 1920,
        height: body.height || 1080,
        is_default: isDefault,
        sort_order: existing.length,
        template_key: body.templateKey || null,
      }),
    });
    const created = rows?.[0];
    if (created && (isDefault || existing.length === 0)) {
      await this.activate(token, created.id);
    }
    return created;
  }

  private async clearDefault(userId: string) {
    await this.db.adminRest(
      `ul_scenes?user_id=eq.${encodeURIComponent(userId)}&is_default=eq.true`,
      { method: 'PATCH', body: JSON.stringify({ is_default: false, updated_at: new Date().toISOString() }) },
    );
  }

  async update(token: string, id: string, body: Record<string, unknown>) {
    const user = await this.db.currentUser(token);
    await this.get(token, id);
    if (body.isDefault === true) await this.clearDefault(user.id);

    const patch: Record<string, unknown> = { updated_at: new Date().toISOString() };
    if ('name' in body) {
      const name = String(body.name || '').trim();
      if (!name) throw new BadRequestException('Scene name is required');
      patch.name = name;
    }
    if ('description' in body) patch.description = body.description || null;
    if ('isDefault' in body) patch.is_default = !!body.isDefault;
    if ('sortOrder' in body) patch.sort_order = Number(body.sortOrder || 0);
    if ('thumbnailUrl' in body) patch.thumbnail_url = body.thumbnailUrl || null;
    if ('isArchived' in body) patch.is_archived = !!body.isArchived;
    if ('metadata' in body && body.metadata && typeof body.metadata === 'object') patch.metadata = body.metadata;

    const rows = await this.db.adminRest<any[]>(
      `ul_scenes?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(patch) },
    );
    if (!rows?.length) throw new NotFoundException('Scene not found');
    return rows[0];
  }

  async activate(token: string, id: string) {
    const user = await this.db.currentUser(token);
    await this.get(token, id);
    const rows = await this.db.adminRest<any[]>(
      'ul_studio_workspaces?on_conflict=user_id',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({ user_id: user.id, active_scene_id: id, updated_at: new Date().toISOString() }),
      },
    );
    return { sceneId: id, workspace: rows?.[0] || null };
  }

  async reorder(token: string, orderedIds: string[]) {
    const user = await this.db.currentUser(token);
    const owned = await this.list(token);
    const ownedIds = new Set(owned.map((s: any) => s.id));
    const unique = [...new Set(orderedIds || [])];
    if (unique.some((id) => !ownedIds.has(id))) throw new BadRequestException('Scene order contains an invalid scene');
    for (let i = 0; i < unique.length; i += 1) {
      await this.db.adminRest(
        `ul_scenes?id=eq.${encodeURIComponent(unique[i])}&user_id=eq.${encodeURIComponent(user.id)}`,
        { method: 'PATCH', body: JSON.stringify({ sort_order: i, updated_at: new Date().toISOString() }) },
      );
    }
    return this.list(token);
  }

  async duplicate(token: string, id: string) {
    const scene = await this.get(token, id);
    const copy = await this.create(token, {
      name: `${scene.name} Copy`,
      description: scene.description || undefined,
      aspectRatio: scene.aspect_ratio,
      width: scene.width,
      height: scene.height,
      isDefault: false,
      templateKey: scene.template_key,
    });

    const user = await this.db.currentUser(token);
    const sources = await this.db.adminRest<any[]>(
      `ul_scene_sources?scene_id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*&order=z_index.asc`,
      { method: 'GET' },
    );
    for (const source of sources || []) {
      const { id: _id, created_at: _created, updated_at: _updated, ...rest } = source;
      await this.db.adminRest('ul_scene_sources', {
        method: 'POST',
        body: JSON.stringify({ ...rest, scene_id: copy.id, source_key: source.source_key ? `${source.source_key}-copy-${copy.id.slice(0, 6)}` : null }),
      });
    }
    return copy;
  }

  async remove(token: string, id: string) {
    const scenes = await this.list(token);
    if (scenes.length <= 1) throw new ConflictException('Keep at least one scene');
    const target = scenes.find((s: any) => s.id === id);
    if (!target) throw new NotFoundException('Scene not found');
    await this.update(token, id, { isArchived: true, isDefault: false });

    const replacement = scenes.find((s: any) => s.id !== id);
    if (target.is_default && replacement) await this.update(token, replacement.id, { isDefault: true });
    if (replacement) await this.activate(token, replacement.id);
    return { deleted: true, activeSceneId: replacement?.id || null };
  }
}
