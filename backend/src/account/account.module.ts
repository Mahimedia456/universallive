import { Module } from '@nestjs/common';
import { AccountController } from './account.controller';
import { AccountService } from './account.service';
import { BackendSupabase } from '../common/backend-supabase';

@Module({
  controllers: [AccountController],
  providers: [AccountService, BackendSupabase],
})
export class AccountModule {}
