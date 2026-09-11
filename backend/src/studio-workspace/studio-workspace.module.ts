import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { StudioWorkspaceController } from './studio-workspace.controller';
import { StudioWorkspaceService } from './studio-workspace.service';

@Module({
  imports: [ConfigModule],
  controllers: [StudioWorkspaceController],
  providers: [StudioWorkspaceService, BackendSupabase],
  exports: [StudioWorkspaceService],
})
export class StudioWorkspaceModule {}
