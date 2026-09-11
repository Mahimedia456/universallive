import { Controller, Get, Headers, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { QaFinalService } from './qa-final.service';

@Controller('qa')
export class QaFinalController {
  constructor(private readonly service: QaFinalService) {}

  @Post('smoke')
  run(@Headers('authorization') auth?: string) {
    return this.service.run(bearerToken(auth));
  }

  @Get('latest')
  latest(@Headers('authorization') auth?: string) {
    return this.service.latest(bearerToken(auth));
  }
}
