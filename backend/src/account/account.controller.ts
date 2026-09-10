import { Controller, Get, Param, UseGuards } from '@nestjs/common';
import type { User } from '@supabase/supabase-js';
import { AuthUser } from '../auth/auth-user.decorator';
import { SupabaseAuthGuard } from '../auth/supabase-auth.guard';
import { SupabaseService } from '../supabase/supabase.service';

@Controller('account')
@UseGuards(SupabaseAuthGuard)
export class AccountController {
  constructor(private readonly supabase: SupabaseService) {}

  @Get('overview')
  async overview(@AuthUser() user: User) {
    const [profile, destinations, sessions, scenes] = await Promise.all([
      this.supabase.admin.from('profiles').select('*').eq('id', user.id).single(),
      this.supabase.admin.from('stream_destinations').select('id', { count: 'exact', head: true }).eq('user_id', user.id),
      this.supabase.admin.from('stream_sessions').select('id', { count: 'exact', head: true }).eq('user_id', user.id),
      this.supabase.admin.from('scenes').select('id', { count: 'exact', head: true }).eq('user_id', user.id),
    ]);
    if (profile.error) throw profile.error;
    return {
      profile: profile.data,
      stats: {
        destinations: destinations.count ?? 0,
        streams: sessions.count ?? 0,
        scenes: scenes.count ?? 0,
      },
    };
  }

  @Get('streams/:id')
  async streamDetail(@AuthUser() user: User, @Param('id') id: string) {
    const { data: stream, error } = await this.supabase.admin.from('stream_sessions').select('*').eq('id', id).eq('user_id', user.id).single();
    if (error) throw error;
    const { data: metrics, error: metricsError } = await this.supabase.admin.from('stream_metrics').select('*').eq('stream_session_id', id).order('created_at', { ascending: false }).limit(300);
    if (metricsError) throw metricsError;
    return { stream, metrics: metrics ?? [] };
  }
}
