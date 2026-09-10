import { Body, Controller, Get, Headers, Param, Patch, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { BroadcastSessionsService } from './broadcast-sessions.service';

@Controller('streams/sessions')
export class BroadcastSessionsController {
  constructor(private readonly service: BroadcastSessionsService) {}

  @Post()
  create(@Headers('authorization') auth: string | undefined, @Body() body: any) {
    return this.service.create(bearerToken(auth), body);
  }

  @Get('active/current')
  current(@Headers('authorization') auth: string | undefined) {
    return this.service.current(bearerToken(auth));
  }

  @Get(':id')
  get(@Headers('authorization') auth: string | undefined, @Param('id') id: string) {
    return this.service.get(bearerToken(auth), id);
  }

  @Post(':id/start')
  start(@Headers('authorization') auth: string | undefined, @Param('id') id: string) {
    return this.service.start(bearerToken(auth), id);
  }

  @Post(':id/heartbeat')
  heartbeat(@Headers('authorization') auth: string | undefined, @Param('id') id: string) {
    return this.service.heartbeat(bearerToken(auth), id);
  }

  @Post(':id/end')
  end(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
    @Body() body: { stopReason?: string },
  ) {
    return this.service.end(bearerToken(auth), id, body?.stopReason);
  }

  @Patch(':sessionId/destinations/:destinationId')
  destination(
    @Headers('authorization') auth: string | undefined,
    @Param('sessionId') sessionId: string,
    @Param('destinationId') destinationId: string,
    @Body() body: any,
  ) {
    return this.service.setDestinationStatus(
      bearerToken(auth),
      sessionId,
      destinationId,
      body,
    );
  }
}
