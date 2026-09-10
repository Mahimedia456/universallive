import { Controller, Get } from '@nestjs/common';
import { SecurityAuditService } from './security-audit.service';

@Controller('security')
export class SecurityAuditController {
  constructor(private readonly service: SecurityAuditService) {}

  @Get('status')
  status() {
    return { success: true, data: this.service.summary() };
  }
}
