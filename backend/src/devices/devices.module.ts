import { Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { DevicesController } from './devices.controller';
import { DevicesService } from './devices.service';

@Module({
  controllers: [DevicesController],
  providers: [DevicesService, BackendSupabase],
  exports: [DevicesService],
})
export class DevicesModule {}
