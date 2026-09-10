import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { SupportV2Controller } from './support-v2.controller';
import { SupportV2Service } from './support-v2.service';

@Module({
  imports: [ConfigModule],
  controllers: [SupportV2Controller],
  providers: [SupportV2Service, BackendSupabase],
})
export class SupportV2Module {}
