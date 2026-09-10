import { Controller, Get, Param, Post, UseGuards } from '@nestjs/common';
import { SupabaseService } from '../supabase/supabase.service';
import { AdminGuard } from './admin.guard';

@Controller('admin')
@UseGuards(AdminGuard)
export class AdminController {
  constructor(private readonly supabase: SupabaseService) {}

  @Get('overview')
  async overview() {
    const [profiles, live, sessions, destinations] = await Promise.all([
      this.supabase.admin.from('profiles').select('id', { count: 'exact', head: true }),
      this.supabase.admin.from('stream_sessions').select('id', { count: 'exact', head: true }).in('status', ['starting','connecting','live','reconnecting','stopping']),
      this.supabase.admin.from('stream_sessions').select('id', { count: 'exact', head: true }),
      this.supabase.admin.from('stream_destinations').select('id', { count: 'exact', head: true }),
    ]);
    return { users: profiles.count ?? 0, activeStreams: live.count ?? 0, totalStreams: sessions.count ?? 0, destinations: destinations.count ?? 0 };
  }

  @Get('streams/live')
  async liveStreams() {
    const { data, error } = await this.supabase.admin.from('stream_sessions').select('*, profiles:user_id(display_name,avatar_url)').in('status', ['starting','connecting','live','reconnecting','stopping']).order('created_at', { ascending: false }).limit(200);
    if (error) throw error;
    return { streams: data ?? [] };
  }

  @Get('streams/recent')
  async recentStreams() {
    const { data, error } = await this.supabase.admin.from('stream_sessions').select('*, profiles:user_id(display_name)').order('created_at', { ascending: false }).limit(200);
    if (error) throw error;
    return { streams: data ?? [] };
  }

  @Get('streams/:id')
  async stream(@Param('id') id: string) {
    const { data: stream, error } = await this.supabase.admin.from('stream_sessions').select('*, profiles:user_id(display_name,avatar_url)').eq('id', id).single();
    if (error) throw error;
    const { data: metrics, error: metricError } = await this.supabase.admin.from('stream_metrics').select('*').eq('stream_session_id', id).order('created_at', { ascending: false }).limit(500);
    if (metricError) throw metricError;
    return { stream, metrics: metrics ?? [] };
  }

  @Post('streams/:id/mark-ended')
  async markEnded(@Param('id') id: string) {
    const now = new Date().toISOString();
    const { data, error } = await this.supabase.admin.from('stream_sessions').update({ status: 'ended', ended_at: now, last_seen_at: now, status_message: 'Ended by admin' }).eq('id', id).select('*').single();
    if (error) throw error;
    await this.supabase.admin.from('admin_audit_log').insert({ actor: 'admin-api', action: 'stream.mark-ended', target_type: 'stream_session', target_id: id });
    return { stream: data };
  }
}
