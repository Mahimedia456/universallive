import { Module } from '@nestjs/common';
import { SupabaseModule } from '../supabase/supabase.module';
import { AccountController } from './account.controller';
@Module({ imports: [SupabaseModule], controllers: [AccountController] })
export class AccountModule {}
