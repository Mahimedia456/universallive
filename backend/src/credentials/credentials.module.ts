import { Module } from '@nestjs/common';
import { AuthModule } from '../auth/auth.module';
import { SupabaseModule } from '../supabase/supabase.module';
import { CredentialVaultService } from './credential-vault.service';
import { CredentialsController } from './credentials.controller';

@Module({
  imports: [SupabaseModule, AuthModule],
  controllers: [CredentialsController],
  providers: [CredentialVaultService],
  exports: [CredentialVaultService],
})
export class CredentialsModule {}
