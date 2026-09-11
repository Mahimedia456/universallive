import { BadRequestException, Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { SupabaseService } from '../supabase/supabase.service';

@Injectable()
export class ProfilesV2Service {
  constructor(
    private readonly db: BackendSupabase,
    private readonly supabase: SupabaseService,
  ) {}

  async getProfile(accessToken: string) {
    const user = await this.db.currentUser(accessToken);
    const rows = await this.db.adminRest<any[]>(
      `ul_creator_profiles?user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    return rows?.[0] || {
      user_id: user.id,
      display_name: user.user_metadata.full_name || null,
      username: user.user_metadata.username || null,
      avatar_url: null,
      creator_type: null,
      onboarding_completed: false,
    };
  }

  async upsertProfile(
    accessToken: string,
    input: {
      displayName?: string;
      username?: string;
      avatarUrl?: string | null;
      creatorType?: string | null;
      onboardingCompleted?: boolean;
      locale?: string | null;
      timezone?: string | null;
      bio?: string | null;
      websiteUrl?: string | null;
    },
  ) {
    const user = await this.db.currentUser(accessToken);
    const payload: Record<string, unknown> = {
      user_id: user.id,
      updated_at: new Date().toISOString(),
    };

    if (input.displayName !== undefined) {
      const value = input.displayName.trim();
      if (!value) throw new BadRequestException('displayName is required');
      payload.display_name = value.slice(0, 120);
    }
    if (input.username !== undefined) {
      const value = input.username.trim().replace(/^@+/, '').toLowerCase();
      if (!value) throw new BadRequestException('username is required');
      payload.username = value.slice(0, 60);
    }
    if (input.avatarUrl !== undefined) payload.avatar_url = input.avatarUrl;
    if (input.creatorType !== undefined) payload.creator_type = input.creatorType;
    if (input.onboardingCompleted !== undefined) {
      payload.onboarding_completed = input.onboardingCompleted;
    }
    if (input.locale !== undefined) payload.locale = input.locale;
    if (input.timezone !== undefined) payload.timezone = input.timezone;
    if (input.bio !== undefined) payload.bio = input.bio?.trim().slice(0, 500) || null;
    if (input.websiteUrl !== undefined) payload.website_url = input.websiteUrl?.trim().slice(0, 300) || null;

    const rows = await this.db.adminRest<any[]>(
      'ul_creator_profiles?on_conflict=user_id',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify(payload),
      },
    );
    return rows?.[0] || null;
  }

  async avatarUpload(accessToken: string, extension = 'webp') {
    const user = await this.db.currentUser(accessToken);
    const safeExt = ['webp', 'jpg', 'jpeg', 'png'].includes(String(extension).toLowerCase())
      ? String(extension).toLowerCase()
      : 'webp';
    const path = `${user.id}/avatar-${Date.now()}.${safeExt}`;
    const { data, error } = await this.supabase.admin.storage
      .from('ul-profile-avatars')
      .createSignedUploadUrl(path);
    if (error || !data) throw error || new Error('Could not create avatar upload URL');
    return { path, token: data.token, signedUrl: data.signedUrl };
  }

  async finalizeAvatar(accessToken: string, path: string) {
    const user = await this.db.currentUser(accessToken);
    const normalized = String(path || '').trim();
    if (!normalized.startsWith(`${user.id}/`)) {
      throw new BadRequestException('Invalid avatar path');
    }
    const { data } = this.supabase.admin.storage.from('ul-profile-avatars').getPublicUrl(normalized);
    return this.upsertProfile(accessToken, { avatarUrl: data.publicUrl });
  }

}
