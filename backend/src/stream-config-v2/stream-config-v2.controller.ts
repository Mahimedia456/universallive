import { Body, Controller, Get, Headers, Param, Patch, Post } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { StreamConfigV2Service } from './stream-config-v2.service';

@Controller('stream/configs')
export class StreamConfigV2Controller {
  constructor(private readonly service: StreamConfigV2Service) {}

  @Get()
  list(@Headers('authorization') auth?: string) {
    return this.service.list(bearerToken(auth));
  }

  @Post()
  create(@Headers('authorization') auth: string | undefined, @Body() body: any) {
    return this.service.create(bearerToken(auth), body);
  }

  @Patch(':id')
  update(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
    @Body() body: any,
  ) {
    return this.service.update(bearerToken(auth), id, body);
  }
}
