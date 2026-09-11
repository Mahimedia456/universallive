import { Global, Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { SupabaseService } from './supabase.service';

@Global()
@Module({
  providers: [SupabaseService, BackendSupabase],
  exports: [SupabaseService, BackendSupabase],
})
export class SupabaseModule {}
