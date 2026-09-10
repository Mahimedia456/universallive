import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { BroadcastSessionsController } from './broadcast-sessions.controller';
import { BroadcastSessionsService } from './broadcast-sessions.service';

@Module({
  imports: [ConfigModule],
  controllers: [BroadcastSessionsController],
  providers: [BroadcastSessionsService, BackendSupabase],
  exports: [BroadcastSessionsService],
})
export class BroadcastSessionsModule {}
