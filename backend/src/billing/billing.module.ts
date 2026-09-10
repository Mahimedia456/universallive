import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { BillingController } from './billing.controller';
import { BillingService } from './billing.service';

@Module({
  imports: [ConfigModule],
  controllers: [BillingController],
  providers: [BillingService, BackendSupabase],
})
export class BillingModule {}
