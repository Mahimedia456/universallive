import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { StreamConfigV2Controller } from './stream-config-v2.controller';
import { StreamConfigV2Service } from './stream-config-v2.service';

@Module({
  imports: [ConfigModule],
  controllers: [StreamConfigV2Controller],
  providers: [StreamConfigV2Service, BackendSupabase],
  exports: [StreamConfigV2Service],
})
export class StreamConfigV2Module {}
