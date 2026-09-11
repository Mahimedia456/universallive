import { Body, Controller, Delete, Get, Headers, Param, Patch, Post, Put } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { SceneSourcesService } from './scene-sources.service';

@Controller('studio')
export class SceneSourcesController {
  constructor(private readonly sources: SceneSourcesService) {}

  @Get('scenes/:sceneId/sources')
  list(@Headers('authorization') auth: string | undefined, @Param('sceneId') sceneId: string) {
    return this.sources.list(bearerToken(auth), sceneId);
  }

  @Post('scenes/:sceneId/sources')
  create(@Headers('authorization') auth: string | undefined, @Param('sceneId') sceneId: string, @Body() body: any) {
    return this.sources.create(bearerToken(auth), sceneId, body);
  }

  @Put('scenes/:sceneId/sources/key/:sourceKey')
  upsertByKey(@Headers('authorization') auth: string | undefined, @Param('sceneId') sceneId: string, @Param('sourceKey') sourceKey: string, @Body() body: any) {
    return this.sources.upsertByKey(bearerToken(auth), sceneId, sourceKey, body);
  }

  @Patch('sources/:id')
  update(@Headers('authorization') auth: string | undefined, @Param('id') id: string, @Body() body: Record<string, unknown>) {
    return this.sources.update(bearerToken(auth), id, body);
  }

  @Post('sources/:id/duplicate')
  duplicate(@Headers('authorization') auth: string | undefined, @Param('id') id: string) {
    return this.sources.duplicate(bearerToken(auth), id);
  }

  @Post('scenes/:sceneId/sources/reorder')
  reorder(@Headers('authorization') auth: string | undefined, @Param('sceneId') sceneId: string, @Body() body: { orderedIds: string[] }) {
    return this.sources.reorder(bearerToken(auth), sceneId, body.orderedIds || []);
  }

  @Delete('sources/:id')
  remove(@Headers('authorization') auth: string | undefined, @Param('id') id: string) {
    return this.sources.remove(bearerToken(auth), id);
  }
}
