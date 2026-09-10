import { Controller, Get, Headers } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { EntitlementsService } from './entitlements.service';

@Controller('billing')
export class EntitlementsController {
  constructor(private readonly service: EntitlementsService) {}

  @Get('plans')
  plans() {
    return this.service.plans();
  }

  @Get('entitlements/me')
  mine(@Headers('authorization') auth?: string) {
    return this.service.mine(bearerToken(auth));
  }
}
