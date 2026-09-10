import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { SupabaseRestClient } from '../common/supabase-rest';
import { ProfilesV2Controller } from './profiles-v2.controller';
import { ProfilesV2Service } from './profiles-v2.service';

@Module({
  imports: [ConfigModule],
  controllers: [ProfilesV2Controller],
  providers: [ProfilesV2Service, SupabaseRestClient],
  exports: [ProfilesV2Service],
})
export class ProfilesV2Module {}
