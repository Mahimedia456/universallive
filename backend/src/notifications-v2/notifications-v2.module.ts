import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { PushModule } from '../push/push.module';
import { NotificationsV2Controller } from './notifications-v2.controller';
import { NotificationsV2Service } from './notifications-v2.service';

@Module({
  imports: [ConfigModule, PushModule],
  controllers: [NotificationsV2Controller],
  providers: [NotificationsV2Service, BackendSupabase],
  exports: [NotificationsV2Service],
})
export class NotificationsV2Module {}
