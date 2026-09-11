import {
  Body, Controller, Delete, Get, Headers, Param, Patch, Post,
} from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { StreamingConnectionsService } from './streaming-connections.service';

@Controller('streaming/connections')
export class StreamingConnectionsController {
  constructor(private readonly service: StreamingConnectionsService) {}

  @Get()
  list(@Headers('authorization') auth?: string) {
    return this.service.list(bearerToken(auth));
  }

  @Get(':id')
  detail(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.service.detail(bearerToken(auth), id);
  }

  @Post()
  create(
    @Headers('authorization') auth: string | undefined,
    @Body() body: { platform: string; displayName: string; isDefault?: boolean },
  ) {
    return this.service.create(bearerToken(auth), body);
  }

  @Patch(':id')
  update(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
    @Body() body: Record<string, unknown>,
  ) {
    return this.service.update(bearerToken(auth), id, body);
  }

  @Delete(':id')
  remove(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.service.remove(bearerToken(auth), id);
  }

  @Post(':id/test')
  test(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.service.test(bearerToken(auth), id);
  }
}
