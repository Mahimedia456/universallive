import { Controller, Get } from '@nestjs/common';
import { SystemStateService } from './system-state.service';

@Controller('system')
export class SystemStateController {
  constructor(private readonly service: SystemStateService) {}

  @Get('state')
  state() {
    return this.service.state();
  }
}
