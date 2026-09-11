import { Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class SystemStateService {
  constructor(private readonly db: BackendSupabase) {}

  async state() {
    const rows = await this.db.adminRest<any[]>(
      'ul_system_flags?is_public=eq.true&select=key,value,description,updated_at&order=key.asc',
      { method: 'GET' },
    ).catch(() => []);
    const flags = Object.fromEntries((rows || []).map((row) => [row.key, row.value]));
    return {
      online: true,
      maintenance: flags.maintenance || { enabled: false },
      minimumVersion: flags.minimum_version || { android: '0.40.0', ios: '0.40.0', force: false },
      featureFlags: flags.feature_flags || {},
      navigation: flags.navigation || {},
      backendContract: flags.backend_contract || {
        api_version: 'v1',
        mobile_contract_version: '2026.09-final',
        phase: '34-39-final',
      },
      updatedAt: (rows || []).map((row) => row.updated_at).filter(Boolean).sort().pop() || null,
    };
  }
}
