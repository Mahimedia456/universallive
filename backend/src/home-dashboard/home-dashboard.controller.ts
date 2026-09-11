import { Controller, Get, Headers } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { HomeDashboardService } from './home-dashboard.service';

@Controller('home')
export class HomeDashboardController {
  constructor(private readonly service: HomeDashboardService) {}

  @Get('dashboard')
  dashboard(@Headers('authorization') authorization?: string) {
    return this.service.dashboard(bearerToken(authorization));
  }
}
