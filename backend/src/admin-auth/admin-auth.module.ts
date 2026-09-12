import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { SupabaseModule } from '../supabase/supabase.module';
import { AdminAuthController } from './admin-auth.controller';
import { AdminAuthService } from './admin-auth.service';
import { AdminJwtGuard } from './admin-jwt.guard';
@Module({imports:[SupabaseModule,JwtModule.register({})],controllers:[AdminAuthController],providers:[AdminAuthService,AdminJwtGuard],exports:[AdminAuthService,AdminJwtGuard]})
export class AdminAuthModule {}
