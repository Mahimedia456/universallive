import { Controller, Get } from '@nestjs/common';

@Controller('health')
export class HealthController {
  @Get()
  health() {
    return { ok: true, service: 'UniversalLive Backend', version: '0.40.0', time: new Date().toISOString() };
  }
}
