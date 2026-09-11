import { Body, Controller, Delete, Get, Headers, Param, Post, Put } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { StreamLifecycleService } from './stream-lifecycle.service';

@Controller('streams')
export class StreamLifecycleController {
  constructor(private readonly service: StreamLifecycleService) {}

  @Get('draft/current')
  currentDraft(@Headers('authorization') auth: string | undefined) {
    return this.service.currentDraft(bearerToken(auth));
  }

  @Put('draft/current')
  saveDraft(
    @Headers('authorization') auth: string | undefined,
    @Body() body: any,
  ) {
    return this.service.saveDraft(bearerToken(auth), body);
  }

  @Delete('draft/current')
  clearDraft(@Headers('authorization') auth: string | undefined) {
    return this.service.clearDraft(bearerToken(auth));
  }

  @Post('preflight')
  preflight(
    @Headers('authorization') auth: string | undefined,
    @Body() body: any,
  ) {
    return this.service.preflight(bearerToken(auth), body);
  }

  @Get('preflight/:id')
  preflightDetail(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.service.preflightDetail(bearerToken(auth), id);
  }

  @Post('sessions/:id/publisher-state')
  publisherState(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
    @Body() body: any,
  ) {
    return this.service.publisherState(bearerToken(auth), id, body);
  }

  @Post('sessions/:id/recover')
  recover(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
    @Body() body: any,
  ) {
    return this.service.recover(bearerToken(auth), id, body);
  }

  @Get('sessions/:id/diagnostics')
  diagnostics(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.service.diagnostics(bearerToken(auth), id);
  }
}
