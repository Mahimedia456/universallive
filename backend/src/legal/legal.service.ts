import { BadRequestException, Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class LegalService {
  constructor(private readonly db: BackendSupabase) {}

  async about() {
    const docs = await this.db.adminRest<any[]>(
      'ul_legal_documents?is_active=eq.true&select=document_key,title,version,effective_at,required_acceptance,public_url&order=document_key.asc',
      { method: 'GET' },
    ).catch(() => []);

    return {
      appName: 'Universal Live',
      appVersion: '0.40.0',
      buildNumber: 39,
      apiVersion: 'v1',
      mobileContractVersion: '2026.09-final',
      documents: docs || [],
    };
  }

  async documents() {
    return this.db.adminRest<any[]>(
      'ul_legal_documents?is_active=eq.true&select=document_key,title,version,body,public_url,effective_at,required_acceptance&order=document_key.asc',
      { method: 'GET' },
    );
  }

  async accept(token: string, body: { documentKey?: string; version?: string }) {
    const user = await this.db.currentUser(token);
    const documentKey = String(body?.documentKey || '').trim();
    const version = String(body?.version || '').trim();
    if (!documentKey || !version) throw new BadRequestException('documentKey and version are required');

    const docs = await this.db.adminRest<any[]>(
      `ul_legal_documents?document_key=eq.${encodeURIComponent(documentKey)}&version=eq.${encodeURIComponent(version)}&is_active=eq.true&select=document_key,version`,
      { method: 'GET' },
    );
    if (!docs?.length) throw new BadRequestException('Legal document version is not active');

    const rows = await this.db.adminRest<any[]>('ul_legal_acceptances?on_conflict=user_id,document_key,version', {
      method: 'POST',
      headers: { Prefer: 'resolution=merge-duplicates,return=representation' },
      body: JSON.stringify({
        user_id: user.id,
        document_key: documentKey,
        version,
        source: 'mobile',
        accepted_at: new Date().toISOString(),
      }),
    });
    return rows?.[0] || { document_key: documentKey, version };
  }

  async acceptances(token: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_legal_acceptances?user_id=eq.${encodeURIComponent(user.id)}&select=document_key,version,accepted_at,source&order=accepted_at.desc`,
      { method: 'GET' },
    );
  }
}
