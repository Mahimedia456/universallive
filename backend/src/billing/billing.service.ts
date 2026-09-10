import { BadRequestException, Injectable } from '@nestjs/common';
import { createHash } from 'crypto';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class BillingService {
  constructor(private readonly db: BackendSupabase) {}

  async submit(token: string, body: any) {
    const user = await this.db.currentUser(token);

    const store = String(body.store || '').toLowerCase();
    if (!['apple', 'google'].includes(store)) {
      throw new BadRequestException('store must be apple or google');
    }
    if (!body.productId) throw new BadRequestException('productId is required');

    const purchaseTokenHash = body.purchaseToken
      ? createHash('sha256').update(String(body.purchaseToken)).digest('hex')
      : null;

    const rows = await this.db.adminRest<any[]>(
      'ul_store_purchases',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          store,
          product_id: body.productId,
          transaction_id: body.transactionId || null,
          original_transaction_id: body.originalTransactionId || null,
          purchase_token_hash: purchaseTokenHash,
          status: 'pending',
          plan_key: body.planKey || null,
          purchased_at: body.purchasedAt || null,
          verification_payload: {},
        }),
      },
    );

    return {
      purchase: rows?.[0] || null,
      verificationStatus: 'pending',
      message:
        'Store server verification endpoint is prepared. Provider credential/API verification is completed when App Store / Play Console production credentials are configured.',
    };
  }

  async listMine(token: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_store_purchases?user_id=eq.${encodeURIComponent(user.id)}&select=id,store,product_id,transaction_id,status,plan_key,purchased_at,expires_at,auto_renew,last_verified_at,created_at&order=created_at.desc`,
      { method: 'GET' },
    );
  }

  async restore(token: string) {
    const purchases = await this.listMine(token);
    return {
      restored: purchases.filter((p: any) => ['verified', 'active'].includes(String(p.status))),
    };
  }
}
