import { Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { SystemStateController } from './system-state.controller';
import { SystemStateService } from './system-state.service';

@Module({ controllers: [SystemStateController], providers: [BackendSupabase, SystemStateService] })
export class SystemStateModule {}
