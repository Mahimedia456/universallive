import { Module } from '@nestjs/common'; import { SupabaseModule } from '../supabase/supabase.module'; import { DestinationsController } from './destinations.controller';
@Module({ imports:[SupabaseModule], controllers:[DestinationsController] }) export class DestinationsModule {}
