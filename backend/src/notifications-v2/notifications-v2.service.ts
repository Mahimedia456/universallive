import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { BackendSupabase } from '../common/backend-supabase';
import { PushService } from '../push/push.service';

@Injectable()
export class NotificationsV2Service {
  constructor(
    private readonly db: BackendSupabase,
    private readonly push: PushService,
    private readonly config: ConfigService,
  ) {}

  async list(token: string, unreadOnly = false) {
    const user = await this.db.currentUser(token);
    const unread = unreadOnly ? '&read_at=is.null' : '';
    const rows = await this.db.adminRest<any[]>(
      `ul_notifications?user_id=eq.${encodeURIComponent(user.id)}${unread}&select=*&order=created_at.desc&limit=100`,
      { method: 'GET' },
    );
    return (rows || []).map((row) => ({ ...row, is_read: Boolean(row.read_at) }));
  }

  async unreadCount(token: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_notifications?user_id=eq.${encodeURIComponent(user.id)}&read_at=is.null&select=id&limit=500`,
      { method: 'GET' },
    );
    return { unread: rows?.length || 0 };
  }

  async read(token: string, id: string) {
    const user = await this.db.currentUser(token);
    const rows = await this.db.adminRest<any[]>(
      `ul_notifications?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      { method: 'PATCH', body: JSON.stringify({ read_at: new Date().toISOString() }) },
    );
    if (!rows?.length) throw new NotFoundException('Notification not found');
    return { ...rows[0], is_read: true };
  }

  async readAll(token: string) {
    const user = await this.db.currentUser(token);
    await this.db.adminRest(
      `ul_notifications?user_id=eq.${encodeURIComponent(user.id)}&read_at=is.null`,
      { method: 'PATCH', body: JSON.stringify({ read_at: new Date().toISOString() }) },
    );
    return { updated: true };
  }

  async pushStatus(token: string) {
    await this.db.currentUser(token);
    return this.push.status();
  }

  async testPush(token: string) {
    const user = await this.db.currentUser(token);
    if (String(this.config.get('NODE_ENV') || 'development') === 'production') {
      throw new ForbiddenException('Test push is disabled in production');
    }

    const rows = await this.db.adminRest<any[]>('ul_notifications', {
      method: 'POST',
      body: JSON.stringify({
        user_id: user.id,
        type: 'system_test',
        title: 'Universal Live push is working',
        body: 'This Android notification was sent by your Universal Live backend.',
        severity: 'info',
        action_type: 'notifications',
        action_payload: { route: 'notifications' },
      }),
    });
    const notification = rows?.[0];
    const delivery = await this.push.sendToUser(user.id, {
      title: notification?.title || 'Universal Live push is working',
      body: notification?.body || 'Universal Live Android push test.',
      notificationId: notification?.id || null,
      data: { route: 'notifications', type: 'system_test' },
    });
    return { notification, delivery };
  }

  async createAndDispatch(
    userId: string,
    input: {
      type: string;
      title: string;
      body: string;
      severity?: string;
      actionType?: string | null;
      actionPayload?: Record<string, unknown>;
      pushData?: Record<string, string | number | boolean | null | undefined>;
    },
  ) {
    const rows = await this.db.adminRest<any[]>('ul_notifications', {
      method: 'POST',
      body: JSON.stringify({
        user_id: userId,
        type: input.type,
        title: input.title,
        body: input.body,
        severity: input.severity || 'info',
        action_type: input.actionType || null,
        action_payload: input.actionPayload || {},
      }),
    });
    const notification = rows?.[0] || null;
    const delivery = await this.push.sendToUser(userId, {
      title: input.title,
      body: input.body,
      notificationId: notification?.id || null,
      data: input.pushData || {},
    });
    return { notification, delivery };
  }
}
