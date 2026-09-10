import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { SceneSourcesController } from './scene-sources.controller';
import { SceneSourcesService } from './scene-sources.service';

@Module({
  imports: [ConfigModule],
  controllers: [SceneSourcesController],
  providers: [SceneSourcesService, BackendSupabase],
})
export class SceneSourcesModule {}
