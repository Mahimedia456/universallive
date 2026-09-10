import { Body, Controller, Get, Headers, Param, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { PlatformOauthService } from './platform-oauth.service';

@Controller('streaming/oauth')
export class PlatformOauthController {
  constructor(private readonly service: PlatformOauthService) {}

  @Post(':platform/start')
  start(
    @Headers('authorization') auth: string | undefined,
    @Param('platform') platform: string,
    @Body() body: { redirectUri?: string },
  ) {
    return this.service.begin(bearerToken(auth), platform, body?.redirectUri);
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
