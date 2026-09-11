import { Body, Controller, Get, Headers, Post, Put } from '@nestjs/common';
import { readBearerToken } from '../common/bearer-token';
import { DevicesService } from './devices.service';

@Controller()
export class DevicesController {
  constructor(private readonly devices: DevicesService) {}

  @Post('devices/register')
  register(
    @Headers('authorization') authorization: string | undefined,
    @Body()
    body: {
      deviceId: string;
      platform: string;
      pushToken?: string | null;
      appVersion?: string | null;
      osVersion?: string | null;
      deviceModel?: string | null;
      locale?: string | null;
      timezone?: string | null;
      metadata?: Record<string, unknown> | null;
    },
  ) {
    return this.devices.registerDevice(readBearerToken(authorization), body);
  }

  @Get('devices/me')
  listMine(@Headers('authorization') authorization?: string) {
    return this.devices.listDevices(readBearerToken(authorization));
  }

  @Put('devices/permissions')
  permissions(
    @Headers('authorization') authorization: string | undefined,
    @Body()
    body: {
      deviceId: string;
      permissionSnapshot: Record<string, unknown>;
    },
  ) {
    return this.devices.updatePermissionSnapshot(
      readBearerToken(authorization),
      body,
    );
  }

  @Get('onboarding/me')
  onboarding(@Headers('authorization') authorization?: string) {
    return this.devices.getOnboarding(readBearerToken(authorization));
  }

  @Put('onboarding/me')
  updateOnboarding(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: Record<string, unknown>,
  ) {
    return this.devices.updateOnboarding(readBearerToken(authorization), body);
  }
}
