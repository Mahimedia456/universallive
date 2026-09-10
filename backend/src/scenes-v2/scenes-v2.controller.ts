import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Param,
  Patch,
  Post,
} from '@nestjs/common';

import { bearerToken } from '../common/backend-supabase';
import { ScenesV2Service } from './scenes-v2.service';

@Controller('studio/scenes')
export class ScenesV2Controller {
  constructor(private readonly scenes: ScenesV2Service) {}

  @Get()
  list(@Headers('authorization') auth?: string) {
    return this.scenes.list(bearerToken(auth));
  }

  @Get(':id')
  get(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.scenes.get(bearerToken(auth), id);
  }

  @Post()
  create(
    @Headers('authorization') auth: string | undefined,
    @Body() body: {
      name: string;
      description?: string;
      aspectRatio?: string;
      width?: number;
      height?: number;
      isDefault?: boolean;
      templateKey?: string | null;
    },
  ) {
    return this.scenes.create(bearerToken(auth), body);
  }

  @Patch(':id')
  update(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
    @Body() body: Record<string, unknown>,
  ) {
    return this.scenes.update(bearerToken(auth), id, body);
  }

  @Post(':id/duplicate')
  duplicate(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.scenes.duplicate(bearerToken(auth), id);
  }

  @Delete(':id')
  remove(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.scenes.remove(bearerToken(auth), id);
  }
}
