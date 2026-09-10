import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Param,
  Post,
} from '@nestjs/common';

import { bearerToken } from '../common/backend-supabase';
import { AssetsV2Service } from './assets-v2.service';

@Controller('studio')
export class AssetsV2Controller {
  constructor(private readonly assets: AssetsV2Service) {}

  @Get('assets')
  list(@Headers('authorization') auth?: string) {
    return this.assets.listAssets(bearerToken(auth));
  }

  @Post('assets')
  register(
    @Headers('authorization') auth: string | undefined,
    @Body() body: {
      assetType: string;
      name: string;
      storageBucket?: string;
      storagePath: string;
      publicUrl?: string | null;
      mimeType?: string | null;
      fileSizeBytes?: number | null;
      width?: number | null;
      height?: number | null;
    },
  ) {
    return this.assets.registerAsset(bearerToken(auth), body);
  }

  @Delete('assets/:id')
  remove(
    @Headers('authorization') auth: string | undefined,
    @Param('id') id: string,
  ) {
    return this.assets.removeAsset(bearerToken(auth), id);
  }

  @Get('presets')
  presets(@Headers('authorization') auth?: string) {
    return this.assets.presets(bearerToken(auth));
  }

  @Post('presets/:presetId/create-scene')
  createFromPreset(
    @Headers('authorization') auth: string | undefined,
    @Param('presetId') presetId: string,
    @Body() body: { name?: string },
  ) {
    return this.assets.createFromPreset(
      bearerToken(auth),
      presetId,
      body?.name,
    );
  }
}
