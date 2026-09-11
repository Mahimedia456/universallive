import { Global, Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { PushService } from './push.service';

@Global()
@Module({
  imports: [ConfigModule],
  providers: [PushService, BackendSupabase],
  exports: [PushService],
})
export class PushModule {}
