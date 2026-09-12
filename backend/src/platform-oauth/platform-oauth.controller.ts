import { Body, Controller, Get, Headers, Param, Post, Query, Res } from '@nestjs/common';
import type { Response } from 'express';
import { bearerToken } from '../common/backend-supabase';
import { PlatformOauthService } from './platform-oauth.service';

@Controller('streaming/oauth')
export class PlatformOauthController {
  constructor(private readonly service: PlatformOauthService) {}

  @Post(':platform/start')
  start(
    @Headers('authorization') auth: string | undefined,
    @Param('platform') platform: string,
  ) {
    return this.service.begin(bearerToken(auth), platform);
  }

  @Get(':platform/status')
  status(
    @Headers('authorization') auth: string | undefined,
    @Param('platform') platform: string,
  ) {
    return this.service.status(bearerToken(auth), platform);
  }

  @Get(':platform/callback')
  async callback(
    @Param('platform') platform: string,
    @Query('code') code: string | undefined,
    @Query('state') state: string | undefined,
    @Query('error') error: string | undefined,
    @Query('error_description') errorDescription: string | undefined,
    @Res() res: Response,
  ) {
    try {
      const result = await this.service.callback(platform, {
        code,
        state,
        error,
        errorDescription,
      });
      res
        .status(200)
        .type('html')
        .send(this.service.successHtml(result));
    } catch (err: any) {
      res
        .status(400)
        .type('html')
        .send(this.service.errorHtml(err?.message || 'OAuth connection failed'));
    }
  }

  @Post(':platform/disconnect')
  disconnect(
    @Headers('authorization') auth: string | undefined,
    @Param('platform') platform: string,
  ) {
    return this.service.disconnect(bearerToken(auth), platform);
  }

  @Get(':platform/channels')
  channels(
    @Headers('authorization') auth: string | undefined,
    @Param('platform') platform: string,
  ) {
    return this.service.channels(bearerToken(auth), platform);
  }

  @Post('connections/:connectionId/select-channel')
  selectChannel(
    @Headers('authorization') auth: string | undefined,
    @Param('connectionId') connectionId: string,
    @Body() body: { channelId: string },
  ) {
    return this.service.selectChannel(
      bearerToken(auth),
      connectionId,
      body.channelId,
    );
  }
}
