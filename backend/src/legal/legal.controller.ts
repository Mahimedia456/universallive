import { Body, Controller, Get, Headers, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { LegalService } from './legal.service';

@Controller('legal')
export class LegalController {
  constructor(private readonly service: LegalService) {}

  @Get('about')
  about() {
    return this.service.about();
  }

  @Get('documents')
  documents() {
    return this.service.documents();
  }

  @Get('acceptances')
  acceptances(@Headers('authorization') auth?: string) {
    return this.service.acceptances(bearerToken(auth));
  }

  @Post('accept')
  accept(@Headers('authorization') auth: string | undefined, @Body() body: any) {
    return this.service.accept(bearerToken(auth), body);
  }
}
