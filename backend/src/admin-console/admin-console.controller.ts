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

  @Post('login')
  login(
    @Body()
    body: {
      email: string;
      password: string;
    },
  ) {
    return this.admin.login(body.email, body.password);
  }



  @Get('runtime-status')
  runtimeStatus() {
    return {
      ok: true,
      service: 'UniversalLive Admin Console API',
      runtime: 'nestjs',
      adminConsole: true,
      cors: true,
      time: new Date().toISOString(),
    };
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


  @Get('plans')
  plans(@Headers('authorization') authorization?: string) {
    return this.admin.plans(this.token(authorization));
  }

  @Patch('plans/:planKey')
  updatePlan(
    @Headers('authorization') authorization: string | undefined,
    @Param('planKey') planKey: string,
    @Body()
    body: {
      name?: string;
      description?: string;
      isActive?: boolean;
      sortOrder?: number;
      entitlements?: Record<string, unknown>;
    },
  ) {
    return this.admin.updatePlan(
      this.token(authorization),
      planKey,
      body,
    );
  }

  @Get('notifications')
  notifications(@Headers('authorization') authorization?: string) {
    return this.admin.notifications(this.token(authorization));
  }

  @Post('notifications/broadcast')
  broadcastNotification(
    @Headers('authorization') authorization: string | undefined,
    @Body()
    body: {
      title: string;
      body: string;
      audience?: 'all' | 'free' | 'creator' | 'pro';
    },
  ) {
    return this.admin.createNotificationBroadcast(
      this.token(authorization),
      body,
    );
  }

  @Get('audit')
  audit(@Headers('authorization') authorization?: string) {
    return this.admin.auditLog(this.token(authorization));
  }

  @Get('system/flags')
  flags(@Headers('authorization') authorization?: string) {
    return this.admin.systemFlags(this.token(authorization));
  }

  @Patch('system/flags/:key')
  updateFlag(
    @Headers('authorization') authorization: string | undefined,
    @Param('key') key: string,
    @Body() body: { value: unknown },
  ) {
    return this.admin.updateSystemFlag(
      this.token(authorization),
      key,
      body.value,
    );
  }

  @Get('system/health')
  systemHealth(@Headers('authorization') authorization?: string) {
    return this.admin.operationalHealth(this.token(authorization));
  }


  @Get('creators/:userId')
  creatorDetail(
    @Headers('authorization') authorization: string | undefined,
    @Param('userId') userId: string,
  ) {
    return this.admin.creatorDetail(this.token(authorization), userId);
  }

  @Get('broadcasts/:id')
  broadcastDetail(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.admin.broadcastDetail(this.token(authorization), id);
  }

  @Get('support/:id')
  supportDetail(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
  ) {
    return this.admin.supportDetail(this.token(authorization), id);
  }

  @Post('support/:id/reply')
  supportReply(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body() body: { message: string },
  ) {
    return this.admin.replySupport(
      this.token(authorization),
      id,
      body.message,
    );
  }

  @Get('admin-users')
  adminUsers(@Headers('authorization') authorization?: string) {
    return this.admin.adminUsers(this.token(authorization));
  }

  @Patch('admin-users/:id')
  updateAdminUser(
    @Headers('authorization') authorization: string | undefined,
    @Param('id') id: string,
    @Body()
    body: {
      role?: 'owner' | 'admin' | 'support' | 'viewer';
      isActive?: boolean;
      displayName?: string;
    },
  ) {
    return this.admin.updateAdminUser(
      this.token(authorization),
      id,
      body,
    );
  }
}
