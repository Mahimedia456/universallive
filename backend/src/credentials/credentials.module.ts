import { Module } from '@nestjs/common';
import { SupabaseModule } from '../supabase/supabase.module';
import { CredentialVaultService } from './credential-vault.service';
import { CredentialsController } from './credentials.controller';

@Module({
  imports: [SupabaseModule],
  controllers: [CredentialsController],
  providers: [CredentialVaultService],
  exports: [CredentialVaultService],
})
export class CredentialsModule {}
