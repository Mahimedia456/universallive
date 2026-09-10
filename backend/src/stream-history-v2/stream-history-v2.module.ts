import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { StreamHistoryV2Controller } from './stream-history-v2.controller';
import { StreamHistoryV2Service } from './stream-history-v2.service';

@Module({
  imports: [ConfigModule],
  controllers: [StreamHistoryV2Controller],
  providers: [StreamHistoryV2Service, BackendSupabase],
  exports: [StreamHistoryV2Service],
})
export class StreamHistoryV2Module {}
