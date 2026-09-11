import { Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { AuthController } from './auth.controller';
import { UniversalLiveAuthGuard } from './universallive-auth.guard';

@Module({
  controllers: [AuthController],
  providers: [UniversalLiveAuthGuard, BackendSupabase],
  exports: [UniversalLiveAuthGuard],
})
export class AuthModule {}
