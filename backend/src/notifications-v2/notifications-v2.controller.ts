import { Controller, Get, Headers, Param, Patch, Post, Query } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { NotificationsV2Service } from './notifications-v2.service';

@Controller('notifications')
export class NotificationsV2Controller {
  constructor(private readonly service: NotificationsV2Service) {}

  @Get()
  list(@Headers('authorization') auth: string | undefined, @Query('unreadOnly') unreadOnly?: string) {
    return this.service.list(bearerToken(auth), unreadOnly === 'true');
  }

  @Get('unread-count')
  unread(@Headers('authorization') auth?: string) {
    return this.service.unreadCount(bearerToken(auth));
  }

  @Get('push/status')
  pushStatus(@Headers('authorization') auth?: string) {
    return this.service.pushStatus(bearerToken(auth));
  }

  @Post('push/test')
  testPush(@Headers('authorization') auth?: string) {
    return this.service.testPush(bearerToken(auth));
  }

  @Patch(':id/read')
  read(@Headers('authorization') auth: string | undefined, @Param('id') id: string) {
    return this.service.read(bearerToken(auth), id);
  }

  @Post('read-all')
  readAll(@Headers('authorization') auth?: string) {
    return this.service.readAll(bearerToken(auth));
  }
}
