import { Body, Controller, Delete, Get, Headers, Post } from '@nestjs/common';
import { readBearerToken } from '../common/bearer-token';
import { AccountService } from './account.service';

@Controller('account')
export class AccountController {
  constructor(private readonly account: AccountService) {}

  @Get('overview')
  overview(@Headers('authorization') authorization?: string) {
    return this.account.overview(readBearerToken(authorization));
  }

  @Get('sessions')
  sessions(@Headers('authorization') authorization?: string) {
    return this.account.sessions(readBearerToken(authorization));
  }

  @Post('logout-all')
  logoutAll(@Headers('authorization') authorization?: string) {
    return this.account.logoutAll(readBearerToken(authorization));
  }

  @Delete()
  delete(
    @Headers('authorization') authorization: string | undefined,
    @Body() body: { confirmation?: string },
  ) {
    return this.account.deleteAccount(readBearerToken(authorization), body?.confirmation || '');
  }
}
