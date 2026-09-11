import { Body, Controller, Delete, Get, Param, Post, UseGuards } from '@nestjs/common';
import type { User } from '@supabase/supabase-js';
import { AuthUser } from '../auth/auth-user.decorator';
import { UniversalLiveAuthGuard } from '../auth/universallive-auth.guard';
import { SupabaseService } from '../supabase/supabase.service';

@Controller('destinations')
@UseGuards(UniversalLiveAuthGuard)
export class DestinationsController {
  constructor(private readonly supabase: SupabaseService) {}
  @Get() async list(@AuthUser() user: User) {
    const { data, error } = await this.supabase.admin.from('stream_destinations').select('*').eq('user_id', user.id).order('created_at');
    if (error) throw error; return { destinations: data ?? [] };
  }
  @Post() async upsert(@AuthUser() user: User, @Body() body: any) {
    const row = { id: body.id || undefined, user_id: user.id, platform: body.platform || 'custom', label: body.label || body.platform || 'Custom', server_url: body.serverUrl || null, secret_ref: body.secretRef || null, enabled: body.enabled !== false };
    const { data, error } = await this.supabase.admin.from('stream_destinations').upsert(row).select('*').single();
    if (error) throw error; return { destination: data };
  }
  @Delete(':id') async remove(@AuthUser() user: User, @Param('id') id: string) {
    const { error } = await this.supabase.admin.from('stream_destinations').delete().eq('id', id).eq('user_id', user.id); if (error) throw error; return { ok: true };
  }
}
