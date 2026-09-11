import {
  Controller,
  Get,
  Headers,
} from '@nestjs/common';

import { BackendSupabase } from './common/backend-supabase';

@Controller()
export class AppController {
  constructor(private readonly db: BackendSupabase) {}

  @Get()
  getApiRoot() {
    return {
      ok: true,
      service: 'UniversalLive Backend',
      name: 'Universal Live API',
      status: 'online',
      apiVersion: 'v1',
      mobileContractVersion: '2026.09-final',
      authProvider: 'universallive-db',
      baseUrl: '/api/v1',
      endpoints: {
        bootstrap: '/api/v1/app/bootstrap',
        authStatus: '/api/v1/auth/mobile/status',
        health: '/api/v1/health',
        foundation: '/api/v1/foundation',
        plans: '/api/v1/billing/plans',
        diagnostics: '/api/v1/diagnostics/summary',
        legal: '/api/v1/legal/about',
        systemState: '/api/v1/system/state',
        qa: '/api/v1/qa/smoke',
      },
    };
  }

  @Get('app/bootstrap')
  async bootstrap(@Headers('authorization') authorization?: string) {
    const flags = await this.db.adminRest<any[]>(
      'ul_system_flags?is_public=eq.true&select=key,value,updated_at',
      { method: 'GET' },
    ).catch(() => []);

    const byKey = Object.fromEntries(
      (flags || []).map((item) => [item.key, item.value]),
    );

    const response: Record<string, unknown> = {
      ok: true,
      api_version: 'v1',
      mobile_contract_version: '2026.09-final',
      auth_provider: 'universallive-db',
      maintenance: byKey.maintenance || { enabled: false },
      minimum_version: byKey.minimum_version || null,
      backend_contract: byKey.backend_contract || null,
      feature_flags: byKey.feature_flags || {},
      navigation: byKey.navigation || {},
      session: null,
      active_live: null,
    };

    const token = (authorization || '').replace(/^Bearer\s+/i, '').trim();
    if (!token) return response;

    try {
      const user = await this.db.currentUser(token);
      const active = await this.db.adminRest<any[]>(
        `ul_broadcast_sessions?user_id=eq.${encodeURIComponent(user.id)}&status=in.(live,reconnecting)&select=id,title,status,started_at,created_at&order=created_at.desc&limit=1`,
        { method: 'GET' },
      ).catch(() => []);

      response.session = {
        valid: true,
        user: {
          id: user.id,
          email: user.email,
          email_verified_at: user.email_confirmed_at,
        },
      };
      response.active_live = active?.[0] || null;
    } catch {
      response.session = { valid: false };
    }

    return response;
  }
}
