import { BadRequestException, Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { createCipheriv, createDecipheriv, createHash, randomBytes } from 'node:crypto';

export type EncryptedSecret = { ciphertext: string; iv: string; authTag: string; keyVersion: number };

@Injectable()
export class CredentialVaultService {
  private readonly key: Buffer;
  private readonly keyVersion = 1;

  constructor(config: ConfigService) {
    const raw = config.get<string>('STREAM_SECRET_MASTER_KEY');
    if (!raw || raw.length < 32) {
      throw new Error('STREAM_SECRET_MASTER_KEY must be configured and at least 32 characters long');
    }
    this.key = createHash('sha256').update(raw, 'utf8').digest();
  }

  encrypt(secret: string): EncryptedSecret {
    const value = secret?.trim();
    if (!value) throw new BadRequestException('Stream key/secret is required');
    const iv = randomBytes(12);
    const cipher = createCipheriv('aes-256-gcm', this.key, iv);
    const ciphertext = Buffer.concat([cipher.update(value, 'utf8'), cipher.final()]);
    const authTag = cipher.getAuthTag();
    return {
      ciphertext: ciphertext.toString('base64'),
      iv: iv.toString('base64'),
      authTag: authTag.toString('base64'),
      keyVersion: this.keyVersion,
    };
  }

  decrypt(row: { ciphertext: string; iv: string; auth_tag: string }): string {
    const decipher = createDecipheriv('aes-256-gcm', this.key, Buffer.from(row.iv, 'base64'));
    decipher.setAuthTag(Buffer.from(row.auth_tag, 'base64'));
    const plain = Buffer.concat([
      decipher.update(Buffer.from(row.ciphertext, 'base64')),
      decipher.final(),
    ]);
    return plain.toString('utf8');
  }
}
