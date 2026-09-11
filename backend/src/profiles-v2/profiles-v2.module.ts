import { Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { ProfilesV2Controller } from './profiles-v2.controller';
import { ProfilesV2Service } from './profiles-v2.service';

@Module({
  controllers: [ProfilesV2Controller],
  providers: [ProfilesV2Service, BackendSupabase],
  exports: [ProfilesV2Service],
})
export class ProfilesV2Module {}
