import { Body, Controller, Get, Headers, Param, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { RtmpCredentialsService } from './rtmp-credentials.service';

@Controller('streaming/rtmp')
export class RtmpCredentialsController {
  constructor(private readonly service: RtmpCredentialsService) {}

  @Post()
  save(
    @Headers('authorization') auth: string | undefined,
    @Body() body: {
      connectionId: string;
      serverUrl: string;
      streamKey: string;
    },
  ) {
    return this.service.save(bearerToken(auth), body);
  }

  @Get(':connectionId/status')
  status(
    @Headers('authorization') auth: string | undefined,
    @Param('connectionId') connectionId: string,
  ) {
    return this.service.status(bearerToken(auth), connectionId);
  }

  @Get(':connectionId/publish-config')
  publishConfig(
    @Headers('authorization') auth: string | undefined,
    @Param('connectionId') connectionId: string,
  ) {
    return this.service.publishConfig(bearerToken(auth), connectionId);
  }
}
