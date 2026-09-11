import { Module } from '@nestjs/common';
import { HomeDashboardController } from './home-dashboard.controller';
import { HomeDashboardService } from './home-dashboard.service';

@Module({
  controllers: [HomeDashboardController],
  providers: [HomeDashboardService],
})
export class HomeDashboardModule {}
