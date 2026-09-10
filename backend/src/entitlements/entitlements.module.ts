import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { EntitlementsController } from './entitlements.controller';
import { EntitlementsService } from './entitlements.service';

@Module({
  imports: [ConfigModule],
  controllers: [EntitlementsController],
  providers: [EntitlementsService, BackendSupabase],
  exports: [EntitlementsService],
})
export class EntitlementsModule {}
