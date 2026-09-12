import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { CredentialCryptoService } from '../rtmp-credentials/credential-crypto.service';
import { PlatformOauthController } from './platform-oauth.controller';
import { PlatformOauthService } from './platform-oauth.service';

@Module({
  imports: [ConfigModule],
  controllers: [PlatformOauthController],
  providers: [PlatformOauthService, BackendSupabase, CredentialCryptoService],
})
export class PlatformOauthModule {}
