import { Body, Controller, Get, Headers, Param, Put } from '@nestjs/common';
import { bearerToken } from '../common/backend-supabase';
import { StudioWorkspaceService } from './studio-workspace.service';

@Controller('studio')
export class StudioWorkspaceController {
  constructor(private readonly service: StudioWorkspaceService) {}

  @Get('workspace')
  get(@Headers('authorization') auth?: string) {
    return this.service.get(bearerToken(auth));
  }

  @Put('workspace')
  update(@Headers('authorization') auth: string | undefined, @Body() body: any) {
    return this.service.update(bearerToken(auth), body || {});
  }

  @Put('workspace/active-scene/:sceneId')
  activate(@Headers('authorization') auth: string | undefined, @Param('sceneId') sceneId: string) {
    return this.service.activateScene(bearerToken(auth), sceneId);
  }

  @Get('audio')
  audio(@Headers('authorization') auth?: string) {
    return this.service.audio(bearerToken(auth));
  }

  @Put('audio')
  saveAudio(@Headers('authorization') auth: string | undefined, @Body() body: any) {
    return this.service.saveAudio(bearerToken(auth), body || {});
  }

  @Get('facecam')
  facecam(@Headers('authorization') auth?: string) {
    return this.service.facecam(bearerToken(auth));
  }

  @Put('facecam')
  saveFacecam(@Headers('authorization') auth: string | undefined, @Body() body: any) {
    return this.service.saveFacecam(bearerToken(auth), body || {});
  }

  @Get('quality')
  quality(@Headers('authorization') auth?: string) {
    return this.service.quality(bearerToken(auth));
  }

  @Put('quality')
  saveQuality(@Headers('authorization') auth: string | undefined, @Body() body: any) {
    return this.service.saveQuality(bearerToken(auth), body || {});
  }
}
