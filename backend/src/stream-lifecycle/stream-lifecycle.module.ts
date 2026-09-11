import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { RtmpCredentialsModule } from '../rtmp-credentials/rtmp-credentials.module';
import { NotificationsV2Module } from '../notifications-v2/notifications-v2.module';
import { StreamLifecycleController } from './stream-lifecycle.controller';
import { StreamLifecycleService } from './stream-lifecycle.service';

@Module({
  imports: [ConfigModule, RtmpCredentialsModule, NotificationsV2Module],
  controllers: [StreamLifecycleController],
  providers: [StreamLifecycleService, BackendSupabase],
  exports: [StreamLifecycleService],
})
export class StreamLifecycleModule {}
