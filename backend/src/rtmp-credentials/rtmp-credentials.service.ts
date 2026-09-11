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
    const serverUrl = normalizeRtmpUrl(body.serverUrl);
    const streamKey = String(body.streamKey || '').trim();

    if (!streamKey) throw new BadRequestException('streamKey is required');
    if (streamKey.length > 2048) throw new BadRequestException('streamKey is too long');

    const connection = await this.assertOwnedConnection(user.id, body.connectionId);
    const existingRows = await this.db.adminRest<any[]>(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(body.connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id,key_version`,
      { method: 'GET' },
    );
    const nextVersion = Number(existingRows?.[0]?.key_version || 0) + 1;
    const now = new Date().toISOString();

    await this.db.adminRest(
      'ul_stream_credentials?on_conflict=connection_id,credential_type',
      {
        method: 'POST',
        headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
        body: JSON.stringify({
          user_id: user.id,
          connection_id: body.connectionId,
          credential_type: 'rtmp',
          server_url_ciphertext: this.crypto.encrypt(serverUrl),
          stream_key_ciphertext: this.crypto.encrypt(streamKey),
          key_version: nextVersion,
          last_rotated_at: now,
          updated_at: now,
        }),
      },
    );

    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(body.connectionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'connected',
          last_health_status: 'ready',
          last_health_checked_at: now,
          last_error_code: null,
          last_error_message: null,
          updated_at: now,
        }),
      },
    );

    await this.logEvent(user.id, body.connectionId, 'credential_rotated', 'ok', {
      platform: connection.platform,
      key_version: nextVersion,
      server_host: safeHost(serverUrl),
    });

    return {
      connectionId: body.connectionId,
      saved: true,
      keyVersion: nextVersion,
      serverUrlMasked: maskServerUrl(serverUrl),
      streamKeyMasked: maskSecret(streamKey),
      updatedAt: now,
    };
  }

  async status(token: string, connectionId: string) {
    const user = await this.db.currentUser(token);
    await this.assertOwnedConnection(user.id, connectionId);

    const rows = await this.db.adminRest<any[]>(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=id,last_rotated_at,key_version,token_expires_at`,
      { method: 'GET' },
    );

    return {
      connectionId,
      configured: !!rows?.length,
      lastRotatedAt: rows?.[0]?.last_rotated_at || null,
      keyVersion: rows?.[0]?.key_version || null,
      tokenExpiresAt: rows?.[0]?.token_expires_at || null,
    };
  }

  async clear(token: string, connectionId: string) {
    const user = await this.db.currentUser(token);
    await this.assertOwnedConnection(user.id, connectionId);

    await this.db.adminRest(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp`,
      { method: 'DELETE' },
    );

    const now = new Date().toISOString();
    await this.db.adminRest(
      `ul_streaming_connections?id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({
          status: 'disconnected',
          last_health_status: 'needs_setup',
          last_health_checked_at: now,
          last_error_code: 'CREDENTIAL_REMOVED',
          last_error_message: 'RTMP credentials are not configured',
          updated_at: now,
        }),
      },
    );

    await this.logEvent(user.id, connectionId, 'credential_removed', 'ok');
    return { connectionId, cleared: true };
  }

  /**
   * Returns the native publish contract to the authenticated owner. Secrets are
   * decrypted only at this boundary and are never returned by list/detail APIs.
   */
  async publishConfig(token: string, connectionId: string) {
    const user = await this.db.currentUser(token);
    const connection = await this.assertOwnedConnection(user.id, connectionId);

    if (connection.is_enabled === false) {
      throw new BadRequestException('Connection is disabled');
    }

    const rows = await this.db.adminRest<any[]>(
      `ul_stream_credentials?connection_id=eq.${encodeURIComponent(connectionId)}&user_id=eq.${encodeURIComponent(user.id)}&credential_type=eq.rtmp&select=server_url_ciphertext,stream_key_ciphertext,key_version,last_rotated_at`,
      { method: 'GET' },
    );

    const credential = rows?.[0];
    if (!credential?.server_url_ciphertext || !credential?.stream_key_ciphertext) {
      throw new BadRequestException('RTMP credentials are not configured for this connection');
    }

    const serverUrl = normalizeRtmpUrl(this.crypto.decrypt(credential.server_url_ciphertext));
    const streamKey = this.crypto.decrypt(credential.stream_key_ciphertext).trim();
    if (!streamKey) throw new BadRequestException('Stored RTMP stream key is invalid');

    return {
      connectionId,
      platform: connection.platform,
      displayName: connection.display_name,
      serverUrl,
      streamKey,
      credentialVersion: credential.key_version || 1,
      credentialUpdatedAt: credential.last_rotated_at || null,
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

  private async logEvent(
    userId: string,
    connectionId: string,
    eventType: string,
    status: string,
    metadata: Record<string, unknown> = {},
  ) {
    await this.db.adminRest('ul_connection_events', {
      method: 'POST',
      body: JSON.stringify({
        user_id: userId,
        connection_id: connectionId,
        event_type: eventType,
        status,
        metadata,
      }),
    });
  }
}

function normalizeRtmpUrl(raw: unknown): string {
  const value = String(raw || '').trim();
  if (!/^rtmps?:\/\//i.test(value)) {
    throw new BadRequestException('serverUrl must use rtmp:// or rtmps://');
  }

  const host = safeHost(value);
  if (!host) throw new BadRequestException('serverUrl must include a valid host');
  if (value.length > 2048) throw new BadRequestException('serverUrl is too long');
  return value;
}

function safeHost(value: string): string | null {
  try {
    const httpish = value
      .replace(/^rtmp:/i, 'http:')
      .replace(/^rtmps:/i, 'https:');
    return new URL(httpish).hostname || null;
  } catch {
    return null;
  }
}

function maskSecret(value: string): string {
  if (!value) return '';
  if (value.length <= 6) return '••••••';
  return `${value.slice(0, 2)}••••••${value.slice(-2)}`;
}

function maskServerUrl(value: string): string {
  const host = safeHost(value);
  return host ? `${host}/••••` : '••••';
}
