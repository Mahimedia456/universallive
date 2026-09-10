import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { StreamingConnectionsController } from './streaming-connections.controller';
import { StreamingConnectionsService } from './streaming-connections.service';

@Module({
  imports: [ConfigModule],
  controllers: [StreamingConnectionsController],
  providers: [StreamingConnectionsService, BackendSupabase],
  exports: [StreamingConnectionsService],
})
export class StreamingConnectionsModule {}
