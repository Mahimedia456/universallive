import { BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { BackendSupabase } from '../common/backend-supabase';

@Injectable()
export class SupportV2Service {
  constructor(private readonly db: BackendSupabase) {}

  async list(token: string) {
    const user = await this.db.currentUser(token);
    return this.db.adminRest<any[]>(
      `ul_support_tickets?user_id=eq.${encodeURIComponent(user.id)}&select=*&order=created_at.desc`,
      { method: 'GET' },
    );
  }

  async create(token: string, body: any) {
    const user = await this.db.currentUser(token);

    if (!body.category) throw new BadRequestException('category is required');
    if (!body.subject?.trim()) throw new BadRequestException('subject is required');
    if (!body.description?.trim()) throw new BadRequestException('description is required');

    const rows = await this.db.adminRest<any[]>(
      'ul_support_tickets',
      {
        method: 'POST',
        body: JSON.stringify({
          user_id: user.id,
          category: body.category,
          subject: body.subject.trim(),
          description: body.description.trim(),
          diagnostics: body.diagnostics || {},
        }),
      },
    );

    return rows?.[0];
  }

  async detail(token: string, ticketId: string) {
    const user = await this.db.currentUser(token);

    const tickets = await this.db.adminRest<any[]>(
      `ul_support_tickets?id=eq.${encodeURIComponent(ticketId)}&user_id=eq.${encodeURIComponent(user.id)}&select=*`,
      { method: 'GET' },
    );

    if (!tickets?.length) throw new NotFoundException('Support ticket not found');

    const messages = await this.db.adminRest<any[]>(
      `ul_support_messages?ticket_id=eq.${encodeURIComponent(ticketId)}&select=*&order=created_at.asc`,
      { method: 'GET' },
    );

    return { ticket: tickets[0], messages };
  }

  async reply(token: string, ticketId: string, message: string) {
    const user = await this.db.currentUser(token);

    const tickets = await this.db.adminRest<any[]>(
      `ul_support_tickets?id=eq.${encodeURIComponent(ticketId)}&user_id=eq.${encodeURIComponent(user.id)}&select=id`,
      { method: 'GET' },
    );

    if (!tickets?.length) throw new NotFoundException('Support ticket not found');
    if (!message?.trim()) throw new BadRequestException('message is required');

    const rows = await this.db.adminRest<any[]>(
      'ul_support_messages',
      {
        method: 'POST',
        body: JSON.stringify({
          ticket_id: ticketId,
          user_id: user.id,
          sender_type: 'user',
          message: message.trim(),
        }),
      },
    );

    return rows?.[0];
  }
}
