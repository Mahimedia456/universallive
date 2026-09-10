import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { RealtimeFinalController } from './realtime-final.controller';
import { RealtimeFinalService } from './realtime-final.service';

@Module({
  imports: [ConfigModule],
  controllers: [RealtimeFinalController],
  providers: [RealtimeFinalService, BackendSupabase],
})
export class RealtimeFinalModule {}
