import { Body, Controller, Get, Headers, Put } from '@nestjs/common';
import { readBearerToken } from '../common/bearer-token';
import { SettingsService } from './settings.service';

@Controller('settings')
export class SettingsController {
  constructor(private readonly settings: SettingsService) {}

  @Get()
  get(@Headers('authorization') authorization?: string) {
    return this.settings.get(readBearerToken(authorization));
  }

  @Put()
  put(@Headers('authorization') authorization: string | undefined, @Body() body: any) {
    return this.settings.update(readBearerToken(authorization), body || {});
  }

  @Get('streaming')
  streaming(@Headers('authorization') authorization?: string) {
    return this.settings.streaming(readBearerToken(authorization));
  }

  @Put('streaming')
  updateStreaming(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: Record<string, unknown>,
  ) {
    return this.settings.updateStreaming(readBearerToken(authorization), body || {});
  }
}
