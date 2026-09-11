import { Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class QaFinalService {
  constructor(private readonly db: BackendSupabase) {}

  private async exists(path: string): Promise<boolean> {
    const rows = await this.db.adminRest<any[]>(path, { method: 'GET' }).catch(() => []);
    return Array.isArray(rows) && rows.length > 0;
  }

  async run(token: string) {
    const user = await this.db.currentUser(token);
    const checks = await Promise.all([
      this.exists(`ul_creator_profiles?user_id=eq.${encodeURIComponent(user.id)}&select=user_id&limit=1`),
      this.exists(`ul_user_entitlements?user_id=eq.${encodeURIComponent(user.id)}&select=user_id&limit=1`),
      this.exists(`ul_scenes?user_id=eq.${encodeURIComponent(user.id)}&select=id&limit=1`),
      this.exists(`ul_streaming_connections?user_id=eq.${encodeURIComponent(user.id)}&select=id&limit=1`),
      this.exists(`ul_devices?user_id=eq.${encodeURIComponent(user.id)}&select=id&limit=1`),
    ]);

    const named = [
      { key: 'profile', pass: checks[0], required: true },
      { key: 'entitlement', pass: checks[1], required: true },
      { key: 'scene', pass: checks[2], required: true },
      { key: 'destination', pass: checks[3], required: false },
      { key: 'device', pass: checks[4], required: false },
    ];
    const requiredPassed = named.filter((c) => c.required).every((c) => c.pass);
    const status = requiredPassed ? 'pass' : 'review';

    const rows = await this.db.adminRest<any[]>('ul_qa_runs', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        status,
        checks: named,
        mobile_contract_version: '2026.09-final',
        backend_phase: '34-39-final',
      }),
    });

    return {
      id: rows?.[0]?.id || null,
      status,
      checks: named,
      requiredPassed,
      generatedAt: rows?.[0]?.created_at || new Date().toISOString(),
    };
  }

  async latest(token: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_qa_runs?user_id=eq.${encodeURIComponent(user.id)}&select=id,status,checks,mobile_contract_version,backend_phase,created_at&order=created_at.desc&limit=1`,
      { method: 'GET' },
    );
    return rows?.[0] || null;
  }
}
