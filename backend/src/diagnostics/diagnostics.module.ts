import { Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { DiagnosticsController } from './diagnostics.controller';
import { DiagnosticsService } from './diagnostics.service';

@Module({
  controllers: [DiagnosticsController],
  providers: [BackendSupabase, DiagnosticsService],
  exports: [DiagnosticsService],
})
export class DiagnosticsModule {}
