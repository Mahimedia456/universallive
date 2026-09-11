import { Body, Controller, Get, Param, Patch, Post, UseGuards } from '@nestjs/common';
import type { User } from '@supabase/supabase-js';
import { AuthUser } from '../auth/auth-user.decorator';
import { UniversalLiveAuthGuard } from '../auth/universallive-auth.guard';
import { SupabaseService } from '../supabase/supabase.service';
import { StartStreamDto, StreamMetricDto, UpdateStreamStatusDto } from './stream.dto';

@Controller('streams')
@UseGuards(UniversalLiveAuthGuard)
export class StreamsController {
  constructor(private readonly supabase: SupabaseService) {}

  @Post('start')
  async start(@AuthUser() user: User, @Body() dto: StartStreamDto) {
    const payload = {
      user_id: user.id,
      destination_id: dto.destinationId ?? null,
      title: dto.title ?? null,
      platform: dto.platform ?? 'custom',
      status: 'starting',
      target_bitrate_kbps: dto.targetBitrateKbps ?? null,
      fps: dto.fps ?? null,
      width: dto.width ?? null,
      height: dto.height ?? null,
      started_at: new Date().toISOString(),
      last_seen_at: new Date().toISOString(),
    };
    const { data, error } = await this.supabase.admin.from('stream_sessions').insert(payload).select('*').single();
    if (error) throw error;
    return { stream: data };
  }

  @Patch(':id/status')
  async status(@AuthUser() user: User, @Param('id') id: string, @Body() dto: UpdateStreamStatusDto) {
    const patch: Record<string, unknown> = {
      status: dto.status,
      status_message: dto.message ?? null,
      last_seen_at: new Date().toISOString(),
    };
    if (dto.status === 'live') patch.live_at = new Date().toISOString();
    if (dto.status === 'ended' || dto.status === 'error') patch.ended_at = new Date().toISOString();
    const { data, error } = await this.supabase.admin.from('stream_sessions').update(patch).eq('id', id).eq('user_id', user.id).select('*').single();
    if (error) throw error;
    return { stream: data };
  }

  @Post(':id/metrics')
  async metrics(@AuthUser() user: User, @Param('id') id: string, @Body() dto: StreamMetricDto) {
    const { data: owned, error: ownedError } = await this.supabase.admin.from('stream_sessions').select('id').eq('id', id).eq('user_id', user.id).single();
    if (ownedError || !owned) throw ownedError ?? new Error('Stream not found');

    const payload = {
      stream_session_id: id,
      bitrate_kbps: dto.bitrateKbps ?? null,
      published_video_frames: dto.publishedVideoFrames ?? null,
      published_audio_frames: dto.publishedAudioFrames ?? null,
      dropped_frames: dto.droppedFrames ?? null,
      reconnect_count: dto.reconnectCount ?? null,
      health: dto.health ?? null,
    };
    const { data, error } = await this.supabase.admin.from('stream_metrics').insert(payload).select('*').single();
    if (error) throw error;
    await this.supabase.admin.from('stream_sessions').update({ last_seen_at: new Date().toISOString() }).eq('id', id);
    return { metric: data };
  }

  @Post(':id/stop')
  async stop(@AuthUser() user: User, @Param('id') id: string) {
    const { data, error } = await this.supabase.admin.from('stream_sessions').update({ status: 'ended', ended_at: new Date().toISOString(), last_seen_at: new Date().toISOString() }).eq('id', id).eq('user_id', user.id).select('*').single();
    if (error) throw error;
    return { stream: data };
  }

  @Get('active')
  async active(@AuthUser() user: User) {
    const { data, error } = await this.supabase.admin.from('stream_sessions').select('*').eq('user_id', user.id).in('status', ['starting', 'connecting', 'live', 'reconnecting', 'stopping']).order('created_at', { ascending: false }).limit(1).maybeSingle();
    if (error) throw error;
    return { stream: data ?? null };
  }

  @Get('history')
  async history(@AuthUser() user: User) {
    const { data, error } = await this.supabase.admin.from('stream_sessions').select('*').eq('user_id', user.id).order('created_at', { ascending: false }).limit(100);
    if (error) throw error;
    return { streams: data ?? [] };
  }
}
