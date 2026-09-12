import { Module } from '@nestjs/common';
import { AdminAuthModule } from '../admin-auth/admin-auth.module';
import { SupabaseModule } from '../supabase/supabase.module';
import { AdminConsoleController } from './admin-console.controller';
import { AdminConsoleService } from './admin-console.service';
@Module({ imports:[AdminAuthModule,SupabaseModule], controllers:[AdminConsoleController], providers:[AdminConsoleService] })
export class AdminConsoleModule {}
