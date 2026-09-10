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
      `ul_streaming_connections?id=eq.${encodeURIComponent(body.connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id,platform,is_enabled`,
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
          last_error_code: null,
          last_error_message: null,
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
    await this.assertOwnedConnection(user.id, connectionId);

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

  /**
   * Returns the short-lived native publish contract to the authenticated owner.
   * The plaintext secret is never persisted outside the encrypted vault; it is
   * decrypted only for the device that must establish the RTMP connection.
   */
  async publishConfig(token: string, connectionId: string) {
    const user = await this.db.currentUser(token);
    const connection = await this.assertOwnedConnection(user.id, connectionId);

    if (connection.is_enabled === false) {
      throw new BadRequestException('Connection is disabled');
    }

    const rows = await this.db.adminRest<any[]>(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=server_url_ciphertext,stream_key_ciphertext`,
      { method: 'GET' },
    );

    const credential = rows?.[0];
    if (!credential?.server_url_ciphertext || !credential?.stream_key_ciphertext) {
      throw new BadRequestException('RTMP credentials are not configured for this connection');
    }

    const serverUrl = this.crypto.decrypt(credential.server_url_ciphertext);
    const streamKey = this.crypto.decrypt(credential.stream_key_ciphertext);

    if (!/^rtmps?:\/\//i.test(serverUrl) || !streamKey.trim()) {
      throw new BadRequestException('Stored RTMP credentials are invalid');
    }

    return {
      connectionId,
      platform: connection.platform,
      displayName: connection.display_name,
      serverUrl,
      streamKey,
    };
  }

  private async assertOwnedConnection(userId: string, connectionId: string) {
    const rows = await this.db.adminRest<any[]>(
      `ul_streaming_connections?id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(userId)}&select=id,platform,display_name,status,is_enabled,is_default`,
      { method: 'GET' },
    );
    if (!rows?.length) throw new NotFoundException('Connection not found');
    return rows[0];
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
