import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { SecurityAuditController } from './security-audit.controller';
import { SecurityAuditService } from './security-audit.service';

@Module({
  imports: [ConfigModule],
  controllers: [SecurityAuditController],
  providers: [SecurityAuditService, BackendSupabase],
  exports: [SecurityAuditService],
})
export class SecurityAuditModule {}
