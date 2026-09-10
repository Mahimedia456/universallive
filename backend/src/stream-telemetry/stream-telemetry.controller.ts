import { Body, Controller, Get, Headers, Param, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { StreamTelemetryService } from './stream-telemetry.service';

@Controller('streams/sessions/:sessionId')
export class StreamTelemetryController {
  constructor(private readonly service: StreamTelemetryService) {}

  @Post('telemetry')
  sample(
    @Headers('authorization') auth: string | undefined,
    @Param('sessionId') sessionId: string,
    @Body() body: any,
  ) {
    return this.service.sample(bearerToken(auth), sessionId, body);
  }

  @Post('events')
  event(
    @Headers('authorization') auth: string | undefined,
    @Param('sessionId') sessionId: string,
    @Body() body: any,
  ) {
    return this.service.event(bearerToken(auth), sessionId, body);
  }

  @Get('health')
  latest(
    @Headers('authorization') auth: string | undefined,
    @Param('sessionId') sessionId: string,
  ) {
    return this.service.latest(bearerToken(auth), sessionId);
  }
}
