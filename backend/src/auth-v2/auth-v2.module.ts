import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';

import { BackendSupabase } from '../common/backend-supabase';
import { AuthV2Controller } from './auth-v2.controller';
import { AuthV2Service } from './auth-v2.service';

@Module({
  imports: [ConfigModule],
  controllers: [AuthV2Controller],
  providers: [AuthV2Service, BackendSupabase],
  exports: [AuthV2Service],
})
export class AuthV2Module {}
