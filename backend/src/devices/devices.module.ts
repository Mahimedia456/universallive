import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { SupabaseRestClient } from '../common/supabase-rest';
import { DevicesController } from './devices.controller';
import { DevicesService } from './devices.service';

@Module({
  imports: [ConfigModule],
  controllers: [DevicesController],
  providers: [DevicesService, SupabaseRestClient],
  exports: [DevicesService],
})
export class DevicesModule {}
