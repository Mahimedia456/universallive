import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { CredentialCryptoService } from './credential-crypto.service';
import { RtmpCredentialsController } from './rtmp-credentials.controller';
import { RtmpCredentialsService } from './rtmp-credentials.service';

@Module({
  imports: [ConfigModule],
  controllers: [RtmpCredentialsController],
  providers: [
    BackendSupabase,
    CredentialCryptoService,
    RtmpCredentialsService,
  ],
  exports: [RtmpCredentialsService],
})
export class RtmpCredentialsModule {}
