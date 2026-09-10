import { Controller, Get, Headers, Param, Post, Query } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { StreamHistoryV2Service } from './stream-history-v2.service';

@Controller('streams/history')
export class StreamHistoryV2Controller {
  constructor(private readonly service: StreamHistoryV2Service) {}

  @Get()
  list(
    @Headers('authorization') auth: string | undefined,
    @Query('limit') limit?: string,
  ) {
    return this.service.list(bearerToken(auth), Number(limit || 50));
  }

  @Get(':sessionId')
  detail(
    @Headers('authorization') auth: string | undefined,
    @Param('sessionId') sessionId: string,
  ) {
    return this.service.detail(bearerToken(auth), sessionId);
  }

  @Post(':sessionId/finalize')
  finalize(
    @Headers('authorization') auth: string | undefined,
    @Param('sessionId') sessionId: string,
  ) {
    return this.service.finalize(bearerToken(auth), sessionId);
  }
}
