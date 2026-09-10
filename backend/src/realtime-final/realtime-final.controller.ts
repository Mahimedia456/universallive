import { Body, Controller, Get, Headers, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { RealtimeFinalService } from './realtime-final.service';

@Controller('system')
export class RealtimeFinalController {
  constructor(private readonly service: RealtimeFinalService) {}

  @Get('final-status')
  status() {
    return { success: true, data: this.service.finalStatus() };
  }

  @Post('realtime/event')
  event(
    @Headers('authorization') auth: string | undefined,
    @Body() body: {
      topic: string;
      eventType: string;
      sessionId?: string | null;
      payload?: Record<string, unknown>;
    },
  ) {
    return this.service.publishUserEvent(bearerToken(auth), body);
  }
}
