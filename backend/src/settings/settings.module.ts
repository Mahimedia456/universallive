import { Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { SettingsController } from './settings.controller';
import { SettingsService } from './settings.service';

@Module({
  controllers: [SettingsController],
  providers: [SettingsService, BackendSupabase],
  exports: [SettingsService],
})
export class SettingsModule {}
