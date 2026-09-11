import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

const ALLOWED_SOURCE_TYPES = new Set([
  'screen', 'camera', 'facecam', 'text', 'image', 'logo', 'browser', 'background',
  'chat', 'alert', 'goal', 'lower_third', 'media', 'audio', 'overlay',
]);

@Injectable()
export class SceneSourcesService {
  constructor(private readonly db: BackendSupabase) {}

  private async assertScene(token: string, sceneId: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_scenes?id=eq.${encodeURIComponent(sceneId)}&user_id=eq.${encodeURIComponent(user.id)}&is_archived=eq.false&select=id`,
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

  async create(token: string, sceneId: string, body: any) {
    const user = await this.assertScene(token, sceneId);
    const sourceType = String(body.sourceType || '').trim().toLowerCase();
    if (!ALLOWED_SOURCE_TYPES.has(sourceType)) throw new BadRequestException('Unsupported sourceType');
    if (!String(body.name || '').trim()) throw new BadRequestException('name is required');

    const existing = await this.list(token, sceneId);
    const rows = await this.db.adminRest<any[]>('ul_scene_sources', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        scene_id: sceneId,
        source_type: sourceType,
        source_key: body.sourceKey || null,
        source_category: body.sourceCategory || this.categoryFor(sourceType),
        name: String(body.name).trim(),
        z_index: body.zIndex ?? existing.length,
        is_visible: body.isVisible !== false,
        is_locked: !!body.isLocked,
        x: this.unit(body.x, 0),
        y: this.unit(body.y, 0),
        width: this.unit(body.width, 1),
        height: this.unit(body.height, 1),
        rotation: Number(body.rotation ?? 0),
        opacity: this.unit(body.opacity, 1),
        blend_mode: body.blendMode || 'normal',
        config: body.config && typeof body.config === 'object' ? body.config : {},
      }),
    });
    return rows?.[0];
  }

  private categoryFor(type: string) {
    if (['text', 'image', 'logo', 'chat', 'alert', 'goal', 'lower_third', 'overlay'].includes(type)) return 'overlay';
    if (['screen', 'camera', 'facecam', 'browser', 'media'].includes(type)) return 'visual';
    if (type === 'audio') return 'audio';
    return 'background';
  }

  private unit(value: unknown, fallback: number) {
    const n = Number(value);
    return Number.isFinite(n) ? Math.min(Math.max(n, 0), 1) : fallback;
  }

  async update(token: string, id: string, body: Record<string, unknown>) {
    const user = await this.db.currentUser(token);
    const patch: Record<string, unknown> = { updated_at: new Date().toISOString() };
    const map: Record<string, string> = {
      name: 'name', zIndex: 'z_index', isVisible: 'is_visible', isLocked: 'is_locked',
      x: 'x', y: 'y', width: 'width', height: 'height', rotation: 'rotation', opacity: 'opacity',
      config: 'config', sourceKey: 'source_key', sourceCategory: 'source_category', blendMode: 'blend_mode',
    };
    for (const [inputKey, dbKey] of Object.entries(map)) if (inputKey in body) patch[dbKey] = body[inputKey];

    const rows = await this.db.adminRest<any[]>(
      `ul_scene_sources?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify(patch) },
    );
    if (!rows?.length) throw new NotFoundException('Source not found');
    return rows[0];
  }

  async upsertByKey(token: string, sceneId: string, sourceKey: string, body: any) {
    const user = await this.assertScene(token, sceneId);
    const key = sourceKey.trim();
    if (!key) throw new BadRequestException('sourceKey is required');
    const found = await this.db.adminRest<any[]>(
      `ul_scene_sources?scene_id=eq.${encodeURIComponent(sceneId)}&user_id=eq.${encodeURIComponent(user.id)}&source_key=eq.${encodeURIComponent(key)}&select=id`,
      { method: 'GET' },
    );
    if (found?.length) return this.update(token, found[0].id, { ...body, sourceKey: key });
    return this.create(token, sceneId, { ...body, sourceKey: key });
  }

  async duplicate(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_scene_sources?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Source not found');
    const source = rows[0];
    return this.create(token, source.scene_id, {
      sourceType: source.source_type,
      name: `${source.name} Copy`,
      sourceCategory: source.source_category,
      x: Number(source.x), y: Number(source.y), width: Number(source.width), height: Number(source.height),
      rotation: Number(source.rotation), opacity: Number(source.opacity), blendMode: source.blend_mode,
      config: source.config || {},
    });
  }

  async reorder(token: string, sceneId: string, orderedIds: string[]) {
    const user = await this.assertScene(token, sceneId);
    const current = await this.list(token, sceneId);
    const valid = new Set(current.map((s: any) => s.id));
    const unique = [...new Set(orderedIds || [])];
    if (unique.some((id) => !valid.has(id))) throw new BadRequestException('Layer order contains an invalid source');
    for (let i = 0; i < unique.length; i += 1) {
      await this.db.adminRest(
        `ul_scene_sources?id=eq.${encodeURIComponent(unique[i])}&scene_id=eq.${encodeURIComponent(sceneId)}&user_id=eq.${encodeURIComponent(user.id)}`,
        { method: 'PATCH', body: JSON.stringify({ z_index: i, updated_at: new Date().toISOString() }) },
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
