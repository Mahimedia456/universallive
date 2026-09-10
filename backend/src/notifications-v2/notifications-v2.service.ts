import { Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class NotificationsV2Service {
  constructor(private readonly db: BackendSupabase) {}

  async list(token: string, unreadOnly = false) {
    const user = await this.db.currentUser(token);
    const unread = unreadOnly ? '&read_at=is.null' : '';

    return this.db.adminRest<any[]>(
      `ul_notifications?user_id=eq.${encodeURIComponent(user.id)}${unread}&select=*&order=created_at.desc&limit=100`,
      { method: 'GET' },
    );
  }

  async read(token: string, id: string) {
    const user = await this.db.currentUser(token);

    const rows = await this.db.adminRest<any[]>(
      `ul_notifications?id=eq.${encodeURIComponent(id)}&user_id=eq.${encodeURIComponent(user.id)}`,
      {
        method: 'PATCH',
        body: JSON.stringify({ read_at: new Date().toISOString() }),
      },
    );

    if (!rows?.length) throw new NotFoundException('Notification not found');
    return rows[0];
  }

  async readAll(token: string) {
    const user = await this.db.currentUser(token);

    await this.db.adminRest(
      `ul_notifications?user_id=eq.${encodeURIComponent(user.id)}&read_at=is.null`,
      {
        method: 'PATCH',
        body: JSON.stringify({ read_at: new Date().toISOString() }),
      },
    );

    return { updated: true };
  }
}
