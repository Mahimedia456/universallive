import { Body, Controller, Get, Patch, UseGuards } from '@nestjs/common';
import type { User } from '@supabase/supabase-js';
import { AuthUser } from '../auth/auth-user.decorator';
import { UniversalLiveAuthGuard } from '../auth/universallive-auth.guard';
import { SupabaseService } from '../supabase/supabase.service';
import { UpdateProfileDto } from './profile.dto';

@Controller('profile')
@UseGuards(UniversalLiveAuthGuard)
export class ProfilesController {
  constructor(private readonly supabase: SupabaseService) {}

  @Get()
  async getProfile(@AuthUser() user: User) {
    const { data, error } = await this.supabase.admin.from('profiles').select('*').eq('id', user.id).single();
    if (error) throw error;
    return { profile: data };
  }

  @Patch()
  async updateProfile(@AuthUser() user: User, @Body() dto: UpdateProfileDto) {
    const patch: Record<string, unknown> = { updated_at: new Date().toISOString() };
    if (dto.displayName !== undefined) patch.display_name = dto.displayName;
    if (dto.bio !== undefined) patch.bio = dto.bio;
    if (dto.avatarUrl !== undefined) patch.avatar_url = dto.avatarUrl;
    const { data, error } = await this.supabase.admin.from('profiles').update(patch).eq('id', user.id).select('*').single();
    if (error) throw error;
    return { profile: data };
  }
}
