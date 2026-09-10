import { Injectable } from '@nestjs/common';
import { createHash } from 'crypto';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class SecurityAuditService {
  constructor(private readonly db: BackendSupabase) {}

  async record(input: {
    userId?: string | null;
    eventType: string;
    severity?: string;
    requestId?: string | null;
    route?: string | null;
    ip?: string | null;
    userAgent?: string | null;
    metadata?: Record<string, unknown>;
  }) {
    const hash = (value?: string | null) =>
      value ? createHash('sha256').update(value).digest('hex') : null;

    const rows = await this.db.adminRest<any[]>(
      'ul_security_events',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: input.userId || null,
          event_type: input.eventType,
          severity: input.severity || 'info',
          request_id: input.requestId || null,
          route: input.route || null,
          ip_hash: hash(input.ip),
          user_agent_hash: hash(input.userAgent),
          metadata: input.metadata || {},
        }),
      },
    );

    return rows?.[0] || null;
  }

  summary() {
    return {
      credentialStorage: 'encrypted-backend-vault',
      mobileSecrets: 'forbidden',
      serviceRoleExposure: 'backend-only',
      rls: 'enabled',
      auditLog: 'enabled',
      rateLimitStore: 'prepared',
    };
  }
}
