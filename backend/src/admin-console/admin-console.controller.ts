import {
  Body,
  Controller,
  Get,
  Headers,
  Param,
  Patch,
  Post,
} from '@nestjs/common';

import { AdminConsoleService } from './admin-console.service';

@Controller('admin-console')
export class AdminConsoleController {
  constructor(private readonly admin: AdminConsoleService) {}

  private token(value?: string) {
    return (value || '').replace(/^Bearer\s+/i, '').trim();
  }

  @Get('me')
  me(@Headers('authorization') authorization?: string) {
    return this.admin.me(this.token(authorization));
  }

  @Get('overview')
  overview(@Headers('authorization') authorization?: string) {
    return this.admin.overview(this.token(authorization));
  }

  @Get('recent-users')
  recentUsers(@Headers('authorization') authorization?: string) {
    return this.admin.recentUsers(this.token(authorization));
  }

  @Get('recent-broadcasts')
  recentBroadcasts(@Headers('authorization') authorization?: string) {
    return this.admin.recentBroadcasts(this.token(authorization));
  }

  @Get('open-support')
  openSupport(@Headers('authorization') authorization?: string) {
    return this.admin.openSupport(this.token(authorization));
  }

  @Get('creators')
  creators(@Headers('authorization') authorization?: string) {
    return this.admin.creators(this.token(authorization));
  }

  @Patch('creators/:userId/membership')
  membership(
    @Headers('authorization') authorization: string | undefined,
    @Param('userId') userId: string,
    @Body()
    body: {
      planKey: 'free' | 'creator' | 'pro';
      status?: string;
    },
  ) {
    return this.admin.updateMembership(
      this.token(authorization),
      userId,
      body,
    );
  }

  @Get('connections')
  connections(@Headers('authorization') authorization?: string) {
    return this.admin.connections(this.token(authorization));
  }

  @Patch('connections/:id')
  updateConnection(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: { isEnabled?: boolean; status?: string },
  ) {
    return this.admin.updateConnection(
      this.token(authorization),
      id,
      body,
    );
  }

  @Get('broadcasts')
  broadcasts(@Headers('authorization') authorization?: string) {
    return this.admin.broadcasts(this.token(authorization));
  }

  @Post('broadcasts/:id/stop')
  stopBroadcast(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.admin.stopBroadcast(this.token(authorization), id);
  }

  @Get('support')
  support(@Headers('authorization') authorization?: string) {
    return this.admin.supportTickets(this.token(authorization));
  }

  @Patch('support/:id')
  updateSupport(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: { status?: string; priority?: string },
  ) {
    return this.admin.updateSupport(
      this.token(authorization),
      id,
      body,
    );
  }
}
