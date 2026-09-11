import { Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { QaFinalController } from './qa-final.controller';
import { QaFinalService } from './qa-final.service';

@Module({ controllers: [QaFinalController], providers: [BackendSupabase, QaFinalService] })
export class QaFinalModule {}
