import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { ScenesV2Controller } from './scenes-v2.controller';
import { ScenesV2Service } from './scenes-v2.service';

@Module({
  imports: [ConfigModule],
  controllers: [ScenesV2Controller],
  providers: [ScenesV2Service, BackendSupabase],
  exports: [ScenesV2Service],
})
export class ScenesV2Module {}
