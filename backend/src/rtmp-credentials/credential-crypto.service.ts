import { Injectable, InternalServerErrorException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createCipheriv, createDecipheriv, createHash, randomBytes } from 'crypto';

@Injectable()
export class CredentialCryptoService {
  constructor(private readonly config: ConfigService) {}

  private key(): Buffer {
    const secret = this.config.get<string>('STREAM_SECRET_MASTER_KEY');
    if (!secret) {
      throw new InternalServerErrorException(
        'STREAM_SECRET_MASTER_KEY is required for streaming credentials',
      );
    }
    return createHash('sha256').update(secret).digest();
  }

  encrypt(value: string): string {
    const iv = randomBytes(12);
    const cipher = createCipheriv('aes-256-gcm', this.key(), iv);
    const encrypted = Buffer.concat([
      cipher.update(value, 'utf8'),
      cipher.final(),
    ]);
    const tag = cipher.getAuthTag();

    return [
      'v1',
      iv.toString('base64url'),
      tag.toString('base64url'),
      encrypted.toString('base64url'),
    ].join('.');
  }

  decrypt(payload: string): string {
    const [version, ivB64, tagB64, dataB64] = payload.split('.');
    if (version !== 'v1' || !ivB64 || !tagB64 || !dataB64) {
      throw new Error('Invalid encrypted credential payload');
    }

    const decipher = createDecipheriv(
      'aes-256-gcm',
      this.key(),
      Buffer.from(ivB64, 'base64url'),
    );

    decipher.setAuthTag(Buffer.from(tagB64, 'base64url'));

    return Buffer.concat([
      decipher.update(Buffer.from(dataB64, 'base64url')),
      decipher.final(),
    ]).toString('utf8');
  }
}
