import { Body, Controller, Get, Headers, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { DiagnosticsService } from './diagnostics.service';

@Controller('diagnostics')
export class DiagnosticsController {
  constructor(private readonly service: DiagnosticsService) {}

  @Get('summary')
  summary(@Headers('authorization') auth?: string) {
    return this.service.summary(bearerToken(auth));
  }

  @Post('snapshot')
  snapshot(@Headers('authorization') auth: string | undefined, @Body() body: any) {
    return this.service.createSnapshot(bearerToken(auth), body);
  }
}
