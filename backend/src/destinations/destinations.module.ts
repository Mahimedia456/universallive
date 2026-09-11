import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { SupabaseModule } from '../supabase/supabase.module';
import { DestinationsController } from './destinations.controller';

@Module({ imports: [SupabaseModule, AuthModule], controllers: [DestinationsController] })
export class DestinationsModule {}
