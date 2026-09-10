import { Body, Controller, Get, Headers, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { BillingService } from './billing.service';

@Controller('billing/purchases')
export class BillingController {
  constructor(private readonly service: BillingService) {}

  @Post('verify')
  verify(
    @Headers('authorization') auth: string | undefined,
    @Body() body: any,
  ) {
    return this.service.submit(bearerToken(auth), body);
  }

  @Get('me')
  mine(@Headers('authorization') auth?: string) {
    return this.service.listMine(bearerToken(auth));
  }

  @Post('restore')
  restore(@Headers('authorization') auth?: string) {
    return this.service.restore(bearerToken(auth));
  }
}
