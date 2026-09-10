import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { AssetsV2Controller } from './assets-v2.controller';
import { AssetsV2Service } from './assets-v2.service';

@Module({
  imports: [ConfigModule],
  controllers: [AssetsV2Controller],
  providers: [AssetsV2Service, BackendSupabase],
})
export class AssetsV2Module {}
