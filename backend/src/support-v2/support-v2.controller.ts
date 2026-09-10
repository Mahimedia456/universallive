import { Body, Controller, Get, Headers, Param, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { SupportV2Service } from './support-v2.service';

@Controller('support/tickets')
export class SupportV2Controller {
  constructor(private readonly service: SupportV2Service) {}

  @Get()
  list(@Headers('authorization') auth?: string) {
    return this.service.list(bearerToken(auth));
  }

  @Post()
  create(
    @Headers('authorization') auth: string | undefined,
    @Body() body: any,
  ) {
    return this.service.create(bearerToken(auth), body);
  }

  @Get(':id')
  detail(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.service.detail(bearerToken(auth), id);
  }

  @Post(':id/messages')
  reply(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
    @Body() body: { message: string },
  ) {
    return this.service.reply(bearerToken(auth), id, body.message);
  }
}
