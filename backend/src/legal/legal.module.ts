import { Module } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { LegalController } from './legal.controller';
import { LegalService } from './legal.service';

@Module({ controllers: [LegalController], providers: [BackendSupabase, LegalService] })
export class LegalModule {}
