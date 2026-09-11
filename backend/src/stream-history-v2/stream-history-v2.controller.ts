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
    @Query('offset') offset?: string,
    @Query('status') status?: string,
    @Query('from') from?: string,
    @Query('to') to?: string,
  ) {
    return this.service.list(bearerToken(auth), {
      limit: Number(limit || 25), offset: Number(offset || 0), status, from, to,
    });
  }

  @Get(':sessionId')
  detail(@Headers('authorization') auth: string | undefined, @Param('sessionId') sessionId: string) {
    return this.service.detail(bearerToken(auth), sessionId);
  }

  @Get(':sessionId/analytics')
  analytics(@Headers('authorization') auth: string | undefined, @Param('sessionId') sessionId: string) {
    return this.service.analytics(bearerToken(auth), sessionId);
  }

  @Post(':sessionId/finalize')
  finalize(@Headers('authorization') auth: string | undefined, @Param('sessionId') sessionId: string) {
    return this.service.finalize(bearerToken(auth), sessionId);
  }
}
