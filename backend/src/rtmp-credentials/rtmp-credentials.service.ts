import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';
import { CredentialCryptoService } from './credential-crypto.service';

@Injectable()
export class RtmpCredentialsService {
  constructor(
    private readonly db: BackendSupabase,
    private readonly crypto: CredentialCryptoService,
  ) {}

  async save(token: string, body: {
    connectionId: string;
    serverUrl: string;
    streamKey: string;
  }) {
    const user = await this.db.currentUser(token);

    if (!/^rtmps?:\/\//i.test(body.serverUrl || '')) {
      throw new BadRequestException('serverUrl must use rtmp:// or rtmps://');
    }
    if (!body.streamKey?.trim()) {
      throw new BadRequestException('streamKey is required');
    }

    const connection = await this.db.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(body.connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id,platform`,
      { method: 'GET' },
    );

    if (!connection?.length) throw new NotFoundException('Connection not found');

    await this.db.adminRest(
      'ul_stream_credentials?on_conflict=connection_id,credential_type',
      {
        method: 'POST',
        headers: {
          Prefer: 'resolution=merge-duplicates,return=representation',
        },
        body: JSON.stringify({
          user_id: user.id,
          connection_id: body.connectionId,
          credential_type: 'rtmp',
          server_url_ciphertext: this.crypto.encrypt(body.serverUrl.trim()),
          stream_key_ciphertext: this.crypto.encrypt(body.streamKey.trim()),
          last_rotated_at: new Date().toISOString(),
          updated_at: new Date().toISOString(),
        }),
      },
    );

    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(body.connectionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'connected',
          updated_at: new Date().toISOString(),
        }),
      },
    );

    return {
      connectionId: body.connectionId,
      saved: true,
      serverUrlMasked: maskServerUrl(body.serverUrl),
      streamKeyMasked: maskSecret(body.streamKey),
    };
  }

  async status(token: string, connectionId: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id,last_rotated_at,key_version`,
      { method: 'GET' },
    );

    return {
      connectionId,
      configured: !!rows?.length,
      lastRotatedAt: rows?.[0]?.last_rotated_at || null,
      keyVersion: rows?.[0]?.key_version || null,
    };
  }
}

function maskSecret(value: string): string {
  if (!value) return '';
  if (value.length <= 6) return '••••••';
  return `${value.slice(0, 2)}••••••${value.slice(-2)}`;
}

function maskServerUrl(value: string): string {
  try {
    const url = new URL(value.replace(/^rtmp:/i, 'http:').replace(/^rtmps:/i, 'https:'));
    return `${url.hostname}/••••`;
  } catch {
    return '••••';
  }
}
