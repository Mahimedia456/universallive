import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { StreamTelemetryController } from './stream-telemetry.controller';
import { StreamTelemetryService } from './stream-telemetry.service';

@Module({
  imports: [ConfigModule],
  controllers: [StreamTelemetryController],
  providers: [StreamTelemetryService, BackendSupabase],
  exports: [StreamTelemetryService],
})
export class StreamTelemetryModule {}
