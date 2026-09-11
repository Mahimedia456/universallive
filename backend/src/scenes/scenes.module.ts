import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { SupabaseModule } from '../supabase/supabase.module';
import { ScenesController } from './scenes.controller';

@Module({ imports: [SupabaseModule, AuthModule], controllers: [ScenesController] })
export class ScenesModule {}
