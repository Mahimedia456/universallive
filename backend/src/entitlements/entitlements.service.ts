import { Injectable } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class EntitlementsService {
  constructor(private readonly db: BackendSupabase) {}

  async plans() {
    return this.db.adminRest<any[]>(
      'ul_plans?is_active=eq.true&select=*&order=sort_order.asc',
      { method: 'GET' },
    );
  }

  async mine(token: string) {
    const user = await this.db.currentUser(token);

    const rows = await this.db.adminRest<any[]>(
      `ul_user_entitlements?user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    let entitlement = rows?.[0];

    if (!entitlement) {
      const created = await this.db.adminRest<any[]>(
        'ul_user_entitlements',
        {
          method: 'POST',
          body: JSON.stringify({
            user_id: user.id,
            plan_key: 'free',
            status: 'active',
            source: 'system',
          }),
        },
      );
      entitlement = created?.[0];
    }

    const plans = await this.db.adminRest<any[]>(
      `ul_plans?plan_key=eq.${encodeURIComponent(entitlement.plan_key)}&select=*`,
      { method: 'GET' },
    );

    const plan = plans?.[0] || null;

    return {
      ...entitlement,
      plan,
      effective_entitlements: {
        ...(plan?.entitlements || {}),
        ...(entitlement?.entitlements_override || {}),
      },
    };
  }
}
